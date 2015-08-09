package charactermanaj.graphics;

import java.io.IOException;

import charactermanaj.graphics.colormodel.ColorModel;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.LoadedImage;

/**
 * 画像リソースを読み込み、カラー変換した画像を返す.
 * @author seraphy
 *
 */
public interface ColorConvertedImageLoader {

	/**
	 * 画像リソースをロードし色変換した結果のBufferedImageを返します.<br>
	 * 返される形式はARGBに変換されています.<br>
	 * 
	 * @param file
	 *            画像リソース
	 * @param colorConvParam
	 *            色変換パラメータ、nullの場合はデフォルト
	 * @param colorModel
	 *            カラーモデル
	 * @return 画像イメージ
	 * @throws IOException
	 *             形式が不明であるか、ファィルがないか読み取りに失敗した場合
	 */
	LoadedImage load(ImageResource file, ColorConvertParameter colorConvParam,
			ColorModel colorModel) throws IOException;
	
}
