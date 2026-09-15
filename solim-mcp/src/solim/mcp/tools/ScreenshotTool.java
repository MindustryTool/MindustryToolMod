package solim.mcp.tools;

import arc.Core;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.ScreenUtils;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Captures the live game framebuffer via {@link ScreenUtils} and returns it as a base64 PNG.
 *
 * <p>The PNG payload never exceeds {@code maxBytes} (default 1 MB): oversized captures are
 * iteratively downscaled until they fit. An optional {@code x}/{@code y}/{@code width}/{@code
 * height} region (framebuffer pixels, bottom-left origin) captures a screen section instead of
 * the full frame.
 */
public final class ScreenshotTool implements McpTool {
	static final int DEFAULT_MAX_BYTES = 1024 * 1024;
	static final int DEFAULT_MAX_WIDTH = 1280;
	static final int MIN_WIDTH = 320;
	static final int MIN_MAX_BYTES = 32 * 1024;
	static final int MAX_MAX_BYTES = 8 * 1024 * 1024;
	static final int MAX_ATTEMPTS = 5;
	static final double SCALE_STEP = 0.75;
	static final long CAPTURE_TIMEOUT_SECONDS = 5;

	@Override
	public String name() {
		return "take_screenshot";
	}

	@Override
	public String description() {
		return "Captures the live game screen as a base64 PNG (max 1 MB, auto-scaled). "
			+ "Optional x/y/width/height capture a framebuffer region (bottom-left origin).";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("maxWidth")
			.put("type", "integer")
			.put("description", "Pre-scale captures wider than this (pixels). Default 1280, minimum 320.");
		props.putObject("maxBytes")
			.put("type", "integer")
			.put("description", "Maximum PNG size in bytes. Default 1048576 (1 MB); larger captures are scaled down.");
		props.putObject("x")
			.put("type", "integer")
			.put("description", "Region left edge in framebuffer pixels from the bottom-left. Requires y, width, height.");
		props.putObject("y")
			.put("type", "integer")
			.put("description", "Region bottom edge in framebuffer pixels from the bottom-left. Requires x, width, height.");
		props.putObject("width")
			.put("type", "integer")
			.put("description", "Region width in pixels. Requires x, y, height.");
		props.putObject("height")
			.put("type", "integer")
			.put("description", "Region height in pixels. Requires x, y, width.");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		if (args == null) {
			args = JsonNodeFactory.instance.objectNode();
		}
		int maxBytes = readInt(args, "maxBytes", DEFAULT_MAX_BYTES);
		if (maxBytes < MIN_MAX_BYTES || maxBytes > MAX_MAX_BYTES) {
			maxBytes = DEFAULT_MAX_BYTES;
		}
		int maxWidth = readInt(args, "maxWidth", DEFAULT_MAX_WIDTH);
		if (maxWidth < MIN_WIDTH) {
			maxWidth = maxWidth <= 0 ? DEFAULT_MAX_WIDTH : MIN_WIDTH;
		}

		boolean hasX = args.hasNonNull("x");
		boolean hasY = args.hasNonNull("y");
		boolean hasW = args.hasNonNull("width");
		boolean hasH = args.hasNonNull("height");
		int regionArgs = (hasX ? 1 : 0) + (hasY ? 1 : 0) + (hasW ? 1 : 0) + (hasH ? 1 : 0);
		if (regionArgs > 0 && regionArgs < 4) {
			throw new MCPException("take_screenshot requires x, y, width and height together for a region capture");
		}
		boolean wantRegion = regionArgs == 4;
		if (wantRegion && (!args.path("x").isIntegralNumber() || !args.path("y").isIntegralNumber()
				|| !args.path("width").isIntegralNumber() || !args.path("height").isIntegralNumber())) {
			throw new MCPException("take_screenshot region x, y, width and height must be integers");
		}
		final int reqX = wantRegion ? args.path("x").asInt() : 0;
		final int reqY = wantRegion ? args.path("y").asInt() : 0;
		final int reqW = wantRegion ? args.path("width").asInt() : 0;
		final int reqH = wantRegion ? args.path("height").asInt() : 0;
		if (wantRegion && (reqW <= 0 || reqH <= 0)) {
			throw new MCPException("take_screenshot region width and height must be positive");
		}

		if (Core.app == null || Core.graphics == null) {
			throw new MCPException("take_screenshot requires a running game (no render context available)");
		}

		List<Pixmap> owned = new ArrayList<>();
		try {
			Capture capture = captureOnRenderThread(reqX, reqY, reqW, reqH, wantRegion);
			owned.add(capture.pixmap);
			Pixmap current = capture.pixmap;
			boolean scaled = false;

			if (current.getWidth() > maxWidth) {
				int[] dims = fitWidth(current.getWidth(), current.getHeight(), maxWidth);
				current = scaleTo(current, dims[0], dims[1], owned);
				scaled = true;
			}

			byte[] png = encode(current);
			int attempts = 0;
			while (png.length > maxBytes && attempts < MAX_ATTEMPTS) {
				int[] dims = shrinkDims(current.getWidth(), current.getHeight());
				if (dims[0] >= current.getWidth()) {
					break;
				}
				current = scaleTo(current, dims[0], dims[1], owned);
				png = encode(current);
				attempts++;
				scaled = true;
			}

			ObjectNode out = JsonNodeFactory.instance.objectNode();
			out.put("mimeType", "image/png");
			out.put("image", "data:image/png;base64," + Base64.getEncoder().encodeToString(png));
			out.put("width", current.getWidth());
			out.put("height", current.getHeight());
			out.put("bytes", png.length);
			out.put("scaled", scaled);
			if (capture.region != null) {
				ObjectNode region = out.putObject("region");
				region.put("x", capture.region[0]);
				region.put("y", capture.region[1]);
				region.put("width", capture.region[2]);
				region.put("height", capture.region[3]);
			}
			return out;
		} finally {
			for (int i = 0; i < owned.size(); i++) {
				owned.get(i).dispose();
			}
		}
	}

