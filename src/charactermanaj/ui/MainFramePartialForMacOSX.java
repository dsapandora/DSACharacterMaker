package charactermanaj.ui;

import java.awt.Image;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;


import charactermanaj.util.SystemUtil;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * Mac OS X用のメインフレームサポートクラス.<br>
 * スクリーンメニューのハンドラなどを接続している.<br>
 * @author seraphy
 *
 */
public class MainFramePartialForMacOSX {
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(MainFramePartialForMacOSX.class.getName());

	private MainFramePartialForMacOSX() {
		super();
	}

	public static void setupScreenMenu(final MainFrame mainFrame) {
		if (mainFrame == null) {
			throw new IllegalArgumentException();
		}
		
		Application app = Application.getApplication();
		
		app.setEnabledAboutMenu(true);
		app.setEnabledPreferencesMenu(true);

		ApplicationAdapter listener = new ApplicationAdapter() {
			public void handleAbout(ApplicationEvent arg0) {
				if (MainFrame.getActivedMainFrame() != null) {
					MainFrame.getActivedMainFrame().onAbout();
				}
				arg0.setHandled(true);
			}
			public void handleQuit(ApplicationEvent arg0) {
				if (MainFrame.getActivedMainFrame() != null) {
					MainFrame.closeAllProfiles();
				}
				arg0.setHandled(true);
				// JVMを明示的にシャットダウンする. (何もしないと強制終了になるため。)
				SystemUtil.exit(0);
			}
			public void handlePreferences(ApplicationEvent arg0) {
				if (MainFrame.getActivedMainFrame() != null) {
					MainFrame.getActivedMainFrame().onPreferences();
				}
				arg0.setHandled(true);
			}
		};
		app.addApplicationListener(listener);
		
		try {
			Class<?> clz = app.getClass();
			Method mtd = clz.getMethod("setDockIconImage", new Class[] {Image.class});
			mtd.invoke(app, new Object[] {mainFrame.icon});

		} catch (NoSuchMethodException ex) {
			// メソッドがない = Tiger以前の失敗であろうから、単に無視するだけで良い.
			logger.log(Level.CONFIG, "dockIcon not supported.", ex);

		} catch (Exception ex) {
			// 実行時の失敗だが、DockIconが設定できないだけなので継続する.
			logger.log(Level.WARNING, "dockIcon failed.", ex);
		}
	}
	
}
