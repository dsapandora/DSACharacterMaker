package charactermanaj.clipboardSupport;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.IOException;


/**
 * クリップボード用ユーテリティクラス.<br>
 * @author seraphy
 */
public final class ClipboardUtil {
	
	private ClipboardUtil() {
		super();
	}
	
	/**
	 * クリップボードにイメージを設定する.<br>
	 * JDKのクリップボード経由の画像転送では透過色を表現できないので、背景色を指定する必要がある.<br>
	 * (ただし、このアプリケーション内であれば透過色を維持したままコピー可能.)<br>
	 * @param img イメージ
	 * @param bgColor 背景色
	 */
	public static void setImage(BufferedImage img, Color bgColor) {
		if (img == null || bgColor == null) {
			throw new IllegalArgumentException();
		}

		Toolkit tk  = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		
		ImageSelection imageSelection = new ImageSelection(img, bgColor);
		cb.setContents(imageSelection, null);
	}

	/**
	 * クリップボード内にイメージがあるか?
	 * @return イメージがあればtrue
	 */
	public static boolean hasImage() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		return ImageSelection.isSupprotedFlavorAvailable(cb);
	}
	
	/**
	 * クリップボードからイメージを取得する.<br>
	 * 取得できる形式がない場合はnullを返す.<br>
	 * @return 画像、もしくはnull
	 * @throws IOException 読み取り中に例外が発生した場合
	 */
	public static BufferedImage getImage() throws IOException {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		return ImageSelection.getImage(cb);
	}
	
}
