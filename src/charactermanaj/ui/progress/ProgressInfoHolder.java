package charactermanaj.ui.progress;

/**
 * プログレスダイアログと、そのワーカースレッドの間で進行状態を通信するためのホルダ.<br>
 * @author seraphy
 */
public class ProgressInfoHolder implements ProgressHandle {

	/**
	 * キャプション
	 */
	private String caption;
	
	/**
	 * 進行状態不明フラグ.
	 */
	private Boolean indeterminate;
	
	/**
	 * 進行状態の現在値
	 */
	private Integer progressCurrent;
	
	/**
	 * 進行状態の最大値
	 */
	private Integer progressMaximum;

	
	public synchronized String getCaption() {
		return caption;
	}

	public synchronized void setCaption(String caption) {
		this.caption = caption;
	}

	public synchronized Boolean getIndeterminate() {
		return indeterminate;
	}

	public synchronized void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
	}

	public synchronized Integer getProgressCurrent() {
		return progressCurrent;
	}

	public synchronized void setProgressCurrent(int progressCurrent) {
		this.progressCurrent = progressCurrent;
	}

	public synchronized Integer getProgressMaximum() {
		return progressMaximum;
	}

	public synchronized void setProgressMaximum(int progressMaximum) {
		this.progressMaximum = progressMaximum;
	}
	
	/**
	 * 現在の状態で確定し、ただちに状態をリセットする.<br>
	 */
	public synchronized void flush() {
		caption = null;
		indeterminate = null;
		progressCurrent = null;
		progressMaximum = null;
	}
}
