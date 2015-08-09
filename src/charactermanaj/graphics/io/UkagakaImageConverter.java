package charactermanaj.graphics.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 伺か用PNG変換クラス.
 * @author seraphy
 */
public class UkagakaImageConverter {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(UkagakaImageConverter.class.getName());
	
	/**
	 * シングルトン
	 */
	private static final UkagakaImageConverter inst = new UkagakaImageConverter();
	
	/**
	 * シングルトンコンストラクタ
	 */
	protected UkagakaImageConverter() {
		super();
	}
	
	public static UkagakaImageConverter getInstance() {
		return inst;
	}
	
	/**
	 * 伺か用PNA(アルファチャネルのグレースケール表現)に変換する.
	 * @param img 変換元の透過画像(TYPE_INT_ARGB専用)
	 * @return 伺か用PNA画像(TYPE_INT_RGB, アルファチャネルのグレースケール表現)
	 */
	public BufferedImage createUkagakaPNA(BufferedImage img) {
		if (img == null) {
			throw new IllegalArgumentException("引数にnullは指定できません。");
		}
		if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new IllegalArgumentException("TYPE_INT_ARGB専用です.");
		}

		int w = img.getWidth();
		int h = img.getHeight();
		
		Raster raster = img.getData();
		
		BufferedImage outimg = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

		// アルファ値をグレースケール表現に変換
		int[] argb = null;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				argb = raster.getPixel(x, y, argb);
				int a = argb[3]; // alpha
				int o = a << 16 | a << 8 | a;
				outimg.setRGB(x, y, o);
			}
		}
		
		return outimg;
	}
	
	/**
	 * 伺か用PNGに変換するための透過色に設定できる色を選択する.<br>
	 * 該当がない場合(選択できなかった場合)はnullを返す.<br>
	 * @param img 変換元の透過画像(TYPE_INT_ARGB専用)
	 * @return 伺か用PNG画像(非透過)の透過色キー、該当がない場合はnull
	 */
	public Color detectTransparentColorKey(BufferedImage img) {
		if (img == null) {
			throw new IllegalArgumentException("引数にnullは指定できません。");
		}
		if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new IllegalArgumentException("TYPE_INT_ARGB専用です.");
		}
		
		int w = img.getWidth();
		int h = img.getHeight();
		
		Raster raster = img.getData();
		
		// 512色インデックスグループ化
		final int colorMx = 512;
		int[] colorCounts = new int[colorMx];
		
		// ピクセル単位の512色インデックスごとの使用数を計測
		int[] argb = new int[4];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				argb = raster.getPixel(x, y, argb);
				int a = argb[3];
				if (a == 0) {
					continue; // 完全透過はノーカウント
				}
				// 上位3ビットのみ
				int r = (argb[0] >>> 5) & 0x07;
				int g = (argb[1] >>> 5) & 0x07;
				int b = (argb[2] >>> 5) & 0x07;
				// インデックス生成
				int idx = r << 6 | g << 3 | b;
				colorCounts[idx]++;
			}
		}
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "counts=" + Arrays.toString(colorCounts));
		}
		
		// 色インデックスの色相と選択候補の重み
		float[] colorHues = new float[colorMx];
		float[] hsb = new float[3];
		// 候補の重み
		float[] colorWeights = new float[colorMx];
		// 上位3ビットインデックスから8ビットRGB値への変換マップ
		int[] colorMap = new int[colorMx];
		for (int idx = 0; idx < colorMx; idx++) {
			int r = ((idx >>> 6) & 0x07);
			int g = ((idx >>> 3) & 0x07);
			int b = ((idx) & 0x07);
			
			r = r << 5 | 0x1f;
			g = g << 5 | 0x1f;
			b = b << 5 | 0x1f;

			hsb = Color.RGBtoHSB(r, g, b, hsb);
			
			colorHues[idx] = hsb[0];
			colorWeights[idx] = hsb[1]; // 濃さを重みとする.
			colorMap[idx] = r << 16 | g << 8 | b;
		}
		
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "weight=" + Arrays.toString(colorWeights));
		}

		// 色インデックスごとの未使用色相の個数カウント
		float[] unusedColorScore = new float[colorMx];
		float tolerance = 0.0625f;
		for (int idx = 0; idx < colorMx; idx++) {
			if (colorCounts[idx] > 0) {
				// 使用していればスキップ.
				continue;
			}
			// 類似色相のスコア
			float hue = colorHues[idx];
			float score = 0.f;
			for (int ref = 0; ref < colorMx; ref++) {
				if (colorCounts[ref] > 0) {
					// 使用していればスキップ
					continue;
				}
				float refHue = colorHues[ref];
				// 色相が等しいものを1、色相がズレるほどに0に近づく
				float diff = (tolerance - Math.abs(hue - refHue)) / tolerance;
				if (diff < 0) {
					// 範囲を超えていればスキップ
					continue;
				}
				score += diff;
			}
			
			// 色による重み付け
			float weight = colorWeights[idx];
			unusedColorScore[idx] = score * weight;
		}
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "scores=" + Arrays.toString(unusedColorScore));
		}
		
		// もっとも重いスコアを選択
		float maxScore = 0;
		int maxIdx = -1;
		for (int idx = 0; idx < colorMx; idx++) {
			float score = unusedColorScore[idx];
			if (score > maxScore) {
				maxScore = score;
				maxIdx = idx;
			}
		}
		
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "selectedIdx=" + maxIdx + "/score=" + maxScore);
		}

		// 透過用色の取得
		if (maxIdx >= 0) {
			return new Color(colorMap[maxIdx]);
		}
		
		// 候補を見つけられなかった場合.
		return null;
	}

	/**
	 * 伺か用PNGに変換する.<br>
	 * @param img 変換元の透過画像(TYPE_INT_ARGB専用)
	 * @param transparentColorKey 透過色キー、nullの場合は自動選択
	 * @return 伺か用PNG画像(TYPE_INT_RGB, 左上に透過色指定あり)
	 */
	public BufferedImage createUkagakaPNG(BufferedImage img, Color transparentColorKey) {
		if (img == null) {
			throw new IllegalArgumentException("引数にnullは指定できません。");
		}
		if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new IllegalArgumentException("TYPE_INT_ARGB専用です.");
		}

		// 透過色に設定するカラーキーの取得
		if (transparentColorKey == null) {
			transparentColorKey = detectTransparentColorKey(img);
		}

		int transparencyColor;
		if (transparentColorKey != null) {
			transparencyColor = transparentColorKey.getRGB() & 0xffffff;

		} else {
			// カラーキーの取得がでなければ、黒に限りなく近い非黒を透過色として代替する.
			logger.log(Level.INFO, "透過色の選択ができなかったため、0x010101で代用します.");
			transparencyColor = 0x010101;
		}

		int w = img.getWidth();
		int h = img.getHeight();
		
		Raster raster = img.getData();

		// 完全な透過ピクセルに対して算定した透過色を割り当て、
		// 画像の左上に透過色を設定する.
		int argb[] = new int[4];
		BufferedImage outimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				argb = raster.getPixel(x, y, argb);
				int a = argb[3]; // alpha
				int c;
				if (a == 0) {
					// 完全透過の場合のみ透過色を設定
					c = transparencyColor;

				} else {
					// それ以外はアルファを無視してRGBのみ
					int r = argb[0];
					int g = argb[1];
					int b = argb[2];
					c = r << 16 | g << 8 | b;
				}
				outimg.setRGB(x, y, c);
			}

			// 左上(0,0)に透過とする色を設定
			outimg.setRGB(0, 0, transparencyColor);
		}
		
		return outimg;
	}
}
