package charactermanaj.graphics.colormodel;

public enum ColorModels implements ColorModel {

	/**
	 * HSB(色相・彩度・明度)
	 */
	HSB(new HSBColorModel()),

	/**
	 * HSY(色相・彩度・輝度)
	 */
	HSY(new HSYColorModel());

	/**
	 * デフォルトのカラーモデル.<br>
	 */
	public static ColorModels DEFAULT = HSB;

	/**
	 * カラーモデル名からカラーモデルを取得する.<br>
	 * nullまたは空文字、または該当がない場合はデフォルトを採用する.<br>
	 * 
	 * @param colorModelName
	 *            カラーモデル名
	 * @return カラーモデル
	 */
	public static ColorModels safeValueOf(String colorModelName) {
		try {
			if (colorModelName != null && colorModelName.length() > 0) {
				return valueOf(colorModelName);
			}

		} catch (RuntimeException ex) {
			// 何もしない.
		}
		return DEFAULT;
	}

	private final ColorModel colorModel;

	ColorModels(ColorModel colorModel) {
		this.colorModel = colorModel;
	}

	public String getTitle() {
		return colorModel.getTitle();
	}

	public String getItemTitle(int index) {
		return colorModel.getItemTitle(index);
	}

	public int HSVtoRGB(float hue, float sat, float lum) {
		return colorModel.HSVtoRGB(hue, sat, lum);
	}

	public float[] RGBtoHSV(int r, int g, int b, float[] hsvVals) {
		return colorModel.RGBtoHSV(r, g, b, hsvVals);
	}
}
