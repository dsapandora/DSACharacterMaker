package charactermanaj.model;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.ui.model.WallpaperInfo;

public class WorkingSet implements Serializable {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(WorkingSet.class.getName());

	private static final long serialVersionUID = -4728766140876842242L;

	private Map<PartsIdentifier, PartsColorInfo> partsColorInfoMap;
	
	private String characterDataRev;
	
	/**
	 * 現在の選択中のパーツと色設定からのパーツセット
	 */
	private PartsSet partsSet;
	
	private URI characterDocBase;
	
	private File lastUsedSaveDir;
	
	private File lastUsedExportDir;

	// ver0.92
	private PartsSet lastUsePresetParts;
	
	// ver0.94
	private CharacterData characterData;
	
	// ver0.97
	private WallpaperInfo wallpaperInfo;
	

	/**
	 * デシリアライズ
	 * 
	 * @param inp
	 *            オブジェクトの復元ストリーム
	 * @throws IOException
	 *             例外
	 * @throws ClassNotFoundException
	 *             例外
	 */
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream inp) throws IOException,
			ClassNotFoundException {
		GetField fields = inp.readFields();

		ObjectStreamClass sig = fields.getObjectStreamClass();
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "WorkingSetのデシリアライズ name=" + sig.getName()
					+ "/sid=" + sig.getSerialVersionUID());
		}
		
		partsColorInfoMap = (Map<PartsIdentifier, PartsColorInfo>) fields.get("partsColorInfoMap", null);
		characterDataRev = (String) fields.get("characterDataRev", null);
		partsSet = (PartsSet) fields.get("partsSet", null);
		
		Object anyDocBase = fields.get("characterDocBase", null);
		if (anyDocBase != null && anyDocBase instanceof URL) {
			File file = new File(((URL) anyDocBase).getPath());
			anyDocBase = file.toURI();
		}
		// ver0.95からURI, それ以前はURL
		characterDocBase = (URI) anyDocBase;
		
		lastUsedSaveDir = (File) fields.get("lastUsedSaveDir", null);
		lastUsedExportDir = (File) fields.get("lastUsedExportDir", null);

		// ver0.92
		lastUsePresetParts = (PartsSet) fields.get("lastUsePresetParts", null);
		
		// ver0.94
		characterData = (CharacterData) fields.get("characterData", null);
		
		// ver0.97
		wallpaperInfo = (WallpaperInfo) fields.get("wallpaperInfo", null);
	}
	
	
	public void setCharacterDataRev(String characterDataRev) {
		this.characterDataRev = characterDataRev;
	}
	
	/**
	 * REV情報.<br>
	 * キャラクターデータが設定されていない場合に使用される.<br>
	 * (ver0.96以前旧シリアライズデータ互換用)<br>
	 * 
	 * @return
	 */
	public String getCharacterDataRev() {
		return characterDataRev;
	}
	
	public Map<PartsIdentifier, PartsColorInfo> getPartsColorInfoMap() {
		return partsColorInfoMap;
	}
	
	public void setPartsColorInfoMap(
			Map<PartsIdentifier, PartsColorInfo> partsColorInfoMap) {
		this.partsColorInfoMap = partsColorInfoMap;
	}
	
	public void setCharacterDocBase(URI characterDocBase) {
		this.characterDocBase = characterDocBase;
	}
	
	public void setPartsSet(PartsSet partsSet) {
		this.partsSet = partsSet;
	}
	
	public URI getCharacterDocBase() {
		return characterDocBase;
	}
	
	public PartsSet getPartsSet() {
		return partsSet;
	}
	
	public void setLastUsedSaveDir(File lastUsedSaveDir) {
		this.lastUsedSaveDir = lastUsedSaveDir;
	}
	
	public void setLastUsedExportDir(File lastUsedExportDir) {
		this.lastUsedExportDir = lastUsedExportDir;
	}
	
	public File getLastUsedSaveDir() {
		return lastUsedSaveDir;
	}
	
	public File getLastUsedExportDir() {
		return lastUsedExportDir;
	}

	/**
	 * 最後に使用したお気に入りの情報.<br>
	 * 一度もプリセットを使ってなければnull.<br>
	 * ver0.94以前には存在しなかったため、nullになりえます。
	 * 
	 * @return
	 */
	public PartsSet getLastUsePresetParts() {
		return lastUsePresetParts;
	}

	/**
	 * /** 最後に使用したお気に入りの情報.<br>
	 * 一度もプリセットを使ってなければnull.<br>
	 * (ver0.94以前には存在しなかったため、nullになりえます。)
	 * 
	 * @param lastUsePresetParts
	 */
	public void setLastUsePresetParts(PartsSet lastUsePresetParts) {
		this.lastUsePresetParts = lastUsePresetParts;
	}
	
	public void setCharacterData(CharacterData characterData) {
		this.characterData = characterData;
	}
	
	/**
	 * 使用していたキャラクター定義を取得します.<br>
	 * ver0.95よりも以前には存在しないため、nullとなりえます.<br>
	 * 
	 * @return キャラクターデータ
	 */
	public CharacterData getCharacterData() {
		return characterData;
	}

	/**
	 * 壁紙情報を取得します.<br>
	 * ver0.97よりも以前には存在しないため、nullとなりえます.<br>
	 * 
	 * @return 壁紙情報
	 */
	public WallpaperInfo getWallpaperInfo() {
		return wallpaperInfo;
	}
	
	public void setWallpaperInfo(WallpaperInfo wallpaperInfo) {
		this.wallpaperInfo = wallpaperInfo;
	}
	
	@Override
	public String toString() {
		return "docBase:" + characterDocBase + "/rev:" + characterDataRev;
	}
}
