package charactermanaj.model.io;

import charactermanaj.model.CharacterData;

/**
 * パーツファイルのディレクトリの監視を行うエージェント.<br>
 * @author seraphy
 */
public interface PartsImageDirectoryWatchAgent {

	/**
	 * 監視対象としているキャラクター定義を取得する.
	 * @return キャラクター定義
	 */
	CharacterData getCharcterData();
	
	/**
	 * 監視を接続する.<br>
	 * 接続されるまでディレクトリの監視状態は通知されない.<br>
	 */
	void connect();
	
	/**
	 * 監視を切断する.<br>
	 * 接続されるまでディレクトリの監視状態は通知されない.<br>
	 */
	void disconnect();

	/**
	 * 監視対象ディレクトリの監視を停止する.<br>
	 */
	void suspend();
	
	/**
	 * 監視対象ディレクトリの監視再開を許可する.<br>
	 */
	void resume();
	
	/**
	 * イベントリスナを登録する
	 * @param l リスナ
	 */
	void addPartsImageDirectoryWatchListener(PartsImageDirectoryWatchListener l);
	
	/**
	 * イベントリスナを登録解除する
	 * @param l リスナ
	 */
	void removePartsImageDirectoryWatchListener(PartsImageDirectoryWatchListener l);
	
}
