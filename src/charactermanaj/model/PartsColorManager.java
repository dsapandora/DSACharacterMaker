package charactermanaj.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.graphics.filters.ColorConvertParameter;

/**
 * パーツごとの色情報を設定・取得したり、最後に設定した色情報を記憶するなどの色情報アクセスのためのクラス.<br>
 * @author seraphy
 *
 */
public class PartsColorManager {
	
	private static final Logger logger = Logger.getLogger(PartsColorManager.class.getName());

	/**
	 * カテゴリごとのパーツカラー情報.<br>
	 * @author seraphy
	 */
	public static final class CategoryColorInfo {
		
		private final PartsColorInfo partsColorInfo;
		
		private final boolean applyAll;
		
		public CategoryColorInfo(PartsColorInfo partsColorInfo, boolean applyAll) {
			this.partsColorInfo = partsColorInfo;
			this.applyAll = applyAll;
		}
		
		public PartsColorInfo getPartsColorInfo() {
			return partsColorInfo;
		}
		
		public boolean isApplyAll() {
			return applyAll;
		}
	}
	
	/**
	 * パーツ単位でのカラーグループを含む色情報.<br>
	 * カテゴリ全体に適用される場合はパーツ単位の色情報はリセットする.<br>
	 */
	private HashMap<PartsIdentifier, PartsColorInfo> partsColorInfoMap
		= new HashMap<PartsIdentifier, PartsColorInfo>();

	/**
	 * カテゴリごとに共通となる場合のカラーグループを含む色情報.<br>
	 * パーツ単位の色情報が定義されていない場合、カテゴリ単位での情報が使用される.<br>
	 */
	private HashMap<PartsCategory, CategoryColorInfo> categoryColorInfoMap
		= new HashMap<PartsCategory, CategoryColorInfo>();
	
	/**
	 * カラーグループごとの色情報.<br>
	 */
	private HashMap<ColorGroup, ColorConvertParameter> recentColorGroupMap
		= new HashMap<ColorGroup, ColorConvertParameter>();
	
	/**
	 * パーツ設定のリゾルバ
	 */
	private PartsSpecResolver partsSpecResolver;
	
