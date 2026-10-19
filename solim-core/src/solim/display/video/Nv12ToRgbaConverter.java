package solim.display.video;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Converts Media Foundation NV12 (planar Y, interleaved UV) frames into RGBA8888.
 * Lookup tables keep the per-pixel cost low and the frame is split across a small
 * pool so 1440p conversion stays inside the frame budget; a single decoder owns the
 * pool and shuts it down when it closes.
 */
final class Nv12ToRgbaConverter {

	private static final int MAX_SLICES = 6;
	private static final int MIN_PARALLEL_HEIGHT = 256;

	private static final int[] Y_601 = new int[256];
	private static final int[] R_V_601 = new int[256];
	private static final int[] G_U_601 = new int[256];
	private static final int[] G_V_601 = new int[256];
	private static final int[] B_U_601 = new int[256];

	private static final int[] Y_709 = new int[256];
	private static final int[] R_V_709 = new int[256];
	private static final int[] G_U_709 = new int[256];
	private static final int[] G_V_709 = new int[256];
	private static final int[] B_U_709 = new int[256];

	static {
		fill(Y_601, R_V_601, G_U_601, G_V_601, B_U_601, 298, 409, -100, -208, 516);
		fill(Y_709, R_V_709, G_U_709, G_V_709, B_U_709, 298, 459, -55, -136, 541);
	}

	private final ExecutorService pool;

	Nv12ToRgbaConverter() {
		int parallelism = Math.max(1, Math.min(MAX_SLICES, Runtime.getRuntime().availableProcessors() - 1));
		pool = parallelism > 1 ? Executors.newFixedThreadPool(parallelism, daemonFactory()) : null;
	}

	static int frameSize(int stride, int codedHeight) {
		return Math.max(1, Math.abs(stride)) * Math.max(1, codedHeight) * 3 / 2;
	}

	void convert(byte[] source, int stride, int uvOffset, int codedHeight,
			int offsetX, int offsetY, int width, int height, boolean flip, int[] out) {
		boolean highDefinition = height >= 720;
		if (pool == null || height < MIN_PARALLEL_HEIGHT) {
			convertRange(source, stride, uvOffset, codedHeight, offsetX, offsetY, width, 0, height, flip,
					highDefinition, out);
			return;
		}
		int slices = sliceCount(height);
		int rowsPerSlice = ((height / 2 + slices - 1) / slices) * 2;
		List<Future<?>> pending = new ArrayList<>(slices);
		for (int start = 0; start < height; start += rowsPerSlice) {
			int from = start;
			int to = Math.min(height, start + rowsPerSlice);
			pending.add(pool.submit(() -> convertRange(source, stride, uvOffset, codedHeight,
					offsetX, offsetY, width, from, to, flip, highDefinition, out)));
		}
		for (int index = 0; index < pending.size(); index++) {
			try {
				pending.get(index).get();
			} catch (Exception error) {
				throw new IllegalStateException("NV12 conversion failed", error);
			}
		}
	}

	void close() {
		if (pool != null)
			pool.shutdownNow();
	}

	private static int sliceCount(int height) {
		int slices = MAX_SLICES;
		while (slices > 1 && height / (slices * 2) < 8)
			slices--;
		return slices;
	}

