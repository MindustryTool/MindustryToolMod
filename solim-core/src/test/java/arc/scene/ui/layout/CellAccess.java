package arc.scene.ui.layout;

/** Helper for unit testing package-private Arc Cell padding fields. */
public final class CellAccess {
	private CellAccess() {}

	public static float padTop(Cell<?> cell) {
		return cell.padTop;
	}

	public static float padLeft(Cell<?> cell) {
		return cell.padLeft;
	}

	public static float padBottom(Cell<?> cell) {
		return cell.padBottom;
	}

	public static float padRight(Cell<?> cell) {
		return cell.padRight;
	}

	public static int expandX(Cell<?> cell) {
		return cell.expandX;
	}

	public static int expandY(Cell<?> cell) {
		return cell.expandY;
	}

	public static float fillX(Cell<?> cell) {
		return cell.fillX;
	}

	public static float fillY(Cell<?> cell) {
		return cell.fillY;
	}

	public static int align(Cell<?> cell) {
		return cell.align;
	}

	public static float minWidth(Cell<?> cell) {
		return cell.minWidth;
	}

	public static float maxWidth(Cell<?> cell) {
		return cell.maxWidth;
	}

	public static float minHeight(Cell<?> cell) {
		return cell.minHeight;
	}

	public static float maxHeight(Cell<?> cell) {
		return cell.maxHeight;
	}
}
