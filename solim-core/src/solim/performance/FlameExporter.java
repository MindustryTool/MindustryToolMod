package solim.performance;

import arc.util.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless flame exporter turning trace snapshots into dual-format payloads
 * and a self-contained HTML file. All filtering happens at export time; the
 * recorded span list is never modified.
 */
public final class FlameExporter {

    private FlameExporter() {
    }

    /** Returns a copy ordered by start time, then depth, then span id. */
    public static List<TraceSpan> ordered(@Nullable List<TraceSpan> spans) {
        List<TraceSpan> out = new ArrayList<>();
        if (spans != null) {
            out.addAll(spans);
        }
        Collections.sort(out, new Comparator<TraceSpan>() {
            @Override
            public int compare(TraceSpan a, TraceSpan b) {
                if (a == b) {
                    return 0;
                }
                if (a == null) {
                    return 1;
                }
                if (b == null) {
                    return -1;
                }
                if (a.startNs < b.startNs) {
                    return -1;
                }
                if (a.startNs > b.startNs) {
                    return 1;
                }
                if (a.depth < b.depth) {
                    return -1;
                }
                if (a.depth > b.depth) {
                    return 1;
                }
                if (a.spanId < b.spanId) {
                    return -1;
                }
                if (a.spanId > b.spanId) {
                    return 1;
                }
                return 0;
            }
        });
        return out;
    }

    /**
     * Returns spans with duration at or above {@code minMs}, preserving order.
     * A non-positive threshold keeps every span.
     */
    public static List<TraceSpan> pruned(@Nullable List<TraceSpan> spans, double minMs) {
        List<TraceSpan> out = new ArrayList<>();
        if (spans == null) {
            return out;
        }
        if (minMs <= 0.0) {
            out.addAll(spans);
            return out;
        }
        long thresholdNs = (long) (minMs * 1000000.0);
        for (TraceSpan span : spans) {
            if (span != null && span.durationNs() >= thresholdNs) {
                out.add(span);
            }
        }
        return out;
    }

    /**
     * Computes self-time per span id as total minus direct children totals.
     * Only spans present in the given list contribute; missing parents are ignored.
     */
    public static Map<Integer, Long> selfTimes(@Nullable List<TraceSpan> spans) {
        Map<Integer, Long> self = new HashMap<>();
        if (spans == null || spans.isEmpty()) {
            return self;
        }
        Map<Integer, TraceSpan> byId = new HashMap<>();
        Map<Integer, Long> childrenSum = new HashMap<>();
        for (TraceSpan span : spans) {
            if (span == null) {
                continue;
            }
            byId.put(span.spanId, span);
        }
        for (TraceSpan span : spans) {
            if (span == null) {
                continue;
            }
            if (span.parentId != -1 && byId.containsKey(span.parentId)) {
                Long sum = childrenSum.get(span.parentId);
                long total = span.durationNs();
                childrenSum.put(span.parentId, sum == null ? total : sum + total);
            }
        }
        for (TraceSpan span : spans) {
            if (span == null) {
                continue;
            }
            Long sum = childrenSum.get(span.spanId);
            long children = sum == null ? 0L : sum;
            long total = span.durationNs();
            long own = total - children;
            self.put(span.spanId, own >= 0 ? own : 0L);
        }
        return self;
    }

    /** Prefers an explicit name, falling back to a non-blank generic label. */
    public static String label(@Nullable TraceSpan span) {
        if (span == null) {
            return "Component-element";
        }
        String name = span.name;
        return name != null && !name.trim().isEmpty() ? name : "Component-element";
    }

