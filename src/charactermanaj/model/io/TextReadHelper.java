package charactermanaj.model.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * テキストの読み込みヘルパー.<br>
 * 
 * @author seraphy
 */
public final class TextReadHelper {

	/**
	 * プライベートコンストラクタ
	 */
	private TextReadHelper() {
		super();
	}

	/**
	 * 入力ストリームを指定して、テキストファイルを読み込みます.<br>
	 * 入力ストリームは内部で閉じられます.<br>
	 * 
	 * @param is
	 *            入力ストリーム
	 * @return テキスト、もしくはnull
	 * @throws IOException
	 *             失敗
	 */
	public static String readTextTryEncoding(InputStream is) throws IOException {
		if (is == null) {
			return null;
		}

		// 一旦メモリに取り込む
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			int ch;
			while ((ch = is.read()) != -1) {
				bos.write((byte) ch);
			}
		} finally {
			is.close();
		}
		byte[] buf = bos.toByteArray();

		String enc = null;
		if (buf.length >= 2) {
			// Windowsのメモ帳はUTF-16にBOMをつけるので、これで判定できる。
			// 本アプリケーションのエクスポート時もUTF-16LEのBOM付きで出力する。
			// 一般的なエディタはUTF-16BEにはBOMをつけないので、事前に判定することはできない。
			if ((buf[0] & 0xff) == 0xff && (buf[1] & 0xff) == 0xfe) {
				enc = "UTF-16LE";
			} else if ((buf[0] & 0xff) == 0xfe && (buf[1] & 0xff) == 0xff) {
				enc = "UTF-16BE";
			}
		}
		if (enc == null && buf.length >= 3) {
			if ((buf[0] & 0xff) == 0xef && (buf[1] & 0xff) == 0xbb
					&& (buf[1] & 0xff) == 0xbf) {
				// Windowsのメモ帳などはUTF-8にBOMをつけるので、これで判定できる。
				// 一般的なエディタではUTF-8のBOMはつけないのでUTF-8であるかどうかを事前判定することはできない。
				enc = "UTF-8";
			}
		}
		if (enc == null) {
			// BOMがない場合はMS932かEUC_JPのいずれかであろう、と仮定する。
			enc = "JISAutoDetect"; // SJIS/EUC_JPの自動判定
		}

		// 文字列として変換
		StringBuilder str = new StringBuilder();
		InputStreamReader rd = new InputStreamReader(new ByteArrayInputStream(
				buf), enc);
		try {
			int ch;
			while ((ch = rd.read()) != -1) {
				str.append((char) ch);
			}
		} finally {
			rd.close();
		}

		// 改行コードをプラットフォーム固有のものに変換
		String line = str.toString();
		line = line.replace("\r\n", "\n");
		line = line.replace("\r", "\n");
		line = line.replace("\n", System.getProperty("line.separator"));

		return line;
	}

}
