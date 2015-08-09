package charactermanaj.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import charactermanaj.model.AppConfig;


/**
 * このアプリケーションの活動を記録するログハンドラ.<br>
 * アプリケーション用のディレクトリのlogsフォルダ下に開始日時のファイル名をもつログファイルを作成し、ログを記録する.<br>
 * ただし、終了時、警告以上のログが一度も書き込まれなかった場合はログファィルは自動的に削除される.<br>
 *  
 * @author seraphy
 */
public class ApplicationLogHandler extends Handler {

	private static final String LOGS_DIR = "logs";
	
	private final Object lock = new Object();
	
	private final File logFile;
	
	private PrintWriter pw;
	
	private boolean notRemove;
	
	public ApplicationLogHandler() {
		File appDir = ConfigurationDirUtilities.getUserDataDir();
		File logsDir = new File(appDir, LOGS_DIR);
		if (!logsDir.exists()) {
			if (!logsDir.mkdirs()) {
				// ログ記録場所が作成できていないのでコンソールに出すしかない.
				System.err.println("can't create the log directory. " + logsDir);
			}
		}
		
		String fname = getCurrentTimeForFileName() + ".log";
		logFile = new File(logsDir, fname);
		PrintWriter tmp;
		try {
			tmp = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
		} catch (Exception ex) {
			ex.printStackTrace(); // ロガーが失敗しているので、この失敗はコンソールに出すしかない。
			tmp = null;
		}
		this.pw = tmp;
	}
	
	@Override
	public void close() throws SecurityException {
		synchronized (lock) {
			// 終了時にAppConfigにアクセスする. 
			// (アプリケーションの終了時にアクセスすることで初期化タイミングの問題を避ける.)
			try {
				AppConfig appConfig = AppConfig.getInstance();
				if (appConfig.isNoRemoveLog()) {
					notRemove = true;
				}
			} catch (Exception ex) {
				// なんらかのアクセスに失敗した場合でも継続できるようにする.
				// ロガーが閉じられようとしているので、pwに直接出力する
				notRemove = true;
				try {
					if (pw != null) {
						ex.printStackTrace(pw);
					}
				} catch (Exception iex) {
					iex.printStackTrace(); // コンソールに出す他ない
				}
			}

			if (pw != null) {
				pw.close();
				pw = null;
			}
			if (logFile != null && !notRemove) {
				// 警告未満のログしかない場合はログファイルは毎回削除する.
				if (!logFile.delete()) {
					System.err.println("can't delete file. " + logFile);
				}
			}
		}
	}
	
	@Override
	public void flush() {
		synchronized (lock) {
			if (pw != null) {
				pw.flush();
			}
		}
	}
	
	@Override
	public void publish(LogRecord record) {
		if (record == null) {
			return;
		}

		// メッセージの記録
		synchronized (lock) {
			if (pw == null) {
				return;
			}

			Level lv = record.getLevel();
			String name = record.getLoggerName();
			pw.println("#" + getCurrentTime() + " " + name + " "
					+ lv.getLocalizedName() + " " + record.getMessage());
			
			// 例外があれば、例外の記録
			Throwable tw = record.getThrown(); 
			if (tw != null) {
				tw.printStackTrace(pw); // 例外のコールスタックをロガーに出力
			}
			
			// フラッシュする.(随時、ファイルの中身を見ることができるように.)
			pw.flush();

			// 警告以上であれば終了時にファイルを消さない
			if (lv.intValue() >= Level.WARNING.intValue()) {
				notRemove = true;
			}
		}
	}
	
	public String getCurrentTime() {
		Timestamp tm = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return dt.format(tm);
	}

	public String getCurrentTimeForFileName() {
		Timestamp tm = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS");
		return dt.format(tm);
	}
}
