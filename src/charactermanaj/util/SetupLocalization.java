package charactermanaj.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 言語リソースを管理する.
 * 
 * @author seraphy
 */
public class SetupLocalization extends ResourceLoader {
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	public static final String DIRNAME_RESOURCES = "resources";

	/**
	 * リソースフォルダ下のサブディレクトリ一覧.<br>
	 */
	public enum Resources {
		Languages("languages"), Menu("menu"), Template("template");

		private final String dirName;

		private Resources(String dirName) {
			this.dirName = dirName;
		}

		public String getDirName() {
			return dirName;
		}

		@Override
		public String toString() {
			return getDirName();
		}
	}

	private File baseDir;
	
	/**
	 * アプリケーションデータ用ディレクトリを指定して構築する.
	 * 
	 * @param baseDir
	 *            データディレクトリ
	 */
	public SetupLocalization(File baseDir) {
		if (baseDir == null || !baseDir.isDirectory()) {
			throw new IllegalArgumentException();
		}
		this.baseDir = baseDir;
	}
	
	/**
	 * コピー対象とするリソース一覧を取得する.<br>
	 * 
	 * @param resourceSet
	 *            リソースディレクトリのサブディレクトリ名のリスト
	 * @return リソース一覧(言語関連リソース、テンプレートなど)
	 * @throws IOException
	 *             失敗
	 */
	protected Collection<String> getResourceList(EnumSet<Resources> resourceSet)
			throws IOException {
		if (resourceSet == null) {
			resourceSet = EnumSet.noneOf(Resources.class);
		}
		ArrayList<String> resources = new ArrayList<String>();

		ClassLoader cl = getClass().getClassLoader();

		for (Resources resourceKey : resourceSet) {
			String name = resourceKey.getDirName();
			URL loc = cl.getResource(name);
			if (loc == null) {
				continue;
			}
			String protocol = loc.getProtocol();
			if ("file".equals(protocol)) {
				// ファイル上にクラスやリソースがある場合
				try {
					File dir = new File(loc.toURI());
					File[] files = dir.listFiles();
					if (files != null) {
						for (File file : files) {
							if (file.isDirectory()) {
								continue;
							}
							resources.add(name + "/" + file.getName());
						}
					}
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}

			} else if ("jar".equals(protocol)) {
				// jarにクラスやリソースがある場合
				JarURLConnection conn = (JarURLConnection) loc.openConnection();
				JarEntry dirEntry = conn.getJarEntry();
				assert dirEntry != null; // "jar:file:xxxx.jar!yyyy" のyyyyの部分
				String prefix = dirEntry.getName() + "/";
				JarFile jarFile = conn.getJarFile();
				try {
					Enumeration<JarEntry> enm = jarFile.entries();
					while (enm.hasMoreElements()) {
						JarEntry entry = enm.nextElement();
						if (entry.isDirectory()) {
							continue;
						}
						String entryName = entry.getName();
						if (entryName.startsWith(prefix)) {
							resources.add(entryName);
						}
					}
				} finally {
					if (!conn.getUseCaches()) {
						// キャッシュしてある場合は明示的にクローズしない.
						// (そもそもクローズする必要はないかも)
						// (たぶん、システムなどがインスタンスを再利用していると思われるため)
						// (jdk5でクローズすると例外が発生する。jdk7のリビジョンによっても発生するようだ)
						// http://bugs.sun.com/view_bug.do?bug_id=7050028
						jarFile.close();
					}
				}
			}
		}

		logger.log(Level.FINE, "resource list: " +resources);
		return resources;
	}
	
	/**
	 * リソースをファイルにコピーする.<br>
	 * 
	 * @param fromURL
	 * @param toFile
	 * @throws IOException
	 */
	protected void copyResource(URL fromURL, File toFile) throws IOException {
		logger.log(Level.INFO, "copy resource '" + fromURL + "' to '" + toFile + "'");
		File dir = toFile.getParentFile();
		if ( !dir.exists()) {
			if ( !dir.mkdirs()) {
				throw new IOException("can't create directory. " + dir);
			}
		}

		URLConnection conn = fromURL.openConnection();
		conn.setDoInput(true);
		InputStream is = conn.getInputStream();
		try {
			long lastModified = conn.getLastModified();
			OutputStream os = new FileOutputStream(toFile);
			try {
				byte[] buf = new byte[4096];
				for (;;) {
					int rd = is.read(buf);
					if (rd <= 0) {
						break;
					}
					os.write(buf, 0, rd);
				}
			} finally {
				os.close();
			}
			boolean result = toFile.setLastModified(lastModified);
			logger.log(Level.FINE, "setLastModified(" + toFile+ ") succeeded=" + result);
		} finally {
			is.close();
		}
	}
	
	/**
	 * リソースディレクトリを返す.
	 * 
	 * @return リソースディレクトリ
	 */
	public File getResourceDir() {
		try {
			return new File(baseDir, DIRNAME_RESOURCES).getCanonicalFile();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * ローカルシステム上のアプリケーションデータディレクトリに言語リソースをコピーする.
	 * 
	 * @param resourceSet
	 *            コピーするリソースセット.
	 * @param overwrite
	 *            上書きを許可する場合はtrue、スキップする場合はfalse
	 * @throws IOException
	 *             失敗
	 */
	public void setupToLocal(EnumSet<Resources> resourceSet, boolean overwrite)
			throws IOException {
		File toDir = getResourceDir();
		ClassLoader cl = getDefaultClassLoader();
		for (String resourceName : getResourceList(resourceSet)) {
			URL url = cl.getResource(resourceName);
			if (url != null) {
				File toFile = new File(toDir, resourceName).getCanonicalFile();
				if (overwrite || !toFile.exists()) {
					// 上書き許可か、まだファイルが存在しなければコピーする.
					copyResource(url, toFile);
				}

			} else {
				logger.log(Level.WARNING, "missing resource: " + resourceName);
			}
		}
	}
}
