package charactermanaj.graphics.filters;

import charactermanaj.graphics.colormodel.ColorModel;

/**
 * 色変換フィルタ.<br>
 * @author seraphy
 */
public class ColorConvertFilter extends AbstractFilter {

	/**
	 * 色置換用インターフェイス.<br>
	 * @author seraphy
	 */
	public interface ColorReplace {

		/**
		 * R,G,Bの順に格納されている色データに対して編集を行う.<br> 
		 * @param rgb 編集対象
		 */
		void convert(int[] rgb);
	}

	/**
	 * カラーモデル
	 */
	private final ColorModel colorModel;

	/**
	 * 色置換オブジェクト
	 */
	private final ColorReplace colorReplace;
	
	/**
	 * HSB値の各オフセット(3要素)
	 */
	private final float[] hsbOffsets;
	
	/**
	 * 淡色化率.<br>
	 * 0に近づくほどグレースケールに近づく.<br>
	 * 0で完全なグレースケール、1の場合は色の変更はなし。
	 */
	private final float grayLevel;

	/**
	 * ガンマ値補正用テーブル.<br>
	 */
	private final int[][] gammaTbl;
	
	/**
	 * コントラスト補正用テーブル
	 */
	private final int[][] contrastTbl;
	
	/**
	 * 色変換フィルタを構築する.<br>
	 * 
	 * @param colorModel
	 *            カラーモデル
	 * @param colorReplace
	 *            色置換オブジェクト、不要ならばnull
	 * @param hsbOffsets
	 *            HSBオフセット(3要素)、不要ならばnull
	 * @param grayLevel
	 *            淡色化率、1でそのまま、0でグレースケール化。
	 * @param gammaTableFactory
	 *            ガンマ補正値ファクトリ、不要ならばnull
	 * @param contrastTableFactory
	 *            コントラスト補正ファクトリ、不要ならばnull
	 */
	public ColorConvertFilter(
			ColorModel colorModel,
			ColorReplace colorReplace,
			float[] hsbOffsets, float grayLevel,
			GammaTableFactory gammaTableFactory,
			ContrastTableFactory contrastTableFactory) {
		if (colorModel == null) {
			throw new IllegalArgumentException();
		}
		this.colorModel = colorModel;

		if (gammaTableFactory == null) {
			gammaTableFactory = new GammaTableFactory(1.f);
		}
		if (contrastTableFactory == null) {
			contrastTableFactory = new ContrastTableFactory(1.f);
		}
		if (hsbOffsets != null && hsbOffsets.length < 3) {
			throw new IllegalArgumentException("hsbOffset too short.");
		}
		if (hsbOffsets != null) {
			if (hsbOffsets[0] == 0 && hsbOffsets[1] == 0 && hsbOffsets[2] == 0) {
				hsbOffsets = null;
			} else {
				hsbOffsets = (float[]) hsbOffsets.clone();
			}
		}
		if (grayLevel < 0) {
			grayLevel = 0;
		} else if (grayLevel > 1) {
			grayLevel = 1.f;
		}
		this.grayLevel = grayLevel;
		this.gammaTbl = gammaTableFactory.createTable();
		this.contrastTbl = contrastTableFactory.createTable();
		this.hsbOffsets = hsbOffsets;
		this.colorReplace = colorReplace;
	}
	
	/**
	 * ピクセルデータに対して色変換を行う.<br>
	 * @param pixcels ピクセルデータ
	 */
	protected void filter(int[] pixcels) {
		final float grayLevel = this.grayLevel;
		final float negGrayLevel = 1.f - grayLevel;

		// グレースケール変換テーブル
		int[] precalc = new int[256];
		int[] negPrecalc = new int[256];
		for (int i = 0; i < 256; i++) {
			precalc[i] = (int)(i * grayLevel) & 0xff;
			negPrecalc[i] = (int)(i * negGrayLevel) & 0xff;
		}

		// 全ピクセルに対して計算を行う
		final ColorReplace colorReplace = this.colorReplace;
		int[] rgbvals = new int[3];
		final float[] hsbOffsets = this.hsbOffsets;
		final float[] hsvvals = new float[3];
		final int[][] gammaTbl = this.gammaTbl;
		final int mx = pixcels.length;
		for (int i = 0; i < mx; i++) {
			int argb = pixcels[i];
			
			// ガンマ変換
			int a = gammaTbl[0][(argb >> 24) & 0xff];
			int r = gammaTbl[1][(argb >> 16) & 0xff];
			int g = gammaTbl[2][(argb >> 8) & 0xff];
			int b = gammaTbl[3][(argb) & 0xff];
			
			// 色交換
			if (colorReplace != null) {
				rgbvals[0] = r;
				rgbvals[1] = g;
				rgbvals[2] = b;

				colorReplace.convert(rgbvals);
				
				r = rgbvals[0];
				g = rgbvals[1];
				b = rgbvals[2];
			}
			
			// 輝度
			int br = ((77 * r + 150 * g + 29 * b) >> 8) & 0xff;

			// 輝度(グレースケール)に近づける
			r = ((int)(precalc[r] + negPrecalc[br])) & 0xff;
			g = ((int)(precalc[g] + negPrecalc[br])) & 0xff;
			b = ((int)(precalc[b] + negPrecalc[br])) & 0xff;

			// 色調変換
			if (hsbOffsets != null) {
				colorModel.RGBtoHSV(r, g, b, hsvvals);
				for (int l = 0; l < 3; l++) {
					hsvvals[l] += hsbOffsets[l];
				}
				for (int l = 1; l < 3; l++) {
					if (hsvvals[l] < 0) {
						hsvvals[l] = 0;
					} else if (hsvvals[l] > 1.f) {
						hsvvals[l] = 1.f;
					}
				}
				int rgb = colorModel.HSVtoRGB(hsvvals[0], hsvvals[1], hsvvals[2]);
				r = (rgb >> 16) & 0xff;
				g = (rgb >> 8) & 0xff;
				b = (rgb) & 0xff;
			}

			// コントラスト変換
			r = contrastTbl[0][r];
			g = contrastTbl[1][g];
			b = contrastTbl[2][b];
			argb = (a << 24) | (r << 16) | (g << 8) | b;

			pixcels[i] = argb;
		}
	}
}
