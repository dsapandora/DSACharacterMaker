package charactermanaj.model.util;

import java.io.File;
import java.io.FileFilter;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.AppConfig;
import charactermanaj.model.WorkingSet;
import charactermanaj.model.io.WorkingSetPersist;
import charactermanaj.model.io.WorkingSetXMLWriter;
import charactermanaj.ui.RecentCharactersDir;
import charactermanaj.util.FileUserData;
import charactermanaj.util.UserData;
import charactermanaj.util.UserDataFactory;


/**
 * 開始前の事前準備するためのサポートクラス
 * 
 * @author seraphy
 */
public abstract class StartupSupport {
	
	private static StartupSupport inst;
	
	/**
	 * インスタンスを取得する.
	 * 
	 * @return シングルトンインスタンス
	 */
	public static synchronized StartupSupport getInstance() {
		if (inst == null) {
			inst = new StartupSupport() {
				private final Logger logger = Logger.getLogger(StartupSupport.class.getName());

				@Override
				public void doStartup() {
					StartupSupport[] startups = {
							new PurgeOldLogs(),
							new ConvertRecentCharDirsSerToXmlProps(),
							new ConvertWorkingSetSerToXml(),
							new PurgeOldCaches(),
					};
					for (StartupSupport startup : startups) {
						logger.log(Level.FINE, "startup operation start. class="
								+ startup.getClass().getSimpleName());
						try {
							startup.doStartup();
							logger.log(Level.FINE, "startup operation is done.");

						} catch (Exception ex) {
							logger.log(Level.WARNING, "startup operation failed.", ex);
						}
					}
				}
			};
		}
		return inst;
	}
	
	/**
	 * スタートアップ処理を実施します.
	 */
	public abstract void doStartup();
}

/**
 * シリアライズによるキャラクターディレクトリリストの保存をXML形式のPropertiesにアップグレードします。
 * 
 * @author seraphy
 * 
 */
class ConvertRecentCharDirsSerToXmlProps extends StartupSupport {
	/**
	 * ロガー
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public void doStartup() {
		// 旧形式(ver0.991以前)
		UserDataFactory factory = UserDataFactory.getInstance();

		final String FILENAME = "recent-characterdirs.ser";
		File prevFile = new File(factory.getSpecialDataDir(FILENAME), FILENAME);

		try {
			if (prevFile.exists()) {
				FileUserData recentCharDirs = new FileUserData(prevFile);
				RecentCharactersDir obj = (RecentCharactersDir) recentCharDirs
						.load();

				// 新しい形式で保存する.
				obj.saveRecents();

				// 古いファイルを削除する
				prevFile.delete();
			}

		} catch (Exception ex) {
			logger.log(Level.WARNING, FILENAME + " convert failed.", ex);
		}
	}
}

/**
 * シリアライズによるキャラクターディレクトリリストの保存をXML形式のPropertiesにアップグレードします。
 * 
 * @author seraphy
 * 
 */
class ConvertWorkingSetSerToXml extends StartupSupport {
	/**
	 * ロガー
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public void doStartup() {
		final String FILENAME = "workingset.ser";
		try {
			UserDataFactory userDataFactory = UserDataFactory.getInstance();
			File dir = userDataFactory.getSpecialDataDir(FILENAME);
			if (!dir.exists()) {
				return;
			}
			File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					String name = pathname.getName();
					return name.endsWith(FILENAME);
				}
			});
			if (files == null) {
				logger.log(Level.WARNING, "cache-dir access failed. " + dir);
				return;
			}
			WorkingSetXMLWriter wr = new WorkingSetXMLWriter();
			for (File file : files) {
				FileUserData fileData = new FileUserData(file);
				if (fileData.exists()) {
					try {
						// serファイルをデシリアライズする.
						WorkingSet ws = (WorkingSet) fileData.load();
						URI docBase = ws.getCharacterDocBase();
						if (docBase != null) {
							// XML形式で保存しなおす.
							UserData workingSetXmlData = userDataFactory
									.getMangledNamedUserData(docBase,
											WorkingSetPersist.WORKINGSET_FILE_SUFFIX);
							if (!workingSetXmlData.exists()) {
								// XML形式データがまだない場合のみ保存しなおす.
								OutputStream outstm = workingSetXmlData
										.getOutputStream();
								try {
									wr.writeWorkingSet(ws, outstm);
								} finally {
									outstm.close();
								}
							}
						}

						// serファイルは削除する.
						fileData.delete();

					} catch (Exception ex) {
						logger.log(Level.WARNING,
								FILENAME + " convert failed.", ex);
					}
				}
			}

		} catch (Exception ex) {
			logger.log(Level.WARNING, FILENAME + " convert failed.", ex);
		}
	}
}

/**
 * 古いログファイルを消去する.
 * 
 * @author seraphy
 */
class PurgeOldLogs extends StartupSupport {

	/**
	 * ロガー
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public void doStartup() {
		UserDataFactory userDataFactory = UserDataFactory.getInstance();
		File logsDir = userDataFactory.getSpecialDataDir("*.log");
		if (logsDir.exists()) {
			AppConfig appConfig = AppConfig.getInstance();
			long purgeOldLogsMillSec = appConfig.getPurgeLogDays() * 24L * 3600L * 1000L;
			if (purgeOldLogsMillSec > 0) {
				File[] files = logsDir.listFiles();
				if (files == null) {
					logger.log(Level.WARNING, "log-dir access failed.");
					return;
				}
				long purgeThresold = System.currentTimeMillis()
						- purgeOldLogsMillSec;
				for (File file : files) {
					try {
						String name = file.getName();
						if (file.isFile() && file.canWrite()
								&& name.endsWith(".log")) {
							long lastModified = file.lastModified();
							if (lastModified > 0
									&& lastModified < purgeThresold) {
								boolean result = file.delete();
								logger.log(Level.INFO, "remove file " + file
										+ "/succeeded=" + result);
							}
						}

					} catch (Exception ex) {
						logger.log(Level.WARNING,
								"remove file failed. " + file, ex);
					}
				}
			}
		}
	}
}

/**
 * 古いキャッシュファイルを消去する.<br>
 * -character.xml-cache.ser, -favorites.serは、直接xmlでの読み込みになったため、 ただちに消去しても問題ない.<br>
 * recent-character.serは、使用されなくなったため、ただちに消去して良い.<br>
 * mangled_info.xmlは、*.serを消去したあとには不要となるため、消去する.<br>
 * (今後使われることはない)
 * 
 * @author seraphy
 */
class PurgeOldCaches extends StartupSupport {

	/**
	 * ロガー
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public void doStartup() {
		UserDataFactory userDataFactory = UserDataFactory.getInstance();
		File cacheDir = userDataFactory.getSpecialDataDir(".ser");
		if (cacheDir.exists()) {
			File[] files = cacheDir.listFiles();
			if (files == null) {
				logger.log(Level.WARNING, "cache-dir access failed.");
				return;
			}
			for (File file : files) {
				try {
					if (!file.isFile() || !file.canWrite()) {
						// ファイルでないか、書き込み不可の場合はスキップする.
						continue;
					}
					String name = file.getName();
					if (name.endsWith("-character.xml-cache.ser")
							|| name.endsWith("-favorites.ser")
							|| name.equals("recent-character.ser")
							|| name.equals("mangled_info.xml")) {
						boolean result = file.delete();
						logger.log(Level.INFO, "remove file " + file
								+ "/succeeded=" + result);
					}

				} catch (Exception ex) {
					logger.log(Level.WARNING, "remove file failed. " + file, ex);
				}
			}
		}
	}
}
