#!/usr/bin/env node

/**
 * Stdio-to-WebSocket bridge for Solim MCP debug server and Antigravity.
 *
 * Antigravity communicates with MCP servers via stdio (JSON-RPC 2.0 lines on stdin/stdout).
 * This bridge connects to the Solim WebSocket server running inside the Mindustry JVM.
 *
 * Features:
 * - Auto-reconnects in the background when Mindustry starts/restarts.
 * - Stays alive when Mindustry is offline so Antigravity doesn't crash or lose the MCP server.
 * - Directly handles `initialize`, `ping`, and `tools/list` when offline so tool schemas are always available.
 * - Returns clean, informative status messages for `tools/call` when Mindustry is not running.
 *
 * Requirements: Node.js 22+ (uses native global WebSocket).
 */

const readline = require('readline');

const WS_URL = process.env.SOLIM_MCP_URL || 'ws://127.0.0.1:8754/';
const RECONNECT_INTERVAL_MS = 2000;

const STATIC_TOOLS = [
  {
    name: 'get_component_tree',
    description: 'Returns the live Arc/Solim element tree (name, type, layout, children).',
    inputSchema: {
      type: 'object',
      properties: {
        target: {
          type: 'string',
          description: 'Optional element name or class to scope the snapshot to a subtree.'
        }
      }
    }
  },
  {
    name: 'get_signal_values',
    description: 'Returns live Signal/Computed values with listener/observer/dependency counts.',
    inputSchema: {
      type: 'object',
      properties: {
        query: {
          type: 'string',
          description: 'Optional case-insensitive substring filter on signal location/id.'
        }
      }
    }
  },
  {
    name: 'get_bindings',
    description: 'Returns reactive effects/bindings discovered on components and elements.',
    inputSchema: {
      type: 'object',
      properties: {
        query: {
          type: 'string',
          description: 'Optional case-insensitive substring filter on binding target/location.'
        }
      }
    }
  },
  {
    name: 'get_layout',
    description: 'Returns x/y/width/height and expansion flags for a named element.',
    inputSchema: {
      type: 'object',
      properties: {
        target: {
          type: 'string',
          description: 'Element name, simple class name, or fully qualified class name.'
        }
      },
      required: ['target']
    }
  },
  {
    name: 'find_elements',
    description: 'Searches the live UI hierarchy for elements matching a query by name, class, or label text.',
    inputSchema: {
      type: 'object',
      properties: {
        query: {
          type: 'string',
          description: 'Substring to search for in element names, class names, or label text (case-insensitive).'
        },
        maxResults: {
          type: 'integer',
          description: 'Maximum number of matching elements to return (default 25).'
        }
      },
      required: ['query']
    }
  },
  {
    name: 'click_element',
    description: 'Finds a UI element by name or class and triggers a click event on the main game thread.',
    inputSchema: {
      type: 'object',
      properties: {
        target: {
          type: 'string',
          description: 'Element name or class name to click.'
        }
      },
      required: ['target']
    }
  },
  {
    name: 'take_screenshot',
    description: 'Captures the live game screen as a base64 PNG (max 1 MB, auto-scaled). Optional x/y/width/height capture a framebuffer region (bottom-left origin).',
    inputSchema: {
      type: 'object',
      properties: {
        maxWidth: {
          type: 'integer',
          description: 'Pre-scale captures wider than this (pixels). Default 1280, minimum 320.'
        },
        maxBytes: {
          type: 'integer',
          description: 'Maximum PNG size in bytes. Default 1048576 (1 MB); larger captures are scaled down.'
        },
        x: {
          type: 'integer',
          description: 'Region left edge in framebuffer pixels from the bottom-left. Requires y, width, height.'
        },
        y: {
          type: 'integer',
          description: 'Region bottom edge in framebuffer pixels from the bottom-left. Requires x, width, height.'
        },
        width: {
          type: 'integer',
          description: 'Region width in pixels. Requires x, y, height.'
        },
        height: {
          type: 'integer',
          description: 'Region height in pixels. Requires x, y, width.'
        }
      }
    }
  },
  {
    name: 'execute_js',
    description: 'Executes JavaScript code in the live Mindustry Rhino environment and returns the evaluated result.',
    inputSchema: {
      type: 'object',
      properties: {
        code: {
          type: 'string',
          description: 'JavaScript code to evaluate in the Rhino runtime.'
        }
      },
      required: ['code']
    }
  },
  {
    name: 'stop',
    description: 'Stops and exits the running Mindustry instance.',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  }
];

let ws = null;
let isConnected = false;
let isConnecting = false;
let hasLoggedOffline = false;
let reconnectTimer = null;
const pendingRequestIds = new Set();

function sendToClient(obj) {
  process.stdout.write(JSON.stringify(obj) + '\n');
}