	private static void convertRange(byte[] source, int stride, int uvOffset, int codedHeight,
			int offsetX, int offsetY, int width, int fromRow, int toRow, boolean flip,
			boolean highDefinition, int[] out) {
		int[] yTable = highDefinition ? Y_709 : Y_601;
		int[] rTable = highDefinition ? R_V_709 : R_V_601;
		int[] gTableU = highDefinition ? G_U_709 : G_U_601;
		int[] gTableV = highDefinition ? G_V_709 : G_V_601;
		int[] bTable = highDefinition ? B_U_709 : B_U_601;

		int evenWidth = width & ~1;
		int evenHeight = toRow - ((toRow - fromRow) & 1);
		for (int y = fromRow; y < evenHeight; y += 2) {
			int topRow = flip ? codedHeight - 1 - (offsetY + y) : offsetY + y;
			int bottomRow = flip ? codedHeight - 2 - (offsetY + y) : offsetY + y + 1;
			int topBase = topRow * stride + offsetX;
			int bottomBase = bottomRow * stride + offsetX;
			int chromaRow = flip ? (codedHeight >> 1) - 1 - ((offsetY + y) >> 1) : (offsetY + y) >> 1;
			int chromaBase = uvOffset + chromaRow * stride + offsetX;
			int targetTop = y * width;
			int targetBottom = targetTop + width;
			for (int x = 0; x < evenWidth; x += 2) {
				int u = source[chromaBase + x + 1] & 0xFF;
				int v = source[chromaBase + x] & 0xFF;
				int redBase = rTable[v];
				int greenBase = gTableU[u] + gTableV[v];
				int blueBase = bTable[u];
				int topLeft = yTable[source[topBase + x] & 0xFF];
				int topRight = yTable[source[topBase + x + 1] & 0xFF];
				int bottomLeft = yTable[source[bottomBase + x] & 0xFF];
				int bottomRight = yTable[source[bottomBase + x + 1] & 0xFF];
				out[targetTop + x] = pack(topLeft, redBase, greenBase, blueBase);
				out[targetTop + x + 1] = pack(topRight, redBase, greenBase, blueBase);
				out[targetBottom + x] = pack(bottomLeft, redBase, greenBase, blueBase);
				out[targetBottom + x + 1] = pack(bottomRight, redBase, greenBase, blueBase);
			}
		}
		if (evenHeight < toRow)
			convertRow(source, stride, uvOffset, codedHeight, offsetX, offsetY, width, evenHeight, flip,
					out, yTable, rTable, gTableU, gTableV, bTable);
	}

	private static void convertRow(byte[] source, int stride, int uvOffset, int codedHeight,
			int offsetX, int offsetY, int width, int y, boolean flip, int[] out,
			int[] yTable, int[] rTable, int[] gTableU, int[] gTableV, int[] bTable) {
		int lumaRow = flip ? codedHeight - 1 - (offsetY + y) : offsetY + y;
		int lumaBase = lumaRow * stride + offsetX;
		int chromaRow = flip ? (codedHeight >> 1) - 1 - ((offsetY + y) >> 1) : (offsetY + y) >> 1;
		int chromaBase = uvOffset + chromaRow * stride + offsetX;
		int targetBase = y * width;
		int evenWidth = width & ~1;
		for (int x = 0; x < evenWidth; x += 2) {
			int u = source[chromaBase + x + 1] & 0xFF;
			int v = source[chromaBase + x] & 0xFF;
			int redBase = rTable[v];
			int greenBase = gTableU[u] + gTableV[v];
			int blueBase = bTable[u];
			out[targetBase + x] = pack(yTable[source[lumaBase + x] & 0xFF], redBase, greenBase, blueBase);
			out[targetBase + x + 1] = pack(yTable[source[lumaBase + x + 1] & 0xFF], redBase, greenBase,
					blueBase);
		}
		if ((width & 1) != 0) {
			int x = evenWidth;
			int chroma = chromaBase + x;
			int u = source[chroma + 1] & 0xFF;
			int v = source[chroma] & 0xFF;
			out[targetBase + x] = pack(yTable[source[lumaBase + x] & 0xFF],
					rTable[v], gTableU[u] + gTableV[v], bTable[u]);
		}
	}

	private static void fill(int[] yTable, int[] rTable, int[] gTableU, int[] gTableV, int[] bTable,
			int yCoefficient, int redCoefficient, int greenUCoefficient, int greenVCoefficient,
			int blueCoefficient) {
		for (int index = 0; index < 256; index++) {
			yTable[index] = (yCoefficient * (index - 16) + 128) >> 8;
			rTable[index] = (redCoefficient * (index - 128)) >> 8;
			gTableU[index] = (greenUCoefficient * (index - 128)) >> 8;
			gTableV[index] = (greenVCoefficient * (index - 128)) >> 8;
			bTable[index] = (blueCoefficient * (index - 128)) >> 8;
		}
	}

	private static int pack(int luma, int redBase, int greenBase, int blueBase) {
		int red = clamp(luma + redBase);
		int green = clamp(luma + greenBase);
		int blue = clamp(luma + blueBase);
		return 0xFF000000 | (blue << 16) | (green << 8) | red;
	}

	private static int clamp(int value) {
		return value < 0 ? 0 : value > 255 ? 255 : value;
	}

	private static ThreadFactory daemonFactory() {
		return runnable -> {
			Thread thread = new Thread(runnable, "solim-video-convert");
			thread.setDaemon(true);
			return thread;
		};
	}
}