	/**
	 * パーツ設定リゾルバを指定して構築する.
	 * @param partsSpecResolver リゾルバ
	 */
	public PartsColorManager(PartsSpecResolver partsSpecResolver) {
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException();
		}
		this.partsSpecResolver = partsSpecResolver;
	}
	
	/**
	 * パーツ識別子ごとの色情報を取得します.<br>
	 * まだ一度も登録されていない場合は、現在の状態から色情報を作成して返します.<br>
	 * その場合、registered引数がtrueである場合は生成と同時に初期値として登録済みとする.<br>
	 * そうでない場合は生成された色情報は一時的なものとなる.
	 * @param partsIdentifier パーツ識別子
	 * @param registered 色情報を新規に作成した場合に登録する場合はtrue
	 * @return 色情報
	 */
	public PartsColorInfo getPartsColorInfo(PartsIdentifier partsIdentifier, boolean registered) {
		if (partsIdentifier == null) {
			throw new IllegalArgumentException();
		}
		// パーツ識別子ごとのカラー情報を取得する.
		PartsColorInfo partsColorInfo = partsColorInfoMap.get(partsIdentifier);
		if (partsColorInfo == null) {
			// パーツ識別子ごとのカラー情報が設定されていない場合は
			// カテゴリ別情報からカラー情報を生成する.
			partsColorInfo = createDefaultColorInfo(partsIdentifier);
			if (registered) {
				// 生成されたカラー情報をパーツ識別子ごとのカラー情報に適用する.
				partsColorInfoMap.put(partsIdentifier, partsColorInfo);
			}
		}
		return partsColorInfo;
	}
	
	/**
	 * パーツ識別子ごとのパーツ色情報を保存します.<br>
	 * @param partsIdentifier パーツ識別子
	 * @param partsColorInfo パーツの色情報
	 * @param applyAll パーツ識別子ではなく、カテゴリに対して保存する場合
	 */
	public void setPartsColorInfo(PartsIdentifier partsIdentifier, PartsColorInfo partsColorInfo, boolean applyAll) {
		if (partsIdentifier == null || partsColorInfo == null) {
			throw new IllegalArgumentException();
		}

		partsColorInfo = partsColorInfo.clone();
		PartsCategory partsCategory = partsIdentifier.getPartsCategory();
		
		if (applyAll) {
			// カテゴリ指定の場合
			// パーツ個別色をリセットすることでカテゴリを優先させる.
			resetPartsColorInfo(partsCategory);

			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "setPartsColorInfo(Category): " + partsIdentifier + "=" + partsColorInfo);
			}

		} else {
			// パーツ個別指定の場合
			partsColorInfoMap.put(partsIdentifier, partsColorInfo);

			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "setPartsColorInfo(Parts): " + partsIdentifier + "=" + partsColorInfo);
			}
		}

		// カラーグループとしての最新のカラー情報を保存する.(有効なカラーグループで連動指定がある場合のみ)
		// ただし、「すべてに適用」でない場合は保存しない.
		if (applyAll) {
			setRecentColorGroup(partsColorInfo);
		}

		// カテゴリごとの最新の色情報を設定する.
		// (「すべてに適用」であるか、単数選択カテゴリで、まだ「すべてに適用」の色情報がない場合のみ.)
		// (複数選択カテゴリの場合は明示的に「すべてに適用」を選択していないかぎり保存されない.)
		CategoryColorInfo categoryColorInfo = categoryColorInfoMap.get(partsCategory);
		if (applyAll ||
				(!partsCategory.isMultipleSelectable() &&
				(categoryColorInfo == null || !categoryColorInfo.isApplyAll()))) {
			categoryColorInfo = new CategoryColorInfo(partsColorInfo, applyAll);
			categoryColorInfoMap.put(partsCategory, categoryColorInfo);
		}
	}

	/**
	 * パーツの色情報を指定して、パーツ識別子の各レイーヤの色グループ情報を保存します.<br>
	 * 連動指定が有効であり、有効なカラーグループである場合のみ保存されます.<br>
	 * @param partsColorInfo パーツ識別子
	 */
	protected void setRecentColorGroup(PartsColorInfo partsColorInfo) {
		if (partsColorInfo == null) {
			return;
		}
		for (Map.Entry<Layer, ColorInfo> entry : partsColorInfo.entrySet()) {
			ColorInfo colorInfo = entry.getValue();
			ColorGroup colorGroup = colorInfo.getColorGroup();
			if (colorInfo.isSyncColorGroup() && colorGroup != null && colorGroup.isEnabled()) {
				ColorConvertParameter colorParam = colorInfo.getColorParameter();
				if (colorParam != null) {
					colorParam = colorParam.clone();
				}
				
				ColorConvertParameter oldColorParam = recentColorGroupMap.put(colorGroup, colorParam);

				if (logger.isLoggable(Level.FINEST)) {
					if ( !ColorConvertParameter.equals(colorParam, oldColorParam)) {
						logger.log(Level.FINEST, "setRecentColorGroup(" + colorGroup + ")=" + colorParam);
					}
				}
			}
		}
	}

	/**
	 * カラーグループごとのもっとも最近に設定した色情報を取得します.<br>
	 * カラーグループが有効でないか、まだ一度も登録されていない場合はnullが返されます.<br>
	 * @param colorGroup 色グループ、null可
	 * @return 色情報、もしくはnull
	 */
	protected ColorConvertParameter getRecentColorGroup(ColorGroup colorGroup) {
		if (colorGroup == null || !colorGroup.isEnabled()) {
			return null;
		}
		ColorConvertParameter colorParam = recentColorGroupMap.get(colorGroup);
		if (colorParam != null) {
			colorParam = colorParam.clone();
		}
		return colorParam;
	}

	/**
	 * 指定したパーツカテゴリの色情報を取得します.<br>
	 * 登録がない場合はnullが返されます.<br>
	 * @param partsCategory パーツカテゴリ
	 * @return 指定したパーツカテゴリの色情報、またはnull
	 */
	public CategoryColorInfo getPartsColorInfo(PartsCategory partsCategory) {
		return categoryColorInfoMap.get(partsCategory);
	}

	/**
	 * すべてのパーツ識別子の色情報をリセットします.<br>
	 */
	public void resetPartsColorInfo() {
		resetPartsColorInfo(null);
	}
	
	/**
	 * 指定したカテゴリと、カテゴリに属するパーツ識別子ごとの色情報をリセットします.<br>
	 * 引数partsCategoryがnullの場合は全パーツ識別子、全カテゴリと、すべてのカラーグループをリセットします.<br>
	 * @param partsCategory パーツカテゴリまたはnull
	 */
	public void resetPartsColorInfo(PartsCategory partsCategory) {
		if (partsCategory == null) {
			recentColorGroupMap.clear();
			partsColorInfoMap.clear();
			categoryColorInfoMap.clear();
			return;
		}

		categoryColorInfoMap.remove(partsCategory);
		Iterator<Map.Entry<PartsIdentifier, PartsColorInfo>> ite = partsColorInfoMap.entrySet().iterator();
		while (ite.hasNext()) {
			Map.Entry<PartsIdentifier, PartsColorInfo> entry = ite.next();
			PartsIdentifier partsIdentifier = entry.getKey();
			if (partsIdentifier.getPartsCategory().equals(partsCategory)) {
				ite.remove();
			}
		}
	}
	
	/**
	 * パーツ識別子ごとの色情報を現在の状態から新たに構築する.
	 * @param partsIdentifier パーツ識別子
	 * @return 色情報
	 */
	protected PartsColorInfo createDefaultColorInfo(PartsIdentifier partsIdentifier) {
		PartsCategory category = partsIdentifier.getPartsCategory();
		PartsColorInfo partsColorInfo = new PartsColorInfo(category);

		// パーツ固有のカラーグループの指定があるか?
		PartsSpec partsSpec = partsSpecResolver.getPartsSpec(partsIdentifier);
		ColorGroup partsSpecColorGroup = null;
		if (partsSpec != null) {
			partsSpecColorGroup = partsSpec.getColorGroup();
		}

		if (partsSpecColorGroup != null && partsSpecColorGroup.isEnabled()) {
			// パーツ固定のカラーグループの指定があれば
			// 全レイヤーを該当カラーグループに設定する.
			for (Map.Entry<Layer, ColorInfo> entry : partsColorInfo.entrySet()) {
				ColorInfo colorInfo = entry.getValue();
				colorInfo = colorInfo.clone();
				colorInfo.setColorGroup(partsSpecColorGroup);
				colorInfo.setSyncColorGroup(true);
				entry.setValue(colorInfo);
			}

		} else {
			// パーツ固有のカラーグループがなければ
			// 同一カテゴリの最近設定されたカラー情報をもとに、パーツカラー情報を作成する.
			CategoryColorInfo categoryColorInfo = categoryColorInfoMap.get(category);
			if (categoryColorInfo != null) {
				PartsColorInfo categoryPartsColorInfo = categoryColorInfo.getPartsColorInfo();
				for (Map.Entry<Layer, ColorInfo> entry : categoryPartsColorInfo.entrySet()) {
					Layer layer = entry.getKey();
					ColorInfo colorInfo = entry.getValue();
					if (colorInfo != null && partsColorInfo.containsKey(layer)) {
						colorInfo = colorInfo.clone();
						
						// ただし、同一カテゴリに設定されたカラー情報が「すべてに適用」でない場合は、
						// レイヤー固有のカラーグループを維持する.
						if ( !categoryColorInfo.isApplyAll()) {
							ColorGroup layerColorGroup = layer.getColorGroup();
							if (layerColorGroup == null) {
								layerColorGroup = ColorGroup.NA;
							}
							colorInfo.setColorGroup(layerColorGroup);
						}

						partsColorInfo.put(layer, colorInfo);
					}
				}
			}
		}

		// カラーグループが指定されている場合、もっとも最近に設定されたカラーグループの色情報に設定し直す
		for (Map.Entry<Layer, ColorInfo> entry : partsColorInfo.entrySet()) {
			ColorInfo colorInfo = entry.getValue();
			ColorGroup colorGroup = colorInfo.getColorGroup();
			if (colorGroup != null && colorGroup.isEnabled() && colorInfo.isSyncColorGroup()) {
				ColorConvertParameter param = getRecentColorGroup(colorGroup);
				if (param != null) {
					colorInfo.setColorParameter(param);
				}
			}
		}
		return partsColorInfo;
	}
	
	public Map<PartsIdentifier, PartsColorInfo> getPartsColorInfoMap() {
		return partsColorInfoMap;
	}
	
}
