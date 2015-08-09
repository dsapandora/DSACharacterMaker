package charactermanaj.ui.progress;

/**
 * ワーカースレッドからプログレスダイアログに状態を通知するためのインターフェイス.<br> 
 * 中途半端な状態で反映されないように、複数のプロパティを設定する場合は
 * 同期をとること.<br>
 * @author seraphy
 */
public interface ProgressHandle {
	
	/**
	 * 進行状態の最大値を設定する.
	 * @param maximum
	 */
	void setProgressMaximum(int maximum);
	
	/**
	 * 進行状態の現在値を設定する.
	 * @param current
	 */
	void setProgressCurrent(int current);
	
	/**
	 * 進行状態が不明であることを設定する.
	 * @param indeterminate
	 */
	void setIndeterminate(boolean indeterminate);
	
	/**
	 * キャプションを設定する.
	 * @param caption
	 */
	void setCaption(String caption);
	
}