function logStatus(msg) {
  process.stderr.write(`[solim-bridge] ${msg}\n`);
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect();
  }, RECONNECT_INTERVAL_MS);
}

function connect() {
  if (isConnected || isConnecting) return;
  isConnecting = true;

  try {
    const socket = new WebSocket(WS_URL);

    socket.addEventListener('open', () => {
      ws = socket;
      isConnected = true;
      isConnecting = false;
      hasLoggedOffline = false;
      logStatus(`Connected to live Mindustry Solim MCP server at ${WS_URL}`);
    });

    socket.addEventListener('message', (event) => {
      if (typeof event.data === 'string') {
        const text = event.data.trim();
        try {
          const parsed = JSON.parse(text);
          if (parsed.id !== undefined) {
            pendingRequestIds.delete(parsed.id);
          }
        } catch (_) {}
        process.stdout.write(text + '\n');
      }
    });

    socket.addEventListener('error', () => {
      // Error will trigger 'close' event right after
    });

    socket.addEventListener('close', () => {
      const wasConnected = isConnected;
      isConnected = false;
      isConnecting = false;
      ws = null;

      // Fail any requests that were in-flight when connection dropped
      if (pendingRequestIds.size > 0) {
        for (const id of pendingRequestIds) {
          sendToClient({
            jsonrpc: '2.0',
            id,
            result: {
              content: [
                {
                  type: 'text',
                  text: `Mindustry connection lost while executing request. Run \`run.bat\` to restart Mindustry.`
                }
              ],
              isError: true
            }
          });
        }
        pendingRequestIds.clear();
      }

      if (wasConnected) {
        logStatus('Mindustry connection closed. Auto-reconnecting in background...');
      } else if (!hasLoggedOffline) {
        logStatus(`Mindustry is not running at ${WS_URL}. Bridge is running offline and will auto-connect when launched.`);
        hasLoggedOffline = true;
      }

      scheduleReconnect();
    });
  } catch (err) {
    isConnecting = false;
    if (!hasLoggedOffline) {
      logStatus(`Failed to connect to ${WS_URL}: ${err.message}. Retrying in background...`);
      hasLoggedOffline = true;
    }
    scheduleReconnect();
  }
}

connect();

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', (line) => {
  const trimmed = line.trim();
  if (!trimmed) return;

  let request;
  try {
    request = JSON.parse(trimmed);
  } catch (err) {
    sendToClient({
      jsonrpc: '2.0',
      id: null,
      error: { code: -32700, message: `Parse error: ${err.message}` }
    });
    return;
  }

  const isNotification = request.id === undefined || request.id === null;

  // When connected to live Mindustry, forward all requests directly
  if (isConnected && ws && ws.readyState === WebSocket.OPEN) {
    if (!isNotification) {
      pendingRequestIds.add(request.id);
    }
    ws.send(trimmed);
    return;
  }

  // When offline (Mindustry not running), answer safely so Antigravity stays operational
  if (isNotification) {
    return;
  }

  switch (request.method) {
    case 'initialize':
      sendToClient({
        jsonrpc: '2.0',
        id: request.id,
        result: {
          protocolVersion: '2024-11-05',
          capabilities: { tools: { listChanged: false } },
          serverInfo: { name: 'solim-mcp-debug', version: '1.0' }
        }
      });
      break;

    case 'ping':
      sendToClient({ jsonrpc: '2.0', id: request.id, result: {} });
      break;

    case 'tools/list':
      sendToClient({
        jsonrpc: '2.0',
        id: request.id,
        result: { tools: STATIC_TOOLS }
      });
      break;

    case 'tools/call':
      if (request.params && (request.params.name === 'stop' || request.params.name === 'stop_mindustry')) {
        sendToClient({
          jsonrpc: '2.0',
          id: request.id,
          result: {
            content: [
              {
                type: 'text',
                text: JSON.stringify({ stopped: true, message: 'Mindustry is not running (already stopped).' })
              }
            ]
          }
        });
        break;
      }
      sendToClient({
        jsonrpc: '2.0',
        id: request.id,
        result: {
          content: [
            {
              type: 'text',
              text: `Mindustry is not currently running (Solim MCP server offline at ${WS_URL}). Run \`run.bat\` to auto build and run Mindustry, then retry UI inspection.`
            }
          ],
          isError: true
        }
      });
      break;

    default:
      sendToClient({
        jsonrpc: '2.0',
        id: request.id,
        error: {
          code: -32000,
          message: `Solim MCP server is offline (${WS_URL}). Mindustry is not running.`
        }
      });
      break;
  }
});

rl.on('close', () => {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.close();
  }
  process.exit(0);
});

process.on('SIGINT', () => {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  if (ws && ws.readyState === WebSocket.OPEN) ws.close();
  process.exit(0);
});

process.on('SIGTERM', () => {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  if (ws && ws.readyState === WebSocket.OPEN) ws.close();
  process.exit(0);
});
