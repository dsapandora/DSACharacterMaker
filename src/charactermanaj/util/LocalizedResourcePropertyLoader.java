package charactermanaj.util;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * xml形式のリソース上のプロパティファイルのローカライズされた読み込みを行うためのクラス.<br>
 * リソースは、単純名、言語名を末尾に付与したもの、言語名と国を末尾に付与したもの、言語名と国とバリアントを末尾に付与したもの、の順で読み取られる.<br>
 * 順番に読み込んで重ね合わせる.<br>
 * 一度読み込んだものはキャッシュに保存され次回以降は、それが用いられる.<br>
 */
public class LocalizedResourcePropertyLoader extends ResourceLoader {
	
	/**
	 * プロパティファイル群と、それに対するキャッシュ
	 */
	private Map<ResourceNames, Properties> propCache;
	
	/**
	 * キャッシュを共有するシングルトンインスタンス.
	 */
	private static final LocalizedResourcePropertyLoader inst = new LocalizedResourcePropertyLoader(
			new HashMap<ResourceNames, Properties>());
	
	/**
	 * 独立したキャッシュを指定することのできるコンストラクタ.<br>
	 * 
	 * @param propCache
	 *            キャッシュ、不要であればnull可
	 */
	public LocalizedResourcePropertyLoader(
			Map<ResourceNames, Properties> propCache) {
		this.propCache = propCache;
	}
	
	/**
	 * インスタンスを取得する
	 * 
	 * @return インスタンス
	 */
	public static LocalizedResourcePropertyLoader getCachedInstance() {
		return inst;
	}
	
	/**
	 * リソース名を指定してデフォルトのロケールでローカライズされたリソースプロパティを読み込む.<br>
	 * リソースはxml形式である。リソース名には.xmlを付与しない.(自動的に内部で付与される.)
	 * 
	 * @param name
	 *            リソース名
	 * @return プロパティ
	 */
	public Properties getLocalizedProperties(String name) {
		return getLocalizedProperties(name, null);
	}
	
	/**
	 * リソース名を指定して指定したロケールでローカライズされたリソースプロパティを読み込む.<br>
	 * リソースはxml形式である。リソース名には.xmlを付与しない.(自動的に内部で付与される.)
	 * 
	 * @param name
	 *            リソース名
	 * @param locale
	 *            ロケール、nullの場合はデフォルトのロケール
	 * @return プロパティ
	 */
	public Properties getLocalizedProperties(String name, Locale locale) {
		return getProperties(getResourceNames(name, locale));
	}

	/**
	 * リソース名を指定して指定したロケールでローカライズされたリソースプロパティの一覧を取得する.<br>
	 * リソースはxml形式である。リソース名には.xmlを付与しない.(自動的に内部で付与される.)<br>
	 * 返される順序は、読み込み順となる。(順番に読み込んで上書きしてゆくことを想定する).<br>
	 * ロケール中立のものが先頭となり、指定したロケールにもっとも一致するものが最後となる.<br>
	 * 
	 * @param name
	 *            リソース名
	 * @param locale
	 *            ロケール、nullの場合はデフォルトのロケール
	 * @return プロパティリソースの一覧(読み込み順)
	 */
	public static ResourceNames getResourceNames(String name, Locale locale) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException();
		}
		if (locale == null) {
			locale = Locale.getDefault();
		}

		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		
		String[] resourceNames = {
			name + ".xml",
			name + "_" + language + ".xml",
			name + "_" + language + "_" + country + ".xml",
			name + "_" + language + "_" + country + "_" + variant + ".xml",
		};
		return new ResourceNames(resourceNames);
	}

	/**
	 * リソース名群をもとにキャッシュもしくはプロパティをロードして返す.<br>
	 * キャッシュされていない場合はプロパティをロードして、それをキャッシュに格納する.<br>
	 * (共有キャッシュ時、もしくは独自のキャッシュが指定されている場合).<br>
	 * リソースが一つも存在しない場合は実行時例外を発生させる.<br>
	 * 
	 * @param resourceNames
	 *            リソース名群
	 * @return プロパティ
	 */
	public Properties getProperties(ResourceNames resourceNames) {
		if (resourceNames == null) {
			throw new IllegalArgumentException();
		}
		Properties prop;
		if (propCache != null) {
			synchronized (propCache) {
				prop = propCache.get(resourceNames);
				if (prop == null) {
					prop = loadProperties(resourceNames);
					propCache.put(resourceNames, prop);
				}
			}
		} else {
			prop = loadProperties(resourceNames);
		}
		if (prop == null) {
			throw new RuntimeException("missing resource: " + resourceNames);
		}
		return prop;
	}

	/**
	 * リソース名群からリソースプロパティをロードして返す.<br>
	 * 一つも存在しない場合はnullを返す.<br>
	 * 
	 * @param resourceNames
	 *            リソース群名
	 * @return プロパティ
	 */
	protected Properties loadProperties(ResourceNames resourceNames) {
		if (resourceNames == null) {
			throw new IllegalArgumentException();
		}

		// システム埋め込みリソースでプロパティを取得したのちに、ユーザ指定のプロパティの内容で上書きする.
		// バージョンアップによりキーが増えて、既存のローカルファイル上のプロパティファイルにキーが存在しない場合でも
		// 安全なようにするためのもの。
		ClassLoader[] loaders = new ClassLoader[] {
				getDefaultClassLoader(),
				getLocalizedClassLoader(null),
				};
		
		boolean foundResource = false;
		Properties props = new Properties();
		for (ClassLoader loader : loaders) {
			if (loader == null) {
				continue;
			}
			for (String resourceName : resourceNames) {
				URL resource = loader.getResource(resourceName);
				if (resource != null) {
					Properties org = new Properties();
					try {
						InputStream is = resource.openStream();
						try {
							org.loadFromXML(is);
						} finally {
							is.close();
						}
					} catch (Exception ex) {
						throw new RuntimeException("resource loading error." + resource, ex);
					}
					foundResource = true;
					
					props.putAll(org);
				}
			}
		}
		
		if (foundResource) {
			return props;
		}
		return null;
	}
}
