package charactermanaj.graphics.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * 画像リソース
 * @author seraphy
 */
public interface ImageResource extends Comparable<ImageResource> {

	/**
	 * 画像リソースをストリームで取得します.
	 * @return 入力ストリーム
	 * @throws IOException 開けなかった場合
	 */
	InputStream openStream() throws IOException;
	
	/**
	 * 更新日時
	 * @return 更新日時を示すエポックタイム
	 */
	long lastModified();
	
	/**
	 * 同値用ハッシュ
	 * @return ハッシュ
	 */
	int hashCode();
	
	/**
	 * 同値判定
	 * @param obj 比較対象
	 * @return 同一であればtrue
	 */
	boolean equals(Object obj);
	
	/**
	 * ソート用比較
	 */
	int compareTo(ImageResource o);
	
	/**
	 * リソース位置を示すフルネーム
	 * @return リソース位置を示すフルネーム
	 */
	String getFullName();

	/**
	 * リソース位置を示すURI
	 * @return リソース位置を示すURI
	 */
	URI getURI();
}
