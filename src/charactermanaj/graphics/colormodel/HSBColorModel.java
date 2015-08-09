package charactermanaj.graphics.colormodel;

import java.awt.Color;

/*
 * Java標準のHSBカラーモデル.<br>
 */
public class HSBColorModel implements ColorModel {

	private static final String[] ITEM_TITLES = {
			"colorModel.HSB.hue",
			"colorModel.HSB.saturation",
			"colorModel.HSB.brightness",
			};

	public String getTitle() {
		return "colorModel.HSB.title";
	}

	public String getItemTitle(int index) {
		return ITEM_TITLES[index];
	}

	/**
	 * RGBからHSBに変換する.
	 */
	public float[] RGBtoHSV(int r, int g, int b, float[] hsvVals) {
		return Color.RGBtoHSB(r, g, b, hsvVals);
	}

	/**
	 * HSBからRGBに変換する.
	 */
	public int HSVtoRGB(float hue, float saturation, float brightness) {
		return Color.HSBtoRGB(hue, saturation, brightness);
	}
}
