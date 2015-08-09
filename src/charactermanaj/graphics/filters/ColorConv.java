package charactermanaj.graphics.filters;

import charactermanaj.graphics.filters.ColorConvertFilter.ColorReplace;

/**
 * 色置換オブジェクトの列挙子.<br>
 * 「キャラクターなんとか機」のカラー変更にあわせて、画像は青・白(黒)の濃淡だけで表現し、
 * RGBのマッピングを変えることで色変換する.(色調変換ではない。)<br>
 * G成分はないがしろにされている。
 * @author seraphy
 */
public enum ColorConv implements ColorReplace {

	/**
	 * 変換なし
	 */
	NONE {
		@Override
		public void convert(int[] rgb) {
			// do nothing.
		}
	},
	/**
	 * 青系.<br>
	 * RGB成分をRRBにする.<br> 
	 */
	BLUE {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[0];
		}
	},
	/**
	 * 紫系.<br>
	 * RGB成分をBRBにする.<br>
	 */
	VIOLET {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[0];
			rgb[0] = rgb[2];
		}
	},
	/**
	 * 赤系.<br>
	 * RGB成分をBRRにする.<br>
	 */
	RED {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[0];
			rgb[0] = rgb[2];
			rgb[2] = rgb[1];
		}
	},
	/**
	 * 黄系.<br>
	 * RGB成分をBBRにする.<br>
	 */
	YELLOW {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[2];
			rgb[2] = rgb[0];
			rgb[0] = rgb[1];
		}
	},
	/**
	 * 緑系.<br>
	 * RGB成分をRBRにする.<br>
	 */
	GREEN {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[2];
			rgb[2] = rgb[0];
		}
	},
	/**
	 * シアン系にする.<br>
	 * RGB成分をRBBにする.<br>
	 */
	CYAN {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[2];
		}
	},
	/**
	 * 黒系にする.<br>
	 * RGB成分をRRRにする.<br>
	 */
	BLACK {
		@Override
		public void convert(int[] rgb) {
			rgb[1] = rgb[0];
			rgb[2] = rgb[0];
		}
	},
	/**
	 * 白系にする.<br>
	 * RGB成分をBBBにする.<br>
	 */
	WHITE {
		@Override
		public void convert(int[] rgb) {
			rgb[0] = rgb[2];
			rgb[1] = rgb[2];
		}
	};
	
	/**
	 * 色置換する.
	 */
	public abstract void convert(int[] rgb);

}
