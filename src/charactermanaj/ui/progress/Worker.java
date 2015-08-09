package charactermanaj.ui.progress;

/**
 * ワーカー.<br>
 * @author seraphy
 *
 * @param <T> ワーカーの戻り型
 */
public interface Worker<T> {
	
	/**
	 * ワーカーを実行する.<br>
	 * 
	 * @param progressHandle 進行状態を通知するハンドル
	 * @return 処理結果
	 * @throws Exception 何らかの失敗
	 */
	T doWork(ProgressHandle progressHandle) throws Exception;
	
}
