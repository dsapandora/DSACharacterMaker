package charactermanaj.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * リソース名を指定してローカライズされたテキストを取得するための抽象実装.<br>
 * リソースの取得部は派生クラスにて実装する必要がある.<br>
 * 
 * @author seraphy
 */
public abstract class LocalizedTextResource {

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
		return getText(name, cs, Locale.getDefault());
	}

	/**
	 * リソース名と文字コードを指定して、ロケールに対応する文字列を取得する.<br>
	 * リソースがなければ実行時例外が発生する.
	 * 
	 * @param name
	 *            リソース名
	 * @param cs
	 *            文字コード
	 * @param locale
	 *            取得するロケール
	 * @return テキスト
	 */
	public String getText(String name, Charset cs, Locale locale) {
		ResourceNames resourceNames = createResourceNames(name, locale);
		String text = loadText(resourceNames, cs);
		if (text == null) {
			throw new RuntimeException("resource not found: " + resourceNames);
		}
		return text;
	}

	/**
	 * リソース名とロケールを指定して読み込む実リソース名のグループを作成して返す.
	 * 
	 * @param name
	 *            リソース名
	 * @param locale
	 *            ロケール
	 * @return リソース名グループ(優先順)
	 */
	protected ResourceNames createResourceNames(String name, Locale locale) {
		if (name == null || name.length() == 0 || locale == null) {
			throw new IllegalArgumentException();
		}

		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();

		int extpos = name.lastIndexOf(".");
		int folderpos = name.lastIndexOf("/");

		String basename;
		String ext;
		if (folderpos > extpos) {
			basename = name;
			ext = "";
		} else {
			basename = name.substring(0, extpos);
			ext = name.substring(extpos);
		}

		String[] resourceNamesStr = {
				basename + "_" + language + "_" + country + "_" + variant + ext,
				basename + "_" + language + "_" + country + ext,
				basename + "_" + language + ext, basename + ext,};

		return new ResourceNames(resourceNamesStr);
	}

	/**
	 * リソース名グループを指定して、リソースをテキストとして取得する.<br>
	 * リソース名グループの優先順にリソースの取得を試みて最初に成功したものを返す.<br>
	 * ひとつも成功しなければnullが返される.<br>
	 * 
	 * @param resourceNames
	 *            リソース名グループ
	 * @param cs
	 *            文字コード
	 * @return リソースのテキスト
	 */
	protected String loadText(ResourceNames resourceNames, Charset cs) {
		if (resourceNames == null || cs == null) {
			throw new IllegalArgumentException();
		}

		for (String resourceName : resourceNames) {
			URL url = getResource(resourceName);
			if (url == null) {
				// リソースがなければ次の候補へスキップする.
				continue;
			}
			StringBuilder buf = new StringBuilder();
			try {
				InputStream is = url.openStream();
				try {
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(is, cs));
					try {
						int ch;
						while ((ch = rd.read()) != -1) {
							buf.append((char) ch);
						}
					} finally {
						rd.close();
					}
				} finally {
					is.close();
				}
			} catch (IOException ex) {
				throw new RuntimeException("resource loading error: " + ex, ex);
			}
			// 1つでも成功すれば、それで終了する.
			return buf.toString();
		}
		// 一つも成功しなかった場合
		return null;
	}

	/**
	 * リソース名からリソースを取得する.<br>
	 * 存在しなければnullを返す.<br>
	 * 
	 * @param resourceName
	 *            リソース名
	 * @return リソース、またはnull
	 */
	protected abstract URL getResource(String resourceName);
}
