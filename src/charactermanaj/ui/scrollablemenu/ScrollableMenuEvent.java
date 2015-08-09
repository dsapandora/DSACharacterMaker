package charactermanaj.ui.scrollablemenu;

import java.util.EventObject;

/**
 * スクローラブルメニューのイベント
 * 
 * @author seraphy
 */
public class ScrollableMenuEvent extends EventObject {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = 5686533260565824649L;

	/**
	 * スクロール中フラグ
	 */
	private boolean _scrolling;

	/**
	 * イベントのコンストラクタ
	 * 
	 * @param s
	 *            イベントソース
	 * @param scrolling
	 *            スクロール中フラグ
	 */
	public ScrollableMenuEvent(JScrollerMenuItem s, boolean scrolling) {
		super(s);
		this._scrolling = scrolling;
	}

	/**
	 * スクロール中か?
	 * 
	 * @return スクロール中であればtrue
	 */
	public boolean isScrolling() {
		return _scrolling;
	}

	/**
	 * 診断用
	 * 
	 * @return 診断用文字列
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName());
		buf.append("[");
		buf.append(this.source);
		buf.append(",scrolling=").append(this._scrolling);
		buf.append("]");
		return buf.toString();
	}
}
