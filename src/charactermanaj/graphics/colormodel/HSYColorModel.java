package charactermanaj.graphics.colormodel;

/**
 * HSYカラーモデルの計算.<br>
 * "gununuの日記"さんのところのC++計算ルーチンをJava用に書き直したもの.<br>
 * 
 * @author seraphy
 * @see http://d.hatena.ne.jp/gununu/20090721/1248171222
 */
public class HSYColorModel implements ColorModel {

	/**
	 * 輝度計算用の係数R
	 */
	private static final float IR = 0.298912f;

	/**
	 * 輝度計算用の係数G
	 */
	private static final float IG = 0.586611f;

	/**
	 * 輝度計算用の係数B
	 */
	private static final float IB = 0.114478f;

	private static final String[] ITEM_TITLES = {
			"colorModel.HSY.hue",
			"colorModel.HSY.saturation",
			"colorModel.HSY.luminance",
			};

	public String getTitle() {
		return "colorModel.HSY.title";
	}

	public String getItemTitle(int index) {
		return ITEM_TITLES[index];
	}

	/**
	 * RGBから輝度を求める.
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @return 輝度
	 */
	public static int getGrayscale(int r, int g, int b) {
		return (int) (IR * r + IG * g + IB * b) & 0xff;
	}

	/**
	 * RGBからHSYに変換する.<br>
	 * 
	 * @param rr
	 *            0-255範囲のR
	 * @param gg
	 *            0-255範囲のG
	 * @param bb
	 *            0-255範囲のB
	 * @param hsyVals
	 *            H色相, S彩度, Y輝度を0-1の実数表現した配列(書き込み先)
	 * @return 引数と同じhsyvalsを返す.
	 */
	public float[] RGBtoHSV(int r, int g, int b, float[] hsyVals) {
		if (hsyVals == null || hsyVals.length < 3) {
			throw new IllegalArgumentException();
		}

		int max = Math.max(Math.max(r, g), b);
		int min = Math.min(Math.min(r, g), b);
		float saturation = (max - min) / 255.f;

		float rr = r / 255.f;
		float gg = g / 255.f;
		float bb = b / 255.f;
		float lum = (IR * rr + IG * gg + IB * bb);
		if (lum > 1.f) {
			lum = 1.f;
		}

		float hue;
		if (saturation == 0) {
			hue = 0;

		} else {
			if (max == r) {
				hue = (gg - bb) / saturation * 60f;
			} else if (max == g) {
				hue = (bb - rr) / saturation * 60f + 120f;
			} else {
				hue = (rr - gg) / saturation * 60f + 240f;
			}
			if (hue < 0) {
				hue += 360f;
			}
			hue /= 360f;
		}

		hsyVals[0] = hue;
		hsyVals[1] = saturation;
		hsyVals[2] = lum;
		return hsyVals;
	}

	/**
	 * HSYからRGBに変換する.<Br>
	 * 
	 * @param hue
	 *            0-1範囲の色相
	 * @param sat
	 *            0-1範囲の彩度
	 * @param lum
	 *            0-1範囲の輝度
	 * @return RGB値
	 * @see http://d.hatena.ne.jp/gununu/20090721/1248171222
	 */
	public int HSVtoRGB(float hue, float sat, float lum) {
		hue = (hue - (float) Math.floor(hue));
		if (sat < 0) {
			sat = 0f;
		} else if (sat > 1f) {
			sat = 1f;
		}
		if (lum < 0) {
			lum = 0f;
		} else if (lum > 1f) {
			lum = 1f;
		}

		float r, g, b;
		if (hue <= 1 / 6.0f) {
			float h = hue * 6;
			r = (1 - IR - IG * h);
			g = (-IR + (1 - IG) * h);
			b = (-IR - IG * h);

		} else if (hue <= 3 / 6.0f) {
			float h = (hue - 1 / 3.0f) * 6;
			if (hue > 2 / 6.0f) {
				r = (-IG - IB * h);
				g = (1 - IG - IB * h);
				b = (-IG + (1 - IB) * h);

			} else {
				r = (-IG + (IR - 1) * h);
				g = (1 - IG + IR * h);
				b = (-IG + IR * h);
			}

		} else if (hue <= 5 / 6.0f) {
			float h = (hue - 2 / 3.0f) * 6;
			if (hue > 4 / 6.0f) {
				r = (-IB + (1 - IR) * h);
				g = (-IB - IR * h);
				b = (1 - IB - IR * h);

			} else {
				r = (-IB + IG * h);
				g = (-IB + (IG - 1) * h);
				b = (1 - IB + IG * h);
			}

		} else {
			float h = (hue - 1) * 6;
			r = (1 - IR + IB * h);
			g = (-IR + IB * h);
			b = (-IR + (IB - 1) * h);
		}

		r *= sat;
		g *= sat;
		b *= sat;

		float ma = Math.max(r, Math.max(g, b));
		float mi = Math.min(r, Math.min(g, b));
		float x = 1;
		float t;
		if (ma + lum > 1f) {
			t = (1f - lum) / ma;
			x = t;
		}

		if (mi + lum < 0) {
			t = lum / (-mi);
			if (t < x) {
				x = t;
			}
		}

		int red = (int) ((lum + r * x) * 255) & 0xff;
		int green = (int) ((lum + g * x) * 255) & 0xff;
		int blue = (int) ((lum + b * x) * 255) & 0xff;

		return 0xff000000 | red << 16 | green << 8 | blue;
	}
}
