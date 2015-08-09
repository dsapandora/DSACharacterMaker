package charactermanaj.util;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;


/**
 * リソースからローカライズされたテキストを取得する.<br>
 * 
 * @author seraphy
 * 
 */
public class LocalizedResourceTextLoader extends ResourceLoader {

	private static final LocalizedResourceTextLoader inst = new LocalizedResourceTextLoader();
	
	private LocalizedTextResource textResource = new LocalizedTextResource() {
		@Override
		protected URL getResource(String resourceName) {
			return LocalizedResourceTextLoader.this.getResource(resourceName);
		}
	};

	private LocalizedResourceTextLoader() {
		super();
	}
	
	public static LocalizedResourceTextLoader getInstance() {
		return inst;
	}
	
	/**
	 * リソース名を指定して、テキストファイルを読み込んで、その文字列を返す.<br>
	 * リソースは現在のデフォルトロケールを優先で検索されます.<br>
	 * ファイルエンコーディングを引数csで指定する.<br>
	 * 
	 * @param name
	 *            リソース名
	 * @param cs
	 *            ファイルのエンコーディング
	 * @return ファイルの内容(テキスト)
	 */
	public String getText(String name, Charset cs) {
		return textResource.getText(name, cs);
	}

	public String getText(String name, Charset cs, Locale locale) {
		return textResource.getText(name, cs, locale);
	}
}
