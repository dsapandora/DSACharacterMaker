package charactermanaj.ui.scrollablemenu;

import java.util.EventListener;

/**
 * スクローラブルメニューのイベントリスナ
 * 
 * @author seraphy
 */
public interface ScrollableMenuEventListener extends EventListener {

	/**
	 * スクロール開始を通知する.
	 * 
	 * @param e
	 *            イベント
	 */
	void start(ScrollableMenuEvent e);

	/**
	 * スクロール終了を通知する.
	 * 
	 * @param e
	 *            イベント
	 */
	void end(ScrollableMenuEvent e);
}
