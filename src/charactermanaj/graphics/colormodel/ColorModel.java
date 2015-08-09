package charactermanaj.graphics.colormodel;

/**
 * カラーモデル.
 * 
 * @author seraphy
 */
public interface ColorModel {

	/**
	 * カラーモデルの説明
	 * 
	 * @return 説明
	 */
	String getTitle();

	/**
	 * 各項目の説明
	 * 
	 * @param index
	 *            インデックス(0-2)
	 * @return 説明
	 */
	String getItemTitle(int index);

	/**
	 * RGBからHSVに変換する.
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param hsvVals
	 * @return
	 */
    float[] RGBtoHSV(int r, int g, int b, float[] hsvVals);

	/**
	 * HSVからRGBに変換する.
	 * 
	 * @param hue
	 * @param sat
	 * @param lum
	 * @return
	 */
    int HSVtoRGB(float hue, float sat, float lum);

}