    /** Chrome Trace Events payload with X phases and ns-to-us conversion. */
    public static String chromePayload(@Nullable List<TraceSpan> spans, double minMs,
            int droppedCount, long traceId) {
        List<TraceSpan> kept = pruned(ordered(spans), minMs);
        Map<Integer, Long> self = selfTimes(kept);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"traceEvents\":[");
        boolean first = true;
        for (TraceSpan span : kept) {
            if (span == null) {
                continue;
            }
            long tsUs = span.startNs / 1000L;
            long durUs = span.durationNs() / 1000L;
            Long ownNs = self.get(span.spanId);
            double selfMs = ownNs == null ? 0.0 : ownNs / 1000000.0;
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"name\":\"").append(escapeJson(label(span))).append('"');
            sb.append(",\"cat\":\"").append(escapeJson(span.phase)).append('"');
            sb.append(",\"ph\":\"X\"");
            sb.append(",\"ts\":").append(tsUs);
            sb.append(",\"dur\":").append(durUs);
            sb.append(",\"pid\":1,\"tid\":1");
            sb.append(",\"args\":{\"spanId\":").append(span.spanId);
            sb.append(",\"parentId\":").append(span.parentId);
            sb.append(",\"depth\":").append(span.depth);
            sb.append(",\"selfTimeMs\":").append(selfMs);
            sb.append(",\"traceId\":").append(traceId);
            sb.append("}}");
        }
        sb.append("],\"displayTimeUnit\":\"ms\"");
        sb.append(",\"metadata\":{\"dropped\":").append(droppedCount < 0 ? 0 : droppedCount);
        sb.append(",\"spanCount\":").append(kept.size()).append("}}");
        return sb.toString();
    }

    /** Speedscope-native evented profile derived from the same span list. */
    public static String speedscopePayload(@Nullable List<TraceSpan> spans, double minMs,
            int droppedCount, long traceId) {
        List<TraceSpan> kept = pruned(ordered(spans), minMs);
        Map<String, Integer> frameIndex = new HashMap<>();
        List<String> frames = new ArrayList<>();
        for (TraceSpan span : kept) {
            if (span == null) {
                continue;
            }
            String name = label(span);
            if (!frameIndex.containsKey(name)) {
                frameIndex.put(name, frames.size());
                frames.add(name);
            }
        }
        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        for (TraceSpan span : kept) {
            if (span == null) {
                continue;
            }
            if (span.startNs < minStart) {
                minStart = span.startNs;
            }
            if (span.endNs > maxEnd) {
                maxEnd = span.endNs;
            }
        }
        boolean empty = kept.isEmpty();
        long base = empty ? 0L : minStart;
        long endValue = empty ? 0L : maxEnd - minStart;

        List<SpeedEvent> events = new ArrayList<>();
        for (TraceSpan span : kept) {
            if (span == null) {
                continue;
            }
            Integer idx = frameIndex.get(label(span));
            int frame = idx == null ? 0 : idx;
            events.add(new SpeedEvent(true, span.startNs - base, frame, span.depth));
            events.add(new SpeedEvent(false, span.endNs - base, frame, span.depth));
        }
        Collections.sort(events, new Comparator<SpeedEvent>() {
            @Override
            public int compare(SpeedEvent a, SpeedEvent b) {
                if (a.at < b.at) {
                    return -1;
                }
                if (a.at > b.at) {
                    return 1;
                }
                boolean aClose = !a.open;
                boolean bClose = !b.open;
                if (aClose != bClose) {
                    return aClose ? -1 : 1;
                }
                if (a.open) {
                    return a.depth < b.depth ? -1 : a.depth > b.depth ? 1 : 0;
                }
                return a.depth > b.depth ? -1 : a.depth < b.depth ? 1 : 0;
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("{\"$schema\":\"https://www.speedscope.app/file-format-schema.json\"");
        sb.append(",\"shared\":{\"frames\":[");
        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":\"").append(escapeJson(frames.get(i))).append("\"}");
        }
        sb.append("]}");
        sb.append(",\"profiles\":[{\"type\":\"evented\",\"name\":\"Solim flame trace ");
        sb.append(traceId).append("\",\"unit\":\"nanoseconds\"");
        sb.append(",\"startValue\":0,\"endValue\":").append(endValue);
        sb.append(",\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            SpeedEvent e = events.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"type\":\"").append(e.open ? "O" : "C").append('"');
            sb.append(",\"frame\":").append(e.frame);
            sb.append(",\"at\":").append(e.at).append('}');
        }
        sb.append("]}]}");
        return sb.toString();
    }

    /** Self-contained HTML embedding both payloads and a simple flame render. */
    public static String exportHtml(@Nullable List<TraceSpan> spans, double minMs,
            int droppedCount, long traceId) {
        List<TraceSpan> kept = pruned(ordered(spans), minMs);
        Map<Integer, Long> self = selfTimes(kept);
        String chrome = chromePayload(kept, 0.0, droppedCount, traceId);
        String speedscope = speedscopePayload(kept, 0.0, droppedCount, traceId);

        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        int maxDepth = 0;
        for (TraceSpan span : kept) {
            if (span == null) {
                continue;
            }
            if (span.startNs < minStart) {
                minStart = span.startNs;
            }
            if (span.endNs > maxEnd) {
                maxEnd = span.endNs;
            }
            if (span.depth > maxDepth) {
                maxDepth = span.depth;
            }
        }
        boolean empty = kept.isEmpty();
        long total = empty ? 1L : maxEnd - minStart;
        if (total <= 0L) {
            total = 1L;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\">");
        sb.append("<title>Solim Flame Trace</title>");
        sb.append("<style>body{font-family:sans-serif;margin:16px;background:#111;color:#eee}");
        sb.append("#flame{position:relative;border:1px solid #444;background:#1a1a1a;overflow:hidden}");
        sb.append(".frame{position:absolute;height:18px;overflow:hidden;white-space:nowrap;");
        sb.append("font-size:11px;line-height:18px;padding:0 4px;box-sizing:border-box;");
        sb.append("border:1px solid rgba(0,0,0,.4);border-radius:3px;background:#3a7bd5;color:#fff}");
        sb.append(".meta{color:#bbb;font-size:12px;margin:8px 0}</style></head><body>");
        sb.append("<h1>Solim Flame Trace</h1>");
        sb.append("<div class=\"meta\">spans ").append(kept.size());
        sb.append(" dropped ").append(droppedCount < 0 ? 0 : droppedCount);
        sb.append(" trace ").append(traceId);
        sb.append(" minMs ").append(minMs).append("</div>");
        int flameHeight = (maxDepth + 1) * 20 + 8;
        if (empty) {
            flameHeight = 28;
        }
        sb.append("<div id=\"flame\" style=\"height:").append(flameHeight).append("px\">");
        if (empty) {
            sb.append("<div class=\"meta\">no spans</div>");
        } else {
            for (TraceSpan span : kept) {
                if (span == null) {
                    continue;
                }
                double left = (span.startNs - minStart) * 100.0 / total;
                double width = span.durationNs() * 100.0 / total;
                if (width < 0.1) {
                    width = 0.1;
                }
                int top = span.depth * 20 + 4;
                Long ownNs = self.get(span.spanId);
                double selfMs = ownNs == null ? 0.0 : ownNs / 1000000.0;
                sb.append("<div class=\"frame\" title=\"");
                sb.append(escapeHtml(label(span))).append(" total ");
                sb.append(span.durationMs()).append("ms self ").append(selfMs).append("ms\"");
                sb.append(" style=\"left:").append(left).append("%;width:");
                sb.append(width).append("%;top:").append(top).append("px\">");
                sb.append(escapeHtml(label(span)));
                sb.append("</div>");
            }
        }
        sb.append("</div>");
        sb.append("<script type=\"application/json\" id=\"chrome-trace\">");
        sb.append(safeScript(chrome));
        sb.append("</script>");
        sb.append("<script type=\"application/json\" id=\"speedscope\">");
        sb.append(safeScript(speedscope));
        sb.append("</script>");
        sb.append("<script>var chromeTrace=JSON.parse(document.getElementById('chrome-trace').textContent);");
        sb.append("var speedscope=JSON.parse(document.getElementById('speedscope').textContent);");
        sb.append("window.solimFlame={chrome:chromeTrace,speedscope:speedscope};</script>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /** Convenience overload defaulting dropped count and trace id. */
    public static String exportHtml(@Nullable List<TraceSpan> spans, double minMs) {
        return exportHtml(spans, minMs, 0, -1L);
    }

    /** Writes the HTML export to a file using Java 8 compatible I/O. */
    public static void writeHtmlFile(@Nullable List<TraceSpan> spans, double minMs,
            int droppedCount, long traceId, File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null for flame export");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create directories for " + file);
        }
        String html = exportHtml(spans, minMs, droppedCount, traceId);
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            writer.write(html);
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String safeScript(String json) {
        if (json == null) {
            return "{}";
        }
        return json.replace("</", "<\\/");
    }

    private static final class SpeedEvent {
        final boolean open;
        final long at;
        final int frame;
        final int depth;

        SpeedEvent(boolean open, long at, int frame, int depth) {
            this.open = open;
            this.at = at;
            this.frame = frame;
            this.depth = depth;
        }
    }
}
