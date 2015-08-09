package charactermanaj.graphics.filters;

import java.awt.Color;

import charactermanaj.graphics.colormodel.HSYColorModel;

public class BackgroundColorFilter extends AbstractFilter {

	/**
	 * 背景モード.
	 * @author seraphy
	 */
	public enum BackgroundColorMode {
		
		ALPHABREND(false, false, 1) {
			@Override
			public void filter(BackgroundColorFilter me, int[] pixcels) {
				me.alphabrend(pixcels);
			}
		},
		
		OPAQUE(false, true, 2) {
			@Override
			public void filter(BackgroundColorFilter me, int[] pixcels) {
				me.opaque(pixcels);
			}
		},
		
		GRAYSCALE(true, false, 4) {
			@Override
			public void filter(BackgroundColorFilter me, int[] pixcels) {
				me.grayscale(pixcels);
			}
		},
		
		DRAW_ALPHA(true, true, 8) {
			@Override
			public void filter(BackgroundColorFilter me, int[] pixcels) {
				me.drawAlpha(pixcels);
			}
		};
		
		private final boolean grayscale;
		
		private final boolean noAlphachanel;
		
		private final int mask;

		public abstract void filter(BackgroundColorFilter me, int[] pixcels);
		
		BackgroundColorMode(boolean grayscale, boolean noAlphachanel, int mask) {
			this.grayscale = grayscale;
			this.noAlphachanel = noAlphachanel;
			this.mask = mask;
		}
		
		public boolean isNoAlphaChannel() {
			return this.noAlphachanel;
		}
		
		public boolean isGrayscale() {
			return this.grayscale;
		}
		
		public int mask() {
			return mask;
		}

		public static BackgroundColorMode valueOf(boolean noAlphachanel, boolean grayscale) {
			for (BackgroundColorMode mode : values()) {
				if (mode.isNoAlphaChannel() == noAlphachanel && mode.isGrayscale() == grayscale) {
					return mode;
				}
			}
			throw new RuntimeException("構成に誤りがあります.");
		}
		
	}
	
	/**
	 * 背景モード
	 */
	private BackgroundColorMode mode;
	
	/**
	 * 背景色、グレースケールまたはアルファ表示モードでは不要
	 */
	private Color bgColor;


	/**
	 * 背景モードと背景色を指定して背景色描画フィルタを構築する.
	 * @param mode モード
	 * @param bgColor 背景色、(グレースケールまたはアルファではnull化)
	 */
	public BackgroundColorFilter(BackgroundColorMode mode, Color bgColor) {
		if (mode == null) {
			// モードは必須.
			throw new IllegalArgumentException();
		}
		if (!mode.isGrayscale() && bgColor == null) {
			// グレースケールもしくはアルファ表示モード以外は背景色は必須.
			throw new IllegalArgumentException();
		}
		
		this.mode = mode;
		this.bgColor = bgColor;
	}
	
	@Override
	protected void filter(int[] pixcels) {
		mode.filter(this, pixcels);
	}
	
	/**
	 * 普通のアルファブレンドします.
	 * @param pixcels
	 */
	public void alphabrend(int[] pixcels) {
		int br = bgColor.getRed();
		int bg = bgColor.getGreen();
		int bb = bgColor.getBlue();

		final int mx = pixcels.length;
		for (int idx = 0; idx < mx; idx++) {
			int argb = pixcels[idx];

			int b = argb & 0xff;
			int g = (argb >>>= 8) & 0xff;
			int r = (argb >>>= 8) & 0xff;
			int a = (argb >>>= 8) & 0xff;
			
			if (a == 0) {
				// 完全透過ならば背景色まま
				b = bb;
				g = bg;
				r = br;

			} else if (a != 0xff) {
				// 完全非透過でなければアルファブレンド
				b = ((b * a) / 0xff + (bb * (0xff - a) / 0xff)) & 0xff;
				g = ((g * a) / 0xff + (bg * (0xff - a) / 0xff)) & 0xff;
				r = ((r * a) / 0xff + (br * (0xff - a) / 0xff)) & 0xff;
			}

			argb = 0xff000000 | (r << 16) | (g << 8) | b;
			pixcels[idx] = argb;
		}
	}

	/**
	 * 完全透過の部分のみ背景色を設定し、それ以外は元の色のままアルファを取り除く.<br>
	 * @param pixcels ピクセルデータ
	 */
	public void opaque(int[] pixcels) {
		int bgRgb = bgColor.getRGB();

		final int mx = pixcels.length;
		for (int idx = 0; idx < mx; idx++) {
			int argb = pixcels[idx];
			int a = (argb >>> 24) & 0xff;
			int rgb = (argb & 0xffffff);
			
			if (a == 0) {
				rgb = bgRgb;
			}
			
			argb = 0xff000000 | rgb;
			pixcels[idx] = argb;
		}
	}

	/**
	 * アルファを取り除き、グレスケールで表現する.<br>
	 * RGBチャネルのうち、RBはアルファ適用されたグレースケールで、
	 * Gチャネルはアルファ未適用のグレースケールで表現される.<br>
	 * @param pixcels ピクセルデータ
	 */
	public void grayscale(int[] pixcels) {
		final int mx = pixcels.length;
		for (int idx = 0; idx < mx; idx++) {
			int argb = pixcels[idx];
			
			int b = argb & 0xff;
			int g = (argb >>>= 8) & 0xff;
			int r = (argb >>>= 8) & 0xff;
			int a = (argb >>>= 8) & 0xff;

			int gray_brend = 0;
			int gray_plain = 0;
			if (a != 0) {
				// 輝度の算定(グレースケール化)
				gray_brend = HSYColorModel.getGrayscale(r, g, b);
				gray_plain = gray_brend;
				
				if (a != 0xff) {
					// アルファによる調整
					gray_brend = ((gray_brend * a) / 0xff) & 0xff;
				}
			}
			
			argb = 0xff000000 | (gray_brend << 16) | (gray_plain << 8) | gray_brend;
			pixcels[idx] = argb;
		}
	}

	/**
	 * アルファチャネルとグレースケールを重ねて表示する.<br>
	 * アルファ未適用のグレースケールをRチャネル、
	 * アルファを無し・半透明・透明の3段階にしたものをGチャネル、
	 * アルファをBチャネルで表現する.<br>
	 * Gチャネルは、完全透過は濃緑(0x80)、完全非透過は黒(0xff)、半透明は明緑(0xff)となる.<br>
	 * @param pixcels ピクセルデータ
	 */
	public void drawAlpha(int[] pixcels) {
		final int mx = pixcels.length;
		for (int idx = 0; idx < mx; idx++) {
			int argb = pixcels[idx];
			
			int b = argb & 0xff;
			int g = (argb >>>= 8) & 0xff;
			int r = (argb >>>= 8) & 0xff;
			int a = (argb >>>= 8) & 0xff;

			int gray_plain = (r + g + b) / 3;
			int alpha_off = (a == 0) ? 0x80 : (a == 0xff) ? 0x00 : 0xff;

			argb = 0xff000000 | (gray_plain << 16) | (alpha_off << 8) | a;
			pixcels[idx] = argb;
		}
	}
}