	private static int readInt(ObjectNode args, String field, int fallback) {
		return args.path(field).isIntegralNumber() ? args.path(field).asInt() : fallback;
	}

	private static Capture captureOnRenderThread(
			final int reqX, final int reqY, final int reqW, final int reqH, final boolean wantRegion)
			throws MCPException {
		final CompletableFuture<Capture> future = new CompletableFuture<>();
		Core.app.post(new Runnable() {
			@Override
			public void run() {
				try {
					int fbW = Core.graphics.getWidth();
					int fbH = Core.graphics.getHeight();
					if (fbW <= 0 || fbH <= 0) {
						throw new MCPException("take_screenshot found an invalid framebuffer size");
					}
					int[] rect;
					if (wantRegion) {
						rect = clampRegion(reqX, reqY, reqW, reqH, fbW, fbH);
						if (rect == null) {
							throw new MCPException("take_screenshot region is outside the screen");
						}
					} else {
						rect = new int[]{0, 0, fbW, fbH};
					}
					Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(rect[0], rect[1], rect[2], rect[3], true);
					future.complete(new Capture(pixmap, wantRegion ? rect : null));
				} catch (Throwable t) {
					future.completeExceptionally(t);
				}
			}
		});
		try {
			return future.get(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new MCPException("take_screenshot timed out waiting for the render thread");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MCPException("take_screenshot was interrupted");
		} catch (ExecutionException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			if (cause instanceof MCPException) {
				throw (MCPException) cause;
			}
			throw new MCPException("take_screenshot capture failed: " + cause.getMessage());
		}
	}

	private static byte[] encode(Pixmap pixmap) throws MCPException {
		try {
			return PixmapIO.writePngBytes(pixmap);
		} catch (Exception e) {
			throw new MCPException("take_screenshot PNG encoding failed: " + e.getMessage());
		}
	}

	private static Pixmap scaleTo(Pixmap src, int dstW, int dstH, List<Pixmap> owned) {
		Pixmap dst = new Pixmap(dstW, dstH);
		owned.add(dst);
		dst.draw(src, 0, 0, src.getWidth(), src.getHeight(), 0, 0, dstW, dstH, true);
		return dst;
	}

	/** Intersects the requested rect with the framebuffer; returns {x, y, w, h} or null when empty. */
	static int[] clampRegion(int x, int y, int w, int h, int fbW, int fbH) {
		int x0 = Math.max(x, 0);
		int y0 = Math.max(y, 0);
		int x1 = Math.min(x + w, fbW);
		int y1 = Math.min(y + h, fbH);
		if (x1 <= x0 || y1 <= y0) {
			return null;
		}
		return new int[]{x0, y0, x1 - x0, y1 - y0};
	}

	/** Next downscale step preserving aspect ratio, floored at {@link #MIN_WIDTH}. */
	static int[] shrinkDims(int w, int h) {
		int nw = Math.max(MIN_WIDTH, (int) (w * SCALE_STEP));
		int nh = Math.max(1, (int) Math.round(h * ((double) nw / w)));
		return new int[]{nw, nh};
	}

	/** Dims fitting {@code maxWidth} while preserving aspect ratio. */
	static int[] fitWidth(int w, int h, int maxWidth) {
		int nh = Math.max(1, (int) Math.round(h * ((double) maxWidth / w)));
		return new int[]{maxWidth, nh};
	}

	private static final class Capture {
		final Pixmap pixmap;
		final @Nullable int[] region;

		Capture(Pixmap pixmap, int[] region) {
			this.pixmap = pixmap;
			this.region = region;
		}
	}
}
