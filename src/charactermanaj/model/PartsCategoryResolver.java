package charactermanaj.model;

import java.util.List;

public interface PartsCategoryResolver {

	/**
	 * パーツカテゴリの一覧を取得する.<br>
	 * @return パーツカテゴリの一覧。(表示順)
	 */
	List<PartsCategory> getPartsCategories();
	
	/**
	 * パーツカテゴリのIDを指定して該当のインスタンスを取得します.<br>
	 * @param id パーツカテゴリID
	 * @return インスタンス、なければnull
	 */
	PartsCategory getPartsCategory(String id);
	
}
