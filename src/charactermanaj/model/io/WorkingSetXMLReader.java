package charactermanaj.model.io;

import static charactermanaj.util.XMLUtilities.getChildElements;
import static charactermanaj.util.XMLUtilities.getElementText;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import charactermanaj.model.IndependentPartsColorInfo;
import charactermanaj.model.IndependentPartsSetInfo;
import charactermanaj.model.WorkingSet2;
import charactermanaj.ui.model.WallpaperInfo;
import charactermanaj.ui.model.WallpaperInfo.WallpaperResourceType;
import charactermanaj.util.XMLUtilities;

/**
 * ワーキングセットのXMLデータを読み込む.<br>
 * 
 * @author seraphy
 */
public class WorkingSetXMLReader {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(WorkingSetXMLReader.class.getCanonicalName());

	/**
	 * WorkingSetのXMLファイルの名前空間
	 */
	private static final String NS_PREFIX = "http://charactermanaj.sourceforge.jp/schema/charactermanaj-workingset";

	/**
	 * XMLコンテンツに対する入力ストリームからワーキングセットを取り出す.<br>
	 * 
	 * @param is
	 *            入力ストリーム
	 * @throws IOException
	 *             読み取りに失敗
	 */
	public WorkingSet2 loadWorkingSet(InputStream is)
			throws IOException {
		if (is == null) {
			throw new IllegalArgumentException();
		}

		WorkingSet2 workingSet = new WorkingSet2();

		CharacterDataXMLReader characterDataXMLReader = new CharacterDataXMLReader();
		
		Document doc = XMLUtilities.loadDocument(is);

		String lang = Locale.getDefault().getLanguage();

		try {
			Element docElm = doc.getDocumentElement();
			if (!"character-workingset".equals(docElm.getNodeName())) {
				throw new IOException("Invalid Format.");
			}
			String ns = docElm.getNamespaceURI();
			if (ns == null || !ns.startsWith(NS_PREFIX)) {
				throw new IOException("unsupported xml format");
			}

			String docVersion = docElm.getAttribute("version").trim();
			if (!"1.0".equals(docVersion)) {
				throw new IOException("unsupported version: " + docVersion);
			}

			// docbase
			String characterDocBase = docElm.getAttribute("characterDocBase")
					.trim();
			try {
				workingSet.setCharacterDocBase(new URI(characterDocBase));

			} catch (URISyntaxException ex) {
				IOException ex2 = new IOException("WorkingSet invalid format.");
				ex2.initCause(ex);
				throw ex2;
			}

			// character data signature
			String characterDataSig = getElementText(docElm, "characterDataSig");
			workingSet.setCharacterDataSig(characterDataSig);

			// キーはカテゴリid, 値は、パーツ名をキーとしレイヤーごとのカラー情報のリストを値とするマップ
			HashMap<String, Map<String, List<IndependentPartsColorInfo>>> partsColorMap = new HashMap<String, Map<String, List<IndependentPartsColorInfo>>>();
			for (Element partsColorInfoMapElm : getChildElements(docElm,
					"partsColorInfoMap")) {
				// カラー定義マップを読み込む
				HashMap<String, List<IndependentPartsColorInfo>> colorMap = new HashMap<String, List<IndependentPartsColorInfo>>();
				for (Element colorsElm : getChildElements(partsColorInfoMapElm,
						"colors")) {
					for (Element colorElm : getChildElements(colorsElm, "color")) {
						String colorId = colorElm.getAttribute("id");
						List<IndependentPartsColorInfo> colorInfoList = characterDataXMLReader
								.readPartsColor(colorElm);
						colorMap.put(colorId, colorInfoList);
					}
				}

				// パーツごとのカラー情報を読み込む
				for (Element partsListElm : getChildElements(
						partsColorInfoMapElm, "partsList")) {
					for (Element partsElm : getChildElements(partsListElm,
							"partsIdentifier")) {
						String categoryId = partsElm.getAttribute("categoryId");
						String name = partsElm.getAttribute("name");
						String colorId = partsElm.getAttribute("colorId");

						Map<String, List<IndependentPartsColorInfo>> partsMap = partsColorMap
								.get(categoryId);
						if (partsMap == null) {
							partsMap = new HashMap<String, List<IndependentPartsColorInfo>>();
							partsColorMap.put(categoryId, partsMap);
						}
						List<IndependentPartsColorInfo> colorInfo = colorMap
								.get(colorId);
						if (colorInfo == null) {
							logger.warning("undefined colorId:" + colorId);
						} else {
							partsMap.put(name, colorInfo);
						}
					}
				}
			}
			workingSet.setPartsColorMap(partsColorMap);

			// 最後に使用した保存先ディレクトリ
			String lastUsedSaveDirStr = getElementText(docElm,
					"lastUsedSaveDir");
			if (lastUsedSaveDirStr != null
					&& lastUsedSaveDirStr.trim().length() > 0) {
				workingSet.setLastUsedSaveDir(new File(lastUsedSaveDirStr
						.trim()));
			}

			// 最後に使用したエクスポート先ディレクトリ
			String lastUsedExportDirStr = getElementText(docElm,
					"lastUsedExportDir");
			if (lastUsedExportDirStr != null
					&& lastUsedExportDirStr.trim().length() > 0) {
				workingSet.setLastUsedExportDir(new File(lastUsedExportDirStr
						.trim()));
			}

			// 壁紙情報
			WallpaperInfo wallpaperInfo = null;
			for (Element wallpaperElm : getChildElements(docElm,
					"wallpaperInfo")) {
				wallpaperInfo = readWallpaperInfo(wallpaperElm);
				break; // wallpaperInfoは最初の一要素しか想定しない.
			}
			workingSet.setWallpaperInfo(wallpaperInfo);

			// 現在のパーツ情報
			for (Element currentPartsSetsElm : getChildElements(docElm,
					"currentPartsSet")) {
				for (Element presetElm : getChildElements(currentPartsSetsElm,
						"preset")) {
					IndependentPartsSetInfo currentPartsSet = characterDataXMLReader
							.loadPartsSet(presetElm, lang);
					workingSet.setCurrentPartsSet(currentPartsSet);
					break; // 最初の一要素のみ
				}
				break; // 最初の一要素のみ
			}

			// 最後に使ったお気に入り情報
			for (Element lastUsePresetPartsElm : getChildElements(docElm,
					"lastUsePresetParts")) {
				for (Element presetElm : getChildElements(
						lastUsePresetPartsElm, "preset")) {
					IndependentPartsSetInfo lastUsePresetParts = characterDataXMLReader
							.loadPartsSet(presetElm, lang);
					workingSet.setLastUsePresetParts(lastUsePresetParts);
					break; // 最初の一要素のみ
				}
				break; // 最初の一要素のみ
			}

			return workingSet;

		} catch (RuntimeException ex) {
			IOException ex2 = new IOException("WorkingSet invalid format.");
			ex2.initCause(ex);
			throw ex2;
		}
	}

