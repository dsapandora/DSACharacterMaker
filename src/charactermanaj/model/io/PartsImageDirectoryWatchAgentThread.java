package charactermanaj.model.io;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;

public class PartsImageDirectoryWatchAgentThread implements Runnable {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(PartsImageDirectoryWatchAgent.class.getName());
	
	private final CharacterData characterData;
	
	private final File baseDir;
	
	/**
	 * リスナーとサスペンド要求マップのロックオブジェクト
	 */
	private final Object lockListeners = new Object();
	
	/**
	 * 監視を通知されるリスナー.
	 */
	private LinkedList<PartsImageDirectoryWatchListener> listeners
			= new LinkedList<PartsImageDirectoryWatchListener>();

	/**
	 * サスペンド要求のマップ.
	 */
	private IdentityHashMap<PartsImageDirectoryWatchListener, Integer> suspendStateMap
			= new IdentityHashMap<PartsImageDirectoryWatchListener, Integer>();
	
	
	/**
	 * ロックオブジェクト
	 */
	private final Object lock = new Object();
	
	
	/**
	 * 停止フラグ
	 */
	private volatile boolean stopFlag;
	
	/**
	 * 監視インターバル
	 */
	private int dirWatchInterval;
	
	/**
	 * スレッド、生成されていなければnull
	 */
	private Thread thread;
	
	/**
	 * 監視結果1、まだ監視されていなければnull
	 */
	private volatile Long signature;
	
	/**
	 * 監視結果2、検出されたアイテムの個数
	 */
	private volatile int itemCount;
	
	/**
	 * 監視結果3、検出されたアイテムの最終更新日。ただし未来の日付は除外する。
	 */
	private volatile long maxLastModified;

	
	
	public PartsImageDirectoryWatchAgentThread(CharacterData characterData) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		URI docBase = characterData.getDocBase();
		File baseDir = null;
		if (docBase != null && "file".equals(docBase.getScheme())) {
			baseDir = new File(docBase).getParentFile();
		}

