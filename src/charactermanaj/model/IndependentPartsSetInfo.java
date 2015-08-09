package charactermanaj.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 素のパーツセットの情報.<br>
 * レイヤーやカテゴリなどのリレーションシップがない、<br>
 * 特定のキャラクターデータモデルのツリーの一部には組み込まれていない状態のもの.<br>
 */
public class IndependentPartsSetInfo implements Serializable {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = 7280485045920860407L;

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(IndependentPartsSetInfo.class.getName());

	/**
	 * バーツセットのID
	 */
	private String id;

	/**
	 * パーツセットの表示名
	 */
	private String displayName;

	/**
	 * 背景色、未設定であればnull
	 */
	private Color backgroundColor;

	/**
	 * アフィン変換パラメータ、未設定であればnull
	 */
	private double[] affineTransformParameter;

	/**
	 * カテゴリIDをキーとし、パーツ名をキーとしカラー情報のリストを値とするマップを値とする.
	 */
	private Map<String, Map<String, List<IndependentPartsColorInfo>>> partsMap = new HashMap<String, Map<String, List<IndependentPartsColorInfo>>>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public double[] getAffineTransformParameter() {
		return affineTransformParameter;
	}

	public void setAffineTransformParameter(
			double[] affineTransformParameter) {
		this.affineTransformParameter = affineTransformParameter;
	}

	/**
	 * カテゴリIDをキーとし、パーツ名をキーとしカラー情報のリストを値とするマップを値とする.
	 * 
	 * @return カテゴリIDをキーとし、パーツ名をキーとしカラー情報のリストを値とするマップを値とする.
	 */
	public Map<String, Map<String, List<IndependentPartsColorInfo>>> getPartsMap() {
		return partsMap;
	}

	public void setPartsMap(
			Map<String, Map<String, List<IndependentPartsColorInfo>>> partsMap) {
		if (partsMap == null) {
			throw new IllegalArgumentException();
		}
		this.partsMap = partsMap;
	}

	/**
	 * インスタンス独立のパーツセット情報から、指定されたキャラクターデータに関連づけられた パーツ情報に変換して返す.<br>
	 * 
	 * @param partsSetInfo
	 *            インスタンス独立のパーツセット情報
	 * @param characterData
	 *            キャラクターデータ
	 * @param presetParts
	 *            プリセットか？
	 * @return キャラクターデータに関連づけられたパーツセットインスタンス
	 */
	public static PartsSet convertPartsSet(
			IndependentPartsSetInfo partsSetInfo, CharacterData characterData,
			boolean presetParts) {
		if (partsSetInfo == null || characterData == null) {
			throw new IllegalArgumentException();
		}
		PartsSet partsSet = new PartsSet();
		partsSet.setPartsSetId(partsSetInfo.getId());
		partsSet.setLocalizedName(partsSetInfo.getDisplayName());
		partsSet.setPresetParts(presetParts);

		Color backgroundColor = partsSetInfo.getBackgroundColor();
		if (backgroundColor != null) {
			partsSet.setBgColor(backgroundColor);
		}

		double[] affineTrans = partsSetInfo.getAffineTransformParameter();
		if (affineTrans != null) {
			partsSet.setAffineTransformParameter(affineTrans);
		}

		Map<String, Map<String, List<IndependentPartsColorInfo>>> partsMap = partsSetInfo
				.getPartsMap();
		for (Map.Entry<String, Map<String, List<IndependentPartsColorInfo>>> categoryEntry : partsMap
				.entrySet()) {
			String categoryId = categoryEntry.getKey();
			Map<String, List<IndependentPartsColorInfo>> categoryPartsMap = categoryEntry
					.getValue();

			PartsCategory partsCategory = characterData
					.getPartsCategory(categoryId);
			if (partsCategory == null) {
				logger.log(Level.WARNING, "undefined category-id: "
						+ categoryId);
				continue;
			}

			for (Map.Entry<String, List<IndependentPartsColorInfo>> partsEntry : categoryPartsMap
					.entrySet()) {
				String partsName = partsEntry.getKey();
				List<IndependentPartsColorInfo> colorInfoList = partsEntry
						.getValue();

				PartsIdentifier partsIdentifier = new PartsIdentifier(
						partsCategory, partsName, partsName);

				PartsColorInfo partsColorInfo = IndependentPartsColorInfo
						.buildPartsColorInfo(characterData, partsCategory,
								colorInfoList);

				partsSet.appendParts(partsCategory, partsIdentifier,
						partsColorInfo);
			}
		}
		return partsSet;
	}
}
