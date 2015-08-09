package charactermanaj.model;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.graphics.filters.ColorConvertParameter;

/**
 * 素のカラー情報.<br>
 * レイヤーやカテゴリなどのリレーションシップがない、<br>
 * 特定のキャラクターデータモデルのツリーの一部には組み込まれていない状態のもの.<br>
 */
public class IndependentPartsColorInfo implements Serializable {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = -8114086036157411198L;

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(IndependentPartsColorInfo.class.getName());

	/**
	 * layerID
	 */
	private String layerId;

	/**
	 * カラーグループのid
	 */
	private String colorGroupId;

	/**
	 * カラーの同期指定
	 */
	private boolean syncColorGroup;
	
	/**
	 * カラー変換パラメータ.<br>
	 */
	private ColorConvertParameter colorConvertParameter = new ColorConvertParameter();

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getLayerId() {
		return layerId;
	}

	public void setColorGroupId(String colorGroupId) {
		this.colorGroupId = colorGroupId;
	}

	public String getColorGroupId() {
		return colorGroupId;
	}

	public void setSyncColorGroup(boolean syncColorGroup) {
		this.syncColorGroup = syncColorGroup;
	}

	public boolean isSyncColorGroup() {
		return syncColorGroup;
	}

	public void setColorConvertParameter(
			ColorConvertParameter colorConvertParameter) {
		this.colorConvertParameter = colorConvertParameter;
	}

	public ColorConvertParameter getColorConvertParameter() {
		return colorConvertParameter;
	}

	/**
	 * インスタンス独立の素のカラー情報から、カテゴリやレイヤー、カラーグループのインスタンスと関連づけられたカラー情報に変換してかえす.
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @param category
	 *            パーツカテゴリインスタンス
	 * @param partsColorInfoList
	 *            素のパーツカラー情報、なければnull可
	 * @return パーツカラー情報、パーツカラー情報がなければnull
	 */
	public static PartsColorInfo buildPartsColorInfo(
			CharacterData characterData,
			PartsCategory category,
			List<IndependentPartsColorInfo> partsColorInfoList) {
		if (characterData == null || category == null) {
			throw new IllegalArgumentException();
		}
		if (partsColorInfoList == null) {
			return null;
		}
		PartsColorInfo partsColorInfo = null;
		for (IndependentPartsColorInfo info : partsColorInfoList) {
			String layerId = info.getLayerId();
			Layer layer = category.getLayer(layerId);
			if (layer == null) {
				logger.log(Level.WARNING, "undefined layer: " + layerId);
				break;
			}
			if (partsColorInfo == null) {
				partsColorInfo = new PartsColorInfo(category);
			}

			ColorInfo colorInfo = partsColorInfo.get(layer);

			// color group
			String colorGroupId = info.getColorGroupId();
			ColorGroup colorGroup = characterData.getColorGroup(colorGroupId);
			boolean syncColorGroup = info.isSyncColorGroup();
			colorInfo.setColorGroup(colorGroup);
			colorInfo.setSyncColorGroup(syncColorGroup);

			// color parameters
			colorInfo.setColorParameter(info.getColorConvertParameter());
		}
		return partsColorInfo;
	}
}