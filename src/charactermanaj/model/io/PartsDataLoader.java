package charactermanaj.model.io;

import java.util.Map;

import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;

/**
 * パーツデータのロードを行うインターフェイス
 * @author seraphy
 */
public interface PartsDataLoader {

	/**
	 * カテゴリを指定してパーツデータをロードする.<br>
	 * 該当がない場合は空が返される
	 * @param category カテゴリ
	 * @return パーツデータのマップ
	 */
	Map<PartsIdentifier, PartsSpec> load(PartsCategory category);
	
}
