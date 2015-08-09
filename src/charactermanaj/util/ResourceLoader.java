package charactermanaj.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;


/**
 * リソースをロードするための抽象基底クラス.
 * 
 * @author seraphy
 */
public class ResourceLoader {

	/**
	 * クラスローダを取得する.<br>
	 * まずローカルファイル上のリソースディレクトリがあれば、それを検索する.<br>
	 * つぎにスレッドに関連づけられているコンテキストクラスローダか、もしなければ、このクラスをロードしたクラスローダを用いて検索する.<br>
	 * 
	 * @return クラスローダ
	 */
	public ClassLoader getClassLoader() {
		return getLocalizedClassLoader(getDefaultClassLoader());
	}
	
	/**
	 * クラスローダを取得する.<br>
	 * スレッドに関連づけられているコンテキストクラスローダか、もしなければ、このクラスをロードしたクラスローダを返す.<br>
	 * 
	 * @return クラスローダ
	 */
	public ClassLoader getDefaultClassLoader() {
		return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			public ClassLoader run() {
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				if (cl == null) {
					cl = ResourceLoader.class.getClassLoader();
				}
				return cl;
			}
		});
	}
	
	/**
	 * ローカルファイル上のリソースディレクトリにアクセスするクラスローダ取得する.<br>
	 * 作成されていなければparentをそのまま返す.<br>
	 * リソースはローカルファイル上のパスで検索されたのちにparentで検索されます.(標準のURLClassLoaderとは違う探索方法)<br>
	 * 
	 * @param parent
	 *            親クラスローダ、nullの場合は親の探索をしない.
	 * @return ローカルシステム上のリソースディレクトリにアクセスするクラスローダ、なければparentのまま
	 */
	public ClassLoader getLocalizedClassLoader(final ClassLoader parent) {
		try {
			File baseDir = ConfigurationDirUtilities.getUserDataDir();
			SetupLocalization localize = new SetupLocalization(baseDir);
			final File resourceDir = localize.getResourceDir();
			if (!resourceDir.exists() || !resourceDir.isDirectory()) {
				return parent;
			}
			URLClassLoader cl = AccessController.doPrivileged(new PrivilegedExceptionAction<URLClassLoader>() {
				public URLClassLoader run() throws MalformedURLException {
					URL[] urls = new URL[] { resourceDir.toURI().toURL() };
					return new URLClassLoader(urls, parent) {
						@Override
						public URL getResource(String name) {
									URL url = findResource(name); // 子が優先 (標準と逆)
							if (url == null) {
								ClassLoader parent = getParent();
								if (parent != null) {
									url = parent.getResource(name);
								}
							}
							return url;
						}
					};
				}
			});
			return cl;

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * クラスローダによりリソースをロードする.<br>
	 * 該当するリソースが存在しない場合はnullを返す.<br>
	 * リソース名がnullの場合もnullを返す.<br>
	 * 
	 * @param name
	 *            リソース名またはnull
	 * @return リソースがあれば、そのURL。なければnull
	 */
	public URL getResource(String name) {
		if (name == null) {
			return null;
		}
		return getClassLoader().getResource(name);
	}
}
