package charactermanaj.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ユーザーデータの保存先
 * @author seraphy
 */
public interface UserData {

	/**
	 * データを開く
	 * @return 入力ストリーム
	 * @throws IOException 開けなかった場合
	 */
	InputStream openStream() throws IOException;
	
	/**
	 * データを書き込む
	 * @return 出力ストリーム
	 * @throws IOException 開けなかった場合
	 */
	OutputStream getOutputStream() throws IOException;
	
	/**
	 * 更新日時(エポックタイム)、まだ存在しない場合は0
	 * @return 更新日時
	 */
	long lastModified();
	
	/**
	 * 存在するか?
	 * @return 存在すればtrue
	 */
	boolean exists();
	
	/**
	 * 削除する.<br>
	 * すでに存在しない場合は何もしない.<br>
	 * @return 削除された場合はtrue
	 */
	boolean delete();
	
	/**
	 * 引数に指定したオブジェクトをシリアライズします.
	 * @param obj オブジェクト
	 * @throws IOException 保存できなかった場合
	 */
	void save(Object obj) throws IOException;
	
	/**
	 * オブジェクトをデシリアライズして返します.
	 * @return オブジェクト
	 * @throws IOException 取得できなかった場合
	 */
	Object load() throws IOException;
	
}
