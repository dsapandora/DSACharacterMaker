package charactermanaj.ui.progress;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;


/**
 * ワーカースレッドの実行中、プログレスを表示するモーダルダイアログ.<br>
 * ワーカースレッドの実行が完了するとダイアログは自動的に閉じられる.<br>
 * モーダルダイアログであるため、ワーカースレッドの実行中はユーザはUIを操作することはできない.<br>
 * @author seraphy
 *
 * @param <T> ワーカーの処理結果の戻り値の型
 */
public class WorkerWithProgessDialog<T> extends JDialog {

	private static final long serialVersionUID = 1L;

	/**
	 * ワーカースレッドが停止したことを示すフラグ
	 */
	private volatile boolean exitThread;
	
	/**
	 * ワーカースレッドの戻り値
	 */
	private volatile T result;
	
	/**
	 * ワーカースレッドが例外により終了した場合の例外
	 */
	private volatile Throwable occuredException;
	
	/**
	 * ワーカースレッド
	 */
	private Thread thread;
	
	/**
	 * ワーカースレッドの状態を監視しプログレスに反映させるタイマー
	 */
	private Timer timer;
	
	/**
	 * プログレスの更新頻度(タイマーのインターバル)
	 */
	private static int interval = 200;
	

	/**
	 * 親フレームとワーカーを指定して構築する.<br>
	 * @param parent 親フレーム
	 * @param worker ワーカー
	 */
	public WorkerWithProgessDialog(JFrame parent, Worker<T> worker) {
		super(parent, true);
		try {
			if (worker == null) {
				throw new IllegalArgumentException();
			}
			
			initComponent(parent, worker);
			
		} catch (RuntimeException ex) {
			dispose();
			throw ex;
		}
	}
	
	/**
	 * 親ダイアログとワーカーを指定して構築する.<br>
	 * @param parent 親フレーム
	 * @param worker ワーカー
	 */
	public WorkerWithProgessDialog(JDialog parent, Worker<T> worker) {
		super(parent, true);
		try {
			if (worker == null) {
				throw new IllegalArgumentException();
			}
			
			initComponent(parent, worker);
			
		} catch (RuntimeException ex) {
			dispose();
			throw ex;
		}
	}

