package charactermanaj.ui.progress;

/**
 * プログレスダイアログのワーカースレッド実行中に例外が発生した場合
 * @author seraphy
 */
public class WorkerException extends Exception {

	private static final long serialVersionUID = -8315947965963588713L;

	public WorkerException() {
		super();
	}
	
	public WorkerException(String message) {
		super(message);
	}
	
	public WorkerException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