		this.characterData = characterData;
		this.baseDir = baseDir;
		this.dirWatchInterval = AppConfig.getInstance().getDirWatchInterval();
	}
	
	public CharacterData getCharcterData() {
		return characterData;
	}
	
	/**
	 * 監視を開始する.<br>
	 * 一定時間待機後、フォルダを監視し、前回監視結果と比較して変更があれば通知を行う.<br>
	 * その処理をstop指示が来るまで繰り返す.<br>
	 * 開始しても、まず指定時間待機するため、すぐにはディレクトリの走査は行わない.<br>
	 * 且つ、初回監視結果は変更通知されないため、二回以上走査しないかぎり通知は発生しない.<br>
	 * これは意図したものである.<br>
	 * 明示的に初回監視を行うには明示的に{@link #reset()}を呼び出す.<br>
	 */
	private void start() {
		logger.log(Level.FINE, "watchAgent request start. " + this);
		synchronized (lock) {
			if (thread != null && thread.isAlive()) {
				// すでに開始済みであれば何もしない.
				return;
			}
			// 書き込み禁止キャラクター定義に対する監視を無効とするか?
			AppConfig appConfig = AppConfig.getInstance();
			if (appConfig.isDisableWatchDirIfNotWritable()) {
				try {
					if (baseDir == null || !baseDir.exists()
							|| !baseDir.isDirectory() || !baseDir.canWrite()) {
						logger.log(Level.INFO, "does not monitor the directory because it is not writable." + baseDir);
						return;
					}
					URI docBase = characterData.getDocBase();
					if (docBase != null) {
						File docBaseFile = new File(docBase);
						if (!docBaseFile.exists() || !docBaseFile.canWrite()) {
							logger.log(Level.INFO,
									"does not monitor the directory because the character.xml is not writable."
											+ characterData);
							return;
						}
					}
				} catch (Exception ex) {
					logger.log(Level.WARNING, "watch-dir start failed. " + characterData, ex);
					return;
				}
			}
			// スレッドを開始する.
			stopFlag = false;
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
	}
	
	/**
	 * 監視を停止する.<br>
	 * 
	 * @return 停止した場合はtrue、すでに停止していたか開始されていない場合はfalse
	 */
	private boolean stop() {
		logger.log(Level.FINE, "watchAgent request stop. " + this);
		boolean stopped = false;
		synchronized (lock) {
			// スレッドが生きていれば停止する.
			if (thread != null && thread.isAlive()) {
				stopFlag = true;
				thread.interrupt();
				try {
					thread.join(10000); // 10Secs

				} catch (InterruptedException ex) {
					logger.log(Level.FINE, "stop request interrupted.", ex);
					// 抜ける
				}
				stopped = true;
			}
			
			// スレッドは停止されていると見なす.
			thread = null;
		}
		return stopped;
	}
	
	
	/**
	 * スレッドの停止フラグがたてられるまで、一定時間待機と監視とを永久に繰り返す.<br>
	 * ただし、スレッド自身はデーモンとして動作させているので他の非デーモンスレッドが存在しなくなれば停止する.<br>
	 */
	public void run() {
		logger.log(Level.FINE, "watch-dir thead started. " + this);

		// 初回スキャンは無視するためリセット状態とする.
		this.signature = null;
		
		// スキャンループ
		while (!stopFlag) {
			try {
				Thread.sleep(dirWatchInterval);
				watch(new Runnable() {
					public void run() {
						fireWatchEvent();
					}
				});

			} catch (InterruptedException ex) {
				logger.log(Level.FINE, "watch-dir thead interrupted.");
				// 何もしない
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "PartsImageDirectoryWatchAgent failed.", ex);
				// 何もしない.
			}
		}
		logger.log(Level.FINE, "watch-dir thead stopped. " + this);
	}
	
	/**
	 * 監視を行う.<br>
	 * 停止フラグが設定されるか、割り込みされた場合は処理を中断してInterruptedException例外を返して終了する.<br>
	 * 
	 * @param notifier
	 *            通知するためのオブジェクト
	 * @throws InterruptedException
	 *             割り込みされた場合
	 */
	protected void watch(Runnable notifier) throws InterruptedException {
		if (baseDir == null || !baseDir.exists() || !baseDir.isDirectory()) {
			return;
		}

		int itemCount = 0;
		long maxLastModified = 0;
		long now = System.currentTimeMillis() + dirWatchInterval;
		
		CRC32 crc = new CRC32();
		for (PartsCategory partsCategory : characterData.getPartsCategories()) {
			for (Layer layer : partsCategory.getLayers()) {
				File watchDir = new File(baseDir, layer.getDir());
				ArrayList<String> files = new ArrayList<String>();
				if (watchDir.exists() && watchDir.isDirectory()) {
					File[] list = watchDir.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							return pathname.isFile() && pathname.getName().toLowerCase().endsWith(".png");
						}
					});
					if (list == null) {
						list = new File[0];
					}
					for (File file : list) {
						if (Thread.interrupted() || stopFlag) {
							throw new InterruptedException();
						}

						itemCount++;
						long lastModified = file.lastModified();
						if (lastModified <= now) {
							// 未来の日付は除外する.
							// 未来の日付のファイルが一つでもあると他のファイルが実際に更新されても判定できなくなるため。
							maxLastModified = Math.max(maxLastModified, lastModified);
						}
						files.add(file.getName() + ":" + lastModified);
					};
					Collections.sort(files);
				}
				for (String file : files) {
					crc.update(file.getBytes());
				}
			}
		}

		long signature = crc.getValue();
		if (this.signature != null) {
			// 初回は無視される.
			if (this.signature.longValue() != signature
					|| this.itemCount != itemCount
					|| this.maxLastModified != maxLastModified) {
				// ハッシュ値が異なるか、アイテム数が異なるか、最大の最終更新日が異なる場合、変更されたとみなす.
				if (notifier != null) {
					notifier.run();
				}
			}
		}
		this.signature = Long.valueOf(signature);
		this.maxLastModified = maxLastModified;
		this.itemCount = itemCount;
		
	}
	
	/**
	 * イベントリスナを登録する
	 * 
	 * @param l
	 *            リスナ
	 */
	public void addPartsImageDirectoryWatchListener(PartsImageDirectoryWatchListener l) {
		if (l != null) {
			synchronized (lockListeners) {
				listeners.add(l);
			}
			changeSuspendState();
		}
	}
	
	/**
	 * イベントリスナを登録解除する
	 * 
	 * @param l
	 *            リスナ
	 */
	public void removePartsImageDirectoryWatchListener(PartsImageDirectoryWatchListener l) {
		if (l != null) {
			synchronized (lockListeners) {
				listeners.remove(l);
				suspendStateMap.remove(l);
			}
			changeSuspendState();
		}
	}
	
	public void suspend(PartsImageDirectoryWatchListener l) {
		if (l != null) {
			synchronized (lockListeners) {
				Integer cnt = suspendStateMap.get(l);
				if (cnt == null) {
					cnt = Integer.valueOf(1);

				} else {
					cnt = cnt + 1;
				}
				suspendStateMap.put(l, cnt);
			}
			changeSuspendState();
		}
	}
	
	public void resume(PartsImageDirectoryWatchListener l) {
		if (l != null) {
			synchronized (lockListeners) {
				Integer cnt = suspendStateMap.get(l);
				if (cnt != null) {
					cnt = cnt - 1;
					if (cnt > 0) {
						suspendStateMap.put(l, cnt);
					} else {
						suspendStateMap.remove(l);
					}
				}
			}
			changeSuspendState();
		}
	}
	
	protected void changeSuspendState() {
		boolean active;
		synchronized (lockListeners) {
			// リスナが接続されており、且つ、サスペンド要求が一つもない場合はスレッド実行可
			active = !listeners.isEmpty() && suspendStateMap.isEmpty();
		}
		if (active) {
			start();
		} else {
			stop();
		}
	}
	
	/**
	 * イベントを通知する.
	 */
	protected void fireWatchEvent() {
		PartsImageDirectoryWatchListener[] listeners;
		synchronized (this.listeners) {
			listeners = this.listeners.toArray(new PartsImageDirectoryWatchListener[this.listeners.size()]);
		}
		PartsImageDirectoryWatchEvent e = new PartsImageDirectoryWatchEvent(characterData);
		for (PartsImageDirectoryWatchListener listener : listeners) {
			listener.detectPartsImageChange(e);
		}
	}

}
