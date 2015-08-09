package charactermanaj.graphics;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 各パーツ情報をもとに非同期にイメージを合成する
 * @author seraphy
 */
public class AsyncImageBuilder extends ImageBuilder implements Runnable {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(AsyncImageBuilder.class.getName());

	/**
	 * 非同期にイメージを構築するためのジョブ定義.<br>
	 * リクエストを受け付けたことを示すイベントおよび、リクエストが放棄されたことを示すイベントを受け取ることができる.<br>
	 * @author seraphy
	 *
	 */
	public interface AsyncImageBuildJob extends ImageBuildJob {
		/**
		 * リクエストを受け付けた場合に呼び出される.<br>
		 * @param ticket このイメージビルダでリクエストを受け付けた通し番号
		 */
		void onQueueing(long ticket);
		
		/**
		 * リクエストを処理するまえに破棄された場合に呼び出される.<br>
		 */
		void onAbandoned();
	}
	
	/**
	 * 同期オブジェクト
	 */
	private final Object lock = new Object();
	
	/**
	 * チケットのシリアルナンバー.<br>
	 * リクエストがあるごとにインクリメントされる.<br>
	 */
	private long ticketSerialNum = 0;
	
	/**
	 * リクエストされているジョブ、なければnull
	 */
	private ImageBuildJob requestJob;

	/**
	 * 停止フラグ(volatile)
	 */
	private volatile boolean stopFlag;
	
	/**
	 * スレッド
	 */
	private Thread thread;
	
	/**
	 * イメージローダを指定して構築する.
	 * @param imageLoader イメージローダー
	 */
	public AsyncImageBuilder(ColorConvertedImageCachedLoader imageLoader) {
		super(imageLoader);
		thread = new Thread(this);
		thread.setDaemon(true);
	}
	
	/**
	 * スレッドの実行部.
	 */
	public void run() {
		logger.log(Level.FINE, "AsyncImageBuilder thread started.");

		// 停止フラグがたてられるまで繰り返す.
		while (!stopFlag) {
			try {
				ImageBuildJob job;
				synchronized (lock) {
					while (!stopFlag && requestJob == null) {
						// ジョブリクエストがくるまで待機
						lock.wait(1000);
					}
					if (stopFlag) {
						break;
					}
					// ジョブを一旦ローカル変数に保存
					job = requestJob;
					// ジョブの受け付けを再開.
					requestJob = null;
					lock.notifyAll();
				}
				// リクエストを処理する.
				AsyncImageBuilder.super.requestJob(job);
				
			} catch (InterruptedException ex) {
				logger.log(Level.FINE, "AsyncImageBuilder thead interrupted.");
				// 割り込みされた場合、単にループを再開する.
				
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "AsyncImageBuilder failed.", ex);
				// ジョブ合成中の予期せぬ例外はログに記録するのみで
				// スレッドそのものは停止させない.
				// (Error系は、たぶんアプリ自身が続行不能な障害なので停止する.)
			}
		}
		logger.log(Level.FINE, "AsyncImageBuilder thread stopped.");
	}
	
	/**
	 * イメージ作成ジョブをリクエストする.<br>
	 * イメージ作成ジョブは非同期に実行される.<br>
	 * 処理がはじまる前に新しいリクエストで上書きされた場合、前のリクエストは単に捨てられる.<br>
	 */
	@Override
	public boolean requestJob(ImageBuildJob imageSource) {
		synchronized (lock) {
			// 現在処理待ちのリクエストがあれば、新しいリクエストで上書きする.
			if (this.requestJob != null && this.requestJob instanceof AsyncImageBuildJob) {
				((AsyncImageBuildJob) this.requestJob).onAbandoned();
			}
			
			// リクエストをセットして待機中のスレッドに通知を出す.
			this.requestJob = imageSource;
			if (imageSource != null && imageSource instanceof AsyncImageBuildJob) {
				((AsyncImageBuildJob) imageSource).onQueueing(++ticketSerialNum);
			}

			lock.notifyAll();
		}
		return false;
	}

	/**
	 * スレッドが生きているか?
	 * @return 生きていればtrue
	 */
	public boolean isAlive() {
		return thread.isAlive();
	}
	
	/**
	 * スレッドを開始する.
	 */
	public void start() {
		if (!thread.isAlive()) {
			stopFlag = false;
			thread.start();
		}
	}

	/**
	 * スレッドを停止する.
	 */
	public void stop() {
		if (thread.isAlive()) {
			stopFlag = true;
			thread.interrupt();
			try {
				// スレッドの停止を待機する.
				thread.join();
			} catch (InterruptedException ex) {
				// do nothing.
			}
		}
	}
	
}
