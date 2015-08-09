package charactermanaj.ui.scrollablemenu;

import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.event.EventListenerList;

/**
 * スクローラブルメニューのスクローラーアイテムのメニュー項目
 * 
 * @author seraphy
 */
public class JScrollerMenuItem extends JMenuItem {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = -1749741596476938310L;
	/**
	 * イベントリスナのコレクション
	 */
	protected EventListenerList _listeners = new EventListenerList();

	/**
	 * スクローラーのアイコンを指定してスクローラーアイテムのメニュー項目を構築します.
	 * 
	 * @param icon
	 *            アイコン
	 */
	public JScrollerMenuItem(Icon icon) {
		setIcon(icon);
	}

	/**
	 * スクローラブルメニューイベントのイベントリスナを登録します.
	 * 
	 * @param l
	 *            リスナー
	 */
	public void addScrollableMenuEventListener(ScrollableMenuEventListener l) {
		_listeners.add(ScrollableMenuEventListener.class, l);
	}

	/**
	 * スクローラブルメニューイベントのイベントリスナを登録解除します.
	 * 
	 * @param l
	 *            リスナー
	 */
	public void removeScrollableMenuEventListener(ScrollableMenuEventListener l) {
		_listeners.remove(ScrollableMenuEventListener.class, l);
	}

	/**
	 * マウスクリックでメニューアイテムとしてのイベントが発生しないように、 マウスイベントをキャプチャして、スクローラブルメニューイベントに変換する。
	 * 
	 * @param e
	 */
	@Override
	protected void processMouseEvent(MouseEvent e) {
		ScrollableMenuEvent ee = null;
		int mouseEventId = e.getID();
		if (mouseEventId == MouseEvent.MOUSE_PRESSED) {
			// マウスダウン時、スクロール開始
			ee = new ScrollableMenuEvent(this, true);
		}
		if (mouseEventId == MouseEvent.MOUSE_RELEASED) {
			// マウスアップされた場合、スクロール停止
			ee = new ScrollableMenuEvent(this, false);
		}
		if (ee != null) {
			fireScrollableMenuEvent(ee);
		}
	}

	/**
	 * スクローラブルメニューイベントを送信する
	 * 
	 * @param e
	 *            メニューイベント
	 */
	protected void fireScrollableMenuEvent(ScrollableMenuEvent e) {
		for (ScrollableMenuEventListener l : _listeners
				.getListeners(ScrollableMenuEventListener.class)) {
			if (e.isScrolling()) {
				l.start(e);
			} else {
				l.end(e);
			}
		}
	}
}
