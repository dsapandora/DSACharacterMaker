package charactermanaj.model;

import java.awt.Dimension;
import java.util.Map;

/**
 * パーツ設定を取得するためのインターフェイス
 * @author seraphy
 */
public interface PartsSpecResolver extends PartsCategoryResolver {

	/**
	 * イメージのサイズを取得する
	 * @return イメージサイズ、設定がなければnull
	 */
	Dimension getImageSize();
	
	/**
	 * 指定したパーツ識別子に対するパーツ設定を取得する.<br>
	 * なければnull
	 * @param partsIdentifier パーツ識別子
	 * @return パーツ設定、なければnull
	 */
	PartsSpec getPartsSpec(PartsIdentifier partsIdentifier);
	
	/**
	 * 指定したカテゴリに該当するパーツ識別子とパーツ設定のマップを取得する.<br>
	 * 該当するカテゴリがなければ空のマップを返す.<br>
	 * @param category パーツカテゴリ
	 * @return パーツ設定のマップ、なければ空
	 */
	Map<PartsIdentifier, PartsSpec> getPartsSpecMap(PartsCategory category);
	
}
