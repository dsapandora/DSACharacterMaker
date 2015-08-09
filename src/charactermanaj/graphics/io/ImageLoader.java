package charactermanaj.graphics.io;

import java.io.IOException;


/**
 * 画像をロードします.<br>
 * @author seraphy
 */
public interface ImageLoader {
	
	/**
	 * 画像リソースからBufferedImageを返します.<br>
	 * 返される形式はARGBに変換されています.<br>
	 * @param imageResource 画像リソース
	 * @throws IOException 読み取りに失敗した場合、もしくは画像の形式が不明な場合
	 */
	LoadedImage load(ImageResource imageResource) throws IOException;

}