	/**
	 * 壁紙情報を読み込む
	 * 
	 * @param elm
	 *            壁紙要素
	 * @return 壁紙情報、elmがnullの場合はnullを返す.
	 */
	private WallpaperInfo readWallpaperInfo(Element elm) {
		if (elm == null) {
			return null;
		}
		WallpaperInfo wallpaperInfo = new WallpaperInfo();

		String typStr = getElementText(elm, "type");
		WallpaperResourceType typ = WallpaperResourceType.valueOf(typStr);
		wallpaperInfo.setType(typ);

		String res = getElementText(elm, "resource");
		if (res != null && res.trim().length() > 0) {
			wallpaperInfo.setResource(res.trim());
		}

		String fileStr = getElementText(elm, "file");
		if (fileStr != null && fileStr.trim().length() > 0) {
			wallpaperInfo.setFile(new File(fileStr.trim()));
		}

		float alpha = 0f;
		String alphaStr = getElementText(elm, "alpha");
		if (alphaStr != null && alphaStr.trim().length() > 0) {
			alpha = Float.parseFloat(alphaStr);
			wallpaperInfo.setAlpha(alpha);
		}

		String backgroundColorStr = getElementText(elm, "backgroundColor");
		if (backgroundColorStr != null
				&& backgroundColorStr.trim().length() > 0) {
			Color backgroundColor = Color.decode(backgroundColorStr.trim());
			wallpaperInfo.setBackgroundColor(backgroundColor);
		}

		return wallpaperInfo;
	}
}
