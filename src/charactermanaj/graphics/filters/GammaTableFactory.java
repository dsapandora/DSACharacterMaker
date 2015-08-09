package charactermanaj.graphics.filters;

/**
 * ガンマ補正値を構築するためのファクトリ.
 * @author seraphy
 */
public class GammaTableFactory implements TableFactory {

	/**
	 * ARGBの、それぞれのガンマ補正値の配列
	 */
	private float[] gammas;
	
	/**
	 * ARGBすべてが同一のガンマ値で構築する.
	 * @param gamma ガンマ値
	 */
	public GammaTableFactory(float gamma) {
		setGamma(gamma);
	}
	
	/**
	 * ARGBそれぞれ異なるガンマ値で構築する.<br>
	 * @param gammas ガンマ値
	 */
	public GammaTableFactory(float[] gammas) {
		setGamma(gammas);
	}

	/**
	 * ARGBすべてが同一のガンマ値を設定する.
	 * @param gamma ガンマ値
	 */
	public final void setGamma(float gamma) {
		setGamma(new float[] {gamma, gamma, gamma, gamma});
	}
	
	/**
	 * ARGBそれぞれ異なるガンマ値を設定する.<br>
	 * @param gammas ガンマ値
	 */
	public final void setGamma(float[] gammas) {
		if (gammas == null || gammas.length < 3) {
			throw new IllegalArgumentException();
		}
		this.gammas = gammas;
	}
	
	/**
	 * ARGB/RGBの、それぞれのガンマ値に対する0-255の範囲に対する補正後の値を格納する
	 * 二次元配列を構築する.<br>
	 * @return ガンマテーブル
	 */
	public int[][] createTable() {
		int mx = gammas.length;
		int[][] gammaTbls = new int[mx][];
		for (int i = 0; i < 4; i++) {
			float gamma;
			if (i < mx) {
				gamma = gammas[i];
			} else {
				gamma = 1.f;
			}
			gammaTbls[i] = createGamma(gamma);
		}
		return gammaTbls;
	}
	
	/**
	 * ガンマ値に対する0-255の入力に対する、そのガンマ補正値の配列を返す.<br>
	 * @param gamma ガンマ値
	 * @return ガンマテーブル
	 */
	private int[] createGamma(float gamma) {
		if (gamma < 0.01f) {
			gamma = 0.01f;
		}
		int gammaTbl[] = new int[256];
		for (int gi = 0; gi <= 0xff; gi++) {
			gammaTbl[gi] = (int)(Math.pow(gi / 255.0, 1 / gamma) * 255) & 0xff;
		}
		return gammaTbl;
	}
	
}