	/**
	 * コンポーネントの初期化
	 * @param parent 親フレームまたは親ダイアログ
	 * @param worker ワーカー
	 */
	private void initComponent(Component parent, Worker<T> worker) {
		// 閉じるボタンは無効
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		// リサイズ不可
		setResizable(false);
		// ウィンドウ装飾なし (閉じるボタンやタイトルバーなども無し)
		setUndecorated(true);

		Container container = getContentPane();

		// プログレスバー
		final JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(false);
		
		container.add(progressBar, BorderLayout.SOUTH);
		
		// デフォルトのラベル表示 
		String title = "please wait for a while.";
		final JLabel lblCaption = new JLabel(title);
		container.add(lblCaption, BorderLayout.NORTH);

		// ウィンドウ枠
		JRootPane rootPane = getRootPane();
		rootPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createBevelBorder(BevelBorder.RAISED),
				BorderFactory.createEmptyBorder(5, 5, 5, 5))
				);

		// 親ウィンドウの幅の70%
		Dimension dim = progressBar.getPreferredSize();
		dim.width = (int)(parent.getWidth() * 0.7);
		progressBar.setPreferredSize(dim);

		// パックする.
		pack();
		
		// 親の中央に表示
		setLocationRelativeTo(parent);

		// ワーカースレッドとプログレスダイアログとの状態通信用オブジェクト
		final ProgressInfoHolder progressHandle = new ProgressInfoHolder() {
			@Override
			public synchronized void flush() {
				final String caption = getCaption();
				final Boolean indeterminate = getIndeterminate();
				final Integer progressMaximum = getProgressMaximum();
				final Integer progressCurrent = getProgressCurrent();

				if (caption != null || progressMaximum != null ||
						progressCurrent != null || indeterminate != null) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// 設定されている値でプログレスダイアログに状態を反映する.
							if (caption != null) {
								lblCaption.setText(caption);
							}
							if (progressMaximum != null) {
								progressBar.setMaximum(progressMaximum.intValue());
							}
							if (progressCurrent != null) {
								progressBar.setValue(progressCurrent.intValue());
							}
							if (indeterminate != null) {
								progressBar.setIndeterminate(indeterminate.booleanValue());
								progressBar.setStringPainted( !indeterminate.booleanValue());
							}
						}
					});
				}
				
				super.flush();
			}
		};
		
		// プログレスダイアログに状態を反映させるためのタイマー
		timer = new Timer(interval, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (exitThread || !thread.isAlive()) {
					// スレッドが終了していればスレッド停止を通知する.
					onExitWork();

				} else {
					// スレッドが生きていれば、スレッドの進行状態を
					// プログレスダイアログに反映させる.
					progressHandle.flush();
				}
			}
		});

		// ワーカースレッドの構築.
		thread = new Thread(createJob(worker, progressHandle));
		thread.setDaemon(true);

		// ワーカースレッドが予期せぬハンドルされていない例外により終了した場合のハンドラ.
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				occuredException = e;
				onExitWork();
			}
		});
	}
	
	/**
	 * プログレスの表示間隔を取得する
	 * @return 表示間隔
	 */
	public static int getInterval() {
		return interval;
	}

	/**
	 * プログレスの表示間隔を設定する
	 * @param interval 表示間隔
	 */
	public static void setInterval(int interval) {
		WorkerWithProgessDialog.interval = interval;
	}


	/**
	 * ワーカーをラップするワーカースレッドのジョブを作成する.<br>
	 * @param worker ワーカー
	 * @param progressHandle 進行状態の通知ハンドル
	 * @return ジョブ
	 */
	protected Runnable createJob(final Worker<T> worker, final ProgressHandle progressHandle) {
		return new Runnable() {
			public void run() {
				try {
					try {
						worker.doWork(progressHandle);

					} catch (Throwable ex) {
						occuredException = ex;
					}

				} finally {
					onExitWork();
				}
			}
		};
	}
	
	/**
	 * ワーカースレッドより、スレッドが終了したことを通知される.<br>
	 * ワーカースレッド自身か、ワーカースレッドの例外ハンドラか、
	 * タイマーから呼び出されるため、2回以上呼び出される可能性がある.<br>
	 */
	protected void onExitWork() {
		exitThread = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// プログレスダイアログが表示されている場合、
				// それを破棄する.(モーダルの解除)
				if (isDisplayable() && isVisible()) {
					dispose();
				}
			}
		});
	}
	
	/**
	 * ワーカースレッドを開始し、プログレスダイアログを表示し、
	 * ワーカースレッドの完了まで待機する.<br>
	 * @throws WorkerException ワーカースレッドが例外により終了した場合
	 */
	public void startAndWait() throws WorkerException {
		// 初期化
		result = null;
		occuredException = null;
		exitThread = false;
		
		// ワーカースレッドの開始
		thread.start();
		try {
			timer.start();
			try {
				// モーダルダイアログの開始
				// (モーダルダイアログが非表示されるまで制御を返さない.)
				setVisible(true);

			} finally {
				timer.stop();
			}

		} finally {
			for (;;) {
				try {
					// ワーカースレッドの停止を待機する.
					thread.join();
					break;

				} catch (InterruptedException ex) {
					// 割り込みされた場合は、ワーカースレッドを割り込みする.
					thread.interrupt();
				}
			}
		}

		// ワーカースレッドが例外により終了した場合
		// その例外を送出する.
		if (occuredException != null) {
			throw new WorkerException(
					"worker has failed." + occuredException.getMessage(),
					occuredException
					);
		}
	}

	/**
	 * ワーカースレッドの戻り値を取得する.<br>
	 * 正常終了していない場合、または処理中の場合は意味を持たない.<br>
	 * @return ワーカースレッドの戻り値
	 */
	public T getResult() {
		return result;
	}
}
