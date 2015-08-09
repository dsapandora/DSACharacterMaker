package charactermanaj.model;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import charactermanaj.ui.model.WallpaperInfo;

/**
 * WorkingSetのXMLの読み込み時に使用する.<br>
 * 特定のキャラクターデータのインスタンスとの関連を持たない状態の設定値を保持している.<br>
 * 
 * @author seraphy
 */
public class WorkingSet2 {

	/**
	 * ドキュメントベース
	 */
	private URI characterDocBase;

	/**
	 * キャラクターデータのID, Revと基本構造のシグネチャ
	 */
	private String characterDataSig;

	/**
	 * キーはカテゴリid, 値は、パーツ名をキーとしレイヤーごとのカラー情報のリストを値とするマップ
	 */
	private Map<String, Map<String, List<IndependentPartsColorInfo>>> partsColorMap = Collections
			.emptyMap();

	/**
	 * 現在の選択中のパーツと色設定からのパーツセット
	 */
	private IndependentPartsSetInfo currentPartsSet;

	/**
	 * 最後に使用したディレクトリ(保存用)
	 */
	private File lastUsedSaveDir;

	/**
	 * 最後に使用したディレクトリ(Export用)
	 */
	private File lastUsedExportDir;

	/**
	 * 最後に使用したお気に入り情報.<br>
	 * (最後に使用したお気に入り情報は、ver0.92からサポート)<br>
	 */
	private IndependentPartsSetInfo lastUsePresetParts;

	/**
	 * 壁紙情報.<br>
	 * (壁紙はver0.97からサポート)<br>
	 */
	private WallpaperInfo wallpaperInfo;

	public void setCharacterDocBase(URI characterDocBase) {
		this.characterDocBase = characterDocBase;
	}

	public URI getCharacterDocBase() {
		return characterDocBase;
	}

	public String getCharacterDataSig() {
		return characterDataSig;
	}

	public void setCharacterDataSig(String characterDataSig) {
		this.characterDataSig = characterDataSig;
	}

	/**
	 * パーツカラーマップ
	 * 
	 * @return キーはカテゴリid, 値は、パーツ名をキーとしレイヤーごとのカラー情報のリストを値とするマップ
	 */
	public Map<String, Map<String, List<IndependentPartsColorInfo>>> getPartsColorMap() {
		return partsColorMap;
	}

	public void setPartsColorMap(
			Map<String, Map<String, List<IndependentPartsColorInfo>>> partsColorMap) {
		if (partsColorMap == null) {
			this.partsColorMap = Collections.emptyMap();
		}
		this.partsColorMap = partsColorMap;
	}

	public void setLastUsedExportDir(File lastUsedExportDir) {
		this.lastUsedExportDir = lastUsedExportDir;
	}

	public File getLastUsedExportDir() {
		return lastUsedExportDir;
	}

	public void setLastUsedSaveDir(File lastUsedSaveDir) {
		this.lastUsedSaveDir = lastUsedSaveDir;
	}

	public File getLastUsedSaveDir() {
		return lastUsedSaveDir;
	}

	public void setWallpaperInfo(WallpaperInfo wallpaperInfo) {
		this.wallpaperInfo = wallpaperInfo;
	}

	public WallpaperInfo getWallpaperInfo() {
		return wallpaperInfo;
	}

	public IndependentPartsSetInfo getCurrentPartsSet() {
		return currentPartsSet;
	}

	public void setCurrentPartsSet(IndependentPartsSetInfo currentPartsSet) {
		this.currentPartsSet = currentPartsSet;
	}

	public IndependentPartsSetInfo getLastUsePresetParts() {
		return lastUsePresetParts;
	}

	public void setLastUsePresetParts(IndependentPartsSetInfo lastUsePresetParts) {
		this.lastUsePresetParts = lastUsePresetParts;
	}

	/**
	 * キャラクターデータを指定して、指定されたキャラクターデータ上のインスタンスと関連づけられた
	 * カテゴリおよびパーツ名などのインスタンスで構成されるパーツ識別名とカラー情報を、 引数で指定したマップに出力する.
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @param partsColorInfoMap
	 *            パーツ識別名とカラー情報を出力するマップ
	 */
	public void createCompatible(CharacterData characterData,
			Map<PartsIdentifier, PartsColorInfo> partsColorInfoMap) {
		if (characterData == null || partsColorInfoMap == null) {
			throw new IllegalArgumentException();
		}

		for (Map.Entry<String, Map<String, List<IndependentPartsColorInfo>>> catEntry : partsColorMap
				.entrySet()) {
			String categoryId = catEntry.getKey();
			for (Map.Entry<String, List<IndependentPartsColorInfo>> layerEntry : catEntry
					.getValue().entrySet()) {
				String partsName = layerEntry.getKey();
				List<IndependentPartsColorInfo> partsColorInfos = layerEntry
						.getValue();

				PartsCategory partsCategory = characterData
						.getPartsCategory(categoryId);
				if (partsCategory != null) {
					String localizedName = partsName;
					PartsIdentifier partsIdentifier = new PartsIdentifier(
							partsCategory, partsName, localizedName);

					PartsColorInfo partsColorInfo = IndependentPartsColorInfo
							.buildPartsColorInfo(characterData, partsCategory,
									partsColorInfos);
					if (partsColorInfo != null) {
						partsColorInfoMap.put(partsIdentifier, partsColorInfo);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("(characterDocBase=").append(characterDocBase);
		buf.append(", characterDataSig=").append(characterDataSig);
		buf.append(", partsColorMap=").append(partsColorMap);
		buf.append(", currentPartsSet=").append(currentPartsSet);
		buf.append(", lastUsedSaveDir=").append(lastUsedSaveDir);
		buf.append(", lastUsedExportDir=").append(lastUsedExportDir);
		buf.append(")");
		return buf.toString();
	}
}
