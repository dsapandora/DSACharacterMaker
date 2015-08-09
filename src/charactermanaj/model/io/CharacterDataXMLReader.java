package charactermanaj.model.io;

import static charactermanaj.util.XMLUtilities.getChildElements;
import static charactermanaj.util.XMLUtilities.getElementText;
import static charactermanaj.util.XMLUtilities.getFirstChildElement;
import static charactermanaj.util.XMLUtilities.getLocalizedElementText;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import charactermanaj.graphics.colormodel.ColorModels;
import charactermanaj.graphics.filters.ColorConv;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.model.CharacterData;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.IndependentPartsColorInfo;
import charactermanaj.model.IndependentPartsSetInfo;
import charactermanaj.model.IndependentPartsSetInfoList;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsSet;
import charactermanaj.model.RecommendationURL;
import charactermanaj.util.XMLUtilities;

/**
 * キャラクターデータを格納したXMLを読み込むためのクラス.
 * 
 * @author seraphy
 */
public class CharacterDataXMLReader {

	/**
	 * character.xmlのデフォルトの名前空間
	 */
	private static final String NS_PREFIX = "http://charactermanaj.sourceforge.jp/schema/charactermanaj";

	/**
	 * favorites.xmlのデフォルトの名前空間.
	 * (character.xmlの名前空間と別にすべきだったかもしれないが、v0.991まで、これでやってきたので、とりあえず、このままとする。)
	 */
	private static final String NS_PREFIX_FAVORITES = "http://charactermanaj.sourceforge.jp/schema/charactermanaj";

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(CharacterDataXMLReader.class.getName());

	/**
	 * キャラクター定義(プロファイル)をロードする.
	 * 
	 * @param docBase
	 *            対象xml
	 * @return キャラクターデータ
	 * @throws IOException
	 */
	public CharacterData loadCharacterDataFromXML(URI docBase)
			throws IOException {
		if (docBase == null) {
			throw new IllegalArgumentException();
		}

		URL docBaseURL = docBase.toURL();
		CharacterData cd;
		InputStream is = docBaseURL.openStream();
		try {
			cd = loadCharacterDataFromXML(is, docBase);

		} finally {
			is.close();
		}
		return cd;
	}

	/**
	 * XMLコンテンツに対する入力ストリームからキャラクターデータを取り出す.<br>
	 * docbaseはXMLファイルの位置を示すものであり、XMLデータ中には含まれず、キャラクターデータのロード時にセットされる.<br>
	 * そのため引数としてdocbaseを引き渡す.<br>
	 * 読み取りは現在のデフォルトロケールで行われる.<br>
	 * 
	 * @param is
	 *            入力ストリーム
	 * @param docBase
	 *            XMLファイルの位置を示すURI、nullの場合はnullが設定される。
	 * @param docInfo
	 *            ドキュメントのタイプ
	 * @return 読み取られたプロファイル
	 * @throws IOException
	 *             読み取りに失敗
	 */
	public CharacterData loadCharacterDataFromXML(InputStream is, URI docBase)
			throws IOException {
		return loadCharacterDataFromXML(is, docBase, Locale.getDefault());
	}

	/**
	 * XMLコンテンツに対する入力ストリームからキャラクターデータを取り出す.<br>
	 * docbaseはXMLファイルの位置を示すものであり、XMLデータ中には含まれず、キャラクターデータのロード時にセットされる.<br>
	 * そのため引数としてdocbaseを引き渡す.<br>
	 * 設定ファイル中の表示文字列にロケール指定がある場合、引数に指定したロケールに合致する言語の情報を取得する.<br>
	 * 合致するものがなければ最初のものを使用する.<br>
	 * 
	 * @param is
	 *            入力ストリーム
	 * @param docBase
	 *            XMLファイルの位置を示すURI、nullの場合はnullが設定される。
	 * @param docInfo
	 *            ドキュメントのタイプ
	 * @param locale
	 *            読み取るロケール
	 * @return 読み取られたプロファイル
	 * @throws IOException
	 *             読み取りに失敗
	 */
	public CharacterData loadCharacterDataFromXML(InputStream is, URI docBase,
			Locale locale) throws IOException {
		if (is == null || locale == null) {
			throw new IllegalArgumentException();
		}

		Document doc = XMLUtilities.loadDocument(is);

		CharacterData characterData = new CharacterData();
		characterData.setDocBase(docBase);

		try {
			Element docElm = doc.getDocumentElement();
			if (!"character".equals(docElm.getNodeName())) {
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
			String characterId = docElm.getAttribute("id").trim();
			String characterRev = docElm.getAttribute("rev").trim();

			characterData.setId(characterId);
			characterData.setRev(characterRev);

			// language
			String lang = locale.getLanguage();

			// name
			String characterName = getLocalizedElementText(docElm, "name", lang);
			if (characterName == null) {
				characterName = "default";
			}
			characterData.setName(characterName.trim());

			// information/author, information/description
			String author = null;
			String description = null;
			for (Element infoElm : getChildElements(docElm, "information")) {
				if (author == null) {
					author = getLocalizedElementText(infoElm, "author", lang);
				}
				if (description == null) {
					description = getLocalizedElementText(infoElm,
							"description", lang);
				}
			}
			if (author == null) {
				author = "";
			}
			characterData.setAuthor(author.trim());

			if (description == null) {
				description = null;
			}
			characterData.setDescription(description);

			// image-size/width, image-size/height
			int width = 0;
			int height = 0;

			for (Element sizeElm : getChildElements(docElm, "image-size")) {
				String tmpWidth = getLocalizedElementText(sizeElm, "width",
						lang);
				if (tmpWidth != null && tmpWidth.trim().length() > 0) {
					width = Integer.parseInt(tmpWidth.trim());
				}
				String tmpHeight = getLocalizedElementText(sizeElm, "height",
						lang);
				if (tmpHeight != null && tmpHeight.trim().length() > 0) {
					height = Integer.parseInt(tmpHeight.trim());
				}
				break;
			}
			if (width <= 0) {
				width = 300;
			}
			if (height <= 0) {
				height = 400;
			}
			characterData.setImageSize(new Dimension(width, height));

			// settings
			for (Element settingElm : getChildElements(docElm, "settings")) {
				for (Element entElm : getChildElements(settingElm, "entry")) {
					String key = entElm.getAttribute("key").trim();
					String val = entElm.getTextContent();
					characterData.setProperty(key, val);
				}
			}

			// colorGroups/colorGroup
			ArrayList<ColorGroup> colorGroups = new ArrayList<ColorGroup>();
			for (Element colorGroupsElm : getChildElements(docElm,
					"colorGroups")) {
				for (Element colorGroupElm : getChildElements(colorGroupsElm,
						"colorGroup")) {
					String colorGroupId = colorGroupElm.getAttribute("id")
							.trim();
					String colorGroupDisplayName = getLocalizedElementText(
							colorGroupElm, "display-name", lang);

					ColorGroup colorGroup = new ColorGroup(colorGroupId,
							colorGroupDisplayName);
					colorGroups.add(colorGroup);
				}
			}
			characterData.setColorGroups(colorGroups);

			// categories/category
			ArrayList<PartsCategory> categories = new ArrayList<PartsCategory>();
			for (Element catsElm : getChildElements(docElm, "categories")) {
				for (Element catElm : getChildElements(catsElm, "category")) {
					String categoryId = catElm.getAttribute("id").trim();
					boolean multipleSelectable = Boolean.parseBoolean(catElm
							.getAttribute("multipleSelectable"));

					String categoryDisplayName = getLocalizedElementText(
							catElm, "display-name", lang);

					int visibleRows = 0;
					String tmpVisibleRows = getLocalizedElementText(catElm,
							"visible-rows", lang);
					if (tmpVisibleRows != null
							&& tmpVisibleRows.trim().length() > 0) {
						visibleRows = Integer.parseInt(tmpVisibleRows.trim());
					}
					if (visibleRows <= 0) {
						visibleRows = 0;
					}

					// layers/layer
					ArrayList<Layer> layers = new ArrayList<Layer>();
					for (Element layersElm : getChildElements(catElm, "layers")) {
						for (Element layerElm : getChildElements(layersElm,
								"layer")) {
							String layerId = layerElm.getAttribute("id");
							String layerDisplayName = getLocalizedElementText(
									layerElm, "display-name", lang);

							// レイヤーの重ね順
							String strOrder = getElementText(layerElm, "order");
							int order = layers.size();
							if (strOrder != null
									&& strOrder.trim().length() > 0) {
								order = Integer.parseInt(strOrder.trim());
							}

							// レイヤーの画像ディレクトリ名
							String layerDir = getElementText(layerElm, "dir");
							if (layerDir == null
									|| layerDir.trim().length() == 0) {
								throw new IOException("layer's dir is null");
							}

							// カラーモデル(省略可)
							String colorModelName = getElementText(layerElm,
									"colorModel");
							if (colorModelName == null
									|| colorModelName.length() == 0) {
								// 省略時はデフォルトのカラーモデル名を使用する.
								colorModelName = ColorModels.DEFAULT.name();
							}

							// layer/colorGroup カラーグループ
							boolean initSync = false;
							ColorGroup colorGroup = null;
							Element lcgElm = getFirstChildElement(layerElm,
									"colorGroup");
							if (lcgElm != null) {
								String tmpInitSync = lcgElm
										.getAttribute("init-sync");
								if (tmpInitSync.trim().length() > 0) {
									initSync = Boolean.parseBoolean(tmpInitSync
											.trim());
								}
								if (colorGroup == null) {
									String colorGroupRefId = lcgElm
											.getAttribute("refid").trim();
									colorGroup = characterData
											.getColorGroup(colorGroupRefId);
								}
							}

							Layer layer = new Layer(layerId, layerDisplayName,
									order, colorGroup, initSync, layerDir,
									colorModelName);
							layers.add(layer);
						}
					}

					PartsCategory category = new PartsCategory(
							categories.size(), categoryId, categoryDisplayName,
							multipleSelectable, visibleRows,
							layers.toArray(new Layer[layers.size()]));
					categories.add(category);
				}
			}
			characterData.setPartsCategories(categories
					.toArray(new PartsCategory[categories.size()]));

			// presets
			for (Element presetssElm : getChildElements(docElm, "presets")) {
				loadPartsSet(characterData, presetssElm, true, lang);
			}

			// recommendations
			List<RecommendationURL> recommendationURLList = null; // お勧めノードがない場合はnull
			for (Element recmsElm : getChildElements(docElm, "recommendations")) {
				for (Element recmElm : getChildElements(recmsElm,
						"recommendation")) {
					String recommentDescription = getLocalizedElementText(
							recmElm, "description", lang);
					String url = getLocalizedElementText(recmElm, "URL", lang);

					if (recommentDescription != null) {
						recommentDescription = recommentDescription.trim();
					}
					if (url != null) {
						url = url.trim();
					}

					RecommendationURL recommendationURL = new RecommendationURL();
					recommendationURL.setDisplayName(recommentDescription);
					recommendationURL.setUrl(url);

					if (recommendationURLList == null) {
						recommendationURLList = new ArrayList<RecommendationURL>();
					}
					recommendationURLList.add(recommendationURL);
				}
			}
			characterData.setRecommendationURLList(recommendationURLList);

		} catch (RuntimeException ex) {
			IOException ex2 = new IOException("CharacterData invalid format.");
			ex2.initCause(ex);
			throw ex2;
		}

		return characterData;
	}

	/**
	 * 入力ストリームからパーツセット定義(Favorites.xml)を読み込んで、 characterDataに追加登録する.<br>
	 * 
	 * @param characterData
	 *            お気に入りを登録されるキャラクターデータ
	 * @param inpstm
	 *            お気に入りのxmlへの入力ストリーム
	 * @param docInfo
	 *            ドキュメントタイプ
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public void loadPartsSet(CharacterData characterData, InputStream inpstm)
			throws IOException {
		if (characterData == null || inpstm == null) {
			throw new IllegalArgumentException();
		}

		Document doc = XMLUtilities.loadDocument(inpstm);
		Element docElm = doc.getDocumentElement();
		if (!"partssets".equals(docElm.getNodeName())) {
			logger.log(Level.WARNING, "invalid partsets format.");
			return;
		}

		String ns = docElm.getNamespaceURI();
		if (ns == null || !ns.startsWith(NS_PREFIX_FAVORITES)) {
			logger.log(Level.WARNING, "invalid partsets format.");
			return;
		}

		String lang = Locale.getDefault().getLanguage();
		loadPartsSet(characterData, docElm, false, lang);
	}

	/**
	 * CharacterDataのプリセットまたはFavoritesのパーツセットのXMLからパーツセットを読み取って登録する.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @param nodePartssets
	 *            パーツセットのノード、プリセットまたはパーツセットノード
	 * @param presetParts
	 *            ロードしたパーツセットにプリセットフラグをたてる場合はtrue
	 * @param lang
	 *            言語
	 */
	protected void loadPartsSet(CharacterData characterData,
			Element nodePartssets, boolean presetParts, String lang) {
		IndependentPartsSetInfoList partsSetLst = loadPartsSetList(
				nodePartssets, lang);
		logger.info("partsSetList: size=" + partsSetLst.size());

		if (presetParts) {
			characterData
					.setDefaultPartsSetId(partsSetLst.getDefaultPresetId());
		}

		for (IndependentPartsSetInfo partsSetInfo : partsSetLst) {
			PartsSet partsSet = IndependentPartsSetInfo.convertPartsSet(
					partsSetInfo, characterData, presetParts);
			characterData.addPartsSet(partsSet);
		}
	}

	/**
	 * CharacterDataのプリセットまたはFavoritesのパーツセットのXMLからパーツセットを読み取って登録する.<br>
	 * 
	 * @param nodePartssets
	 *            パーツセットのノード、プリセットまたはパーツセットノード
	 * @param lang
	 *            言語
	 */
	public IndependentPartsSetInfoList loadPartsSetList(Element nodePartssets,
			String lang) {
		if (nodePartssets == null || lang == null || lang.length() == 0) {
			throw new IllegalArgumentException();
		}

		IndependentPartsSetInfoList partsSetLst = new IndependentPartsSetInfoList();

		// デフォルトのパーツセットID
		String defaultPresetId = nodePartssets.getAttribute("default-preset");
		if (defaultPresetId != null) {
			defaultPresetId = defaultPresetId.trim();
		}

		// パーツセットリストの読み込み
		for (Element presetElm : getChildElements(nodePartssets, "preset")) {
			IndependentPartsSetInfo partsSetInfo = loadPartsSet(presetElm, lang);
			if (partsSetInfo != null) {
				String partsSetId = partsSetInfo.getId();

				// デフォルトのパーツセットIDがない場合は先頭をデフォルトとみなす.
				if (defaultPresetId == null || defaultPresetId.length() == 0) {
					defaultPresetId = partsSetId;
				}

				partsSetLst.add(partsSetInfo);
			}
		}

		if (defaultPresetId.length() == 0) {
			// デフォルトパーツセットがないことを示すためのnull
			defaultPresetId = null;
		}
		partsSetLst.setDefaultPresetId(defaultPresetId);

		return partsSetLst;
	}

	/**
	 * CharacterDataのプリセットまたはFavoritesのパーツセットのXMLからパーツセットを読み取る.<br>
	 * 
	 * @param nodePartssets
	 *            パーツセットのノード、プリセットまたはパーツセットノード
	 * @param lang
	 *            言語
	 * @return 素のパーツセット情報、無ければnull
	 */
	public IndependentPartsSetInfo loadPartsSet(Element presetElm, String lang) {
		if (presetElm == null || lang == null) {
			return null;
		}

		IndependentPartsSetInfo partsSetInfo = new IndependentPartsSetInfo();

		// id
		String partsSetId = presetElm.getAttribute("id");
		if (partsSetId != null) {
			partsSetId = partsSetId.trim();
		}
		if (partsSetId != null && partsSetId.length() == 0) {
			partsSetId = null;
		}
		partsSetInfo.setId(partsSetId);

		// display-name
		String displayName = getLocalizedElementText(presetElm, "display-name",
				lang);
		partsSetInfo.setDisplayName(displayName);

		// bgColor
		Element bgColorElm = getFirstChildElement(presetElm, "background-color");
		if (bgColorElm != null) {
			String tmpBgColor = bgColorElm.getAttribute("color");
			try {
				Color bgColor = Color.decode(tmpBgColor);
				partsSetInfo.setBackgroundColor(bgColor);

			} catch (Exception ex) {
				logger.log(Level.WARNING, "bgColor parameter is invalid. :"
						+ tmpBgColor, ex);
				// 無視する
			}
		}

		// affine-transform-parameter
		String tmpAffienTrans = getElementText(presetElm,
				"affine-transform-parameter");
		if (tmpAffienTrans != null && tmpAffienTrans.trim().length() > 0) {
			try {
				ArrayList<Double> affineTransformParameterArr = new ArrayList<Double>();
				for (String strParam : tmpAffienTrans.split("\\s+")) {
					affineTransformParameterArr.add(Double.valueOf(strParam));
				}
				double[] affineTransformParameter = new double[affineTransformParameterArr
						.size()];
				int idx = 0;
				for (double aaffineItem : affineTransformParameterArr) {
					affineTransformParameter[idx++] = aaffineItem;
				}
				partsSetInfo
						.setAffineTransformParameter(affineTransformParameter);

			} catch (Exception ex) {
				logger.log(Level.WARNING,
						"affine transform parameter is invalid. :"
								+ tmpAffienTrans, ex);
				// 無視する.
			}
		}

		// カテゴリIDをキーとし、パーツ名をキーとしカラー情報のリストを値とするマップを値とする.
		Map<String, Map<String, List<IndependentPartsColorInfo>>> partsMap = partsSetInfo
				.getPartsMap();

		// Category
		for (Element catElm : getChildElements(presetElm, "category")) {
			String categoryId = catElm.getAttribute("refid");
			if (categoryId != null) {
				categoryId = categoryId.trim();
			}
			if (categoryId == null || categoryId.length() == 0) {
				logger.log(Level.WARNING, "missing category refid: " + catElm);
				continue;
			}

			// パーツ名をキーとしカラー情報のリストを値とするマップ.
			Map<String, List<IndependentPartsColorInfo>> categoryPartsMap = partsMap
					.get(categoryId);
			if (categoryPartsMap == null) {
				categoryPartsMap = new HashMap<String, List<IndependentPartsColorInfo>>();
				partsMap.put(categoryId, categoryPartsMap);
			}

			// Parts
			for (Element partsElm : getChildElements(catElm, "parts")) {
				String partsName = partsElm.getAttribute("name");
				if (partsName != null) {
					partsName = partsName.trim();
				}
				if (partsName == null || partsName.length() == 0) {
					logger.log(Level.WARNING, "missing parts name. " + partsElm);
					continue;
				}

				// Color/Layer
				List<IndependentPartsColorInfo> infoList = null;
				for (Element colorElm : getChildElements(partsElm, "color")) {
					infoList = readPartsColor(colorElm);
					break;
				}
				categoryPartsMap.put(partsName, infoList);
			}
		}
		return partsSetInfo;
	}

	/**
	 * パーツごとのカラー情報のXMLを読み込んで返す.<br>
	 * パーツは複数のレイヤーから構成されるので、複数レイヤーのカラー情報のリストとして返される.<br>
	 * (パーツは複数レイヤーをまとめる1つのカテゴリになるので、カテゴリ単位の情報となる.)<br>
	 * 
	 * @param colorElm
	 *            カラーのXML要素
	 * @return 素のカラー情報
	 */
	public List<IndependentPartsColorInfo> readPartsColor(Element colorElm) {
		if (colorElm == null) {
			throw new IllegalArgumentException();
		}

		ArrayList<IndependentPartsColorInfo> infoList = new ArrayList<IndependentPartsColorInfo>();
		for (Element layerElm : getChildElements(colorElm, "layer")) {
			IndependentPartsColorInfo info = new IndependentPartsColorInfo();

			String layerId = layerElm.getAttribute("refid");
			if (layerId != null) {
				layerId = layerId.trim();
			}
			if (layerId == null || layerId.length() == 0) {
				logger.log(Level.WARNING, "missing layer-id: " + layerElm);
				continue;
			}
			info.setLayerId(layerId);

			// color-group
			Element colorGroupElm = getFirstChildElement(layerElm,
					"color-group");
			if (colorGroupElm != null) {
				String colorGroupId = colorGroupElm.getAttribute("group")
						.trim();
				info.setColorGroupId(colorGroupId);
				boolean syncColorGroup = Boolean.parseBoolean(colorGroupElm
						.getAttribute("synchronized").trim());
				info.setSyncColorGroup(syncColorGroup);
			}

			// rgb
			ColorConvertParameter param = info.getColorConvertParameter();
			Element nodeRgb = getFirstChildElement(layerElm, "rgb");
			if (nodeRgb != null) {
				for (Element elmRgb : getChildElements(nodeRgb, null)) {
					String rgbName = elmRgb.getNodeName();
					int offset = Integer
							.parseInt(elmRgb.getAttribute("offset"));
					float factor = Float.parseFloat(elmRgb
							.getAttribute("factor"));
					float gamma = Float
							.parseFloat(elmRgb.getAttribute("gamma"));
					if ("red".equals(rgbName)) {
						param.setOffsetR(offset);
						param.setFactorR(factor);
						param.setGammaR(gamma);
					} else if ("green".equals(rgbName)) {
						param.setOffsetG(offset);
						param.setFactorG(factor);
						param.setGammaG(gamma);
					} else if ("blue".equals(rgbName)) {
						param.setOffsetB(offset);
						param.setFactorB(factor);
						param.setGammaB(gamma);
					} else if ("alpha".equals(rgbName)) {
						param.setOffsetA(offset);
						param.setFactorA(factor);
						param.setGammaA(gamma);
					}
				}
			}

			// hsb
			Element elmHsb = getFirstChildElement(layerElm, "hsb");
			if (elmHsb != null) {
				float hue = Float.parseFloat(elmHsb.getAttribute("hue"));
				float saturation = Float.parseFloat(elmHsb
						.getAttribute("saturation"));
				float brightness = Float.parseFloat(elmHsb
						.getAttribute("brightness"));
				String strContrast = elmHsb.getAttribute("contrast").trim();
				param.setHue(hue);
				param.setSaturation(saturation);
				param.setBrightness(brightness);
				if (strContrast != null && strContrast.length() > 0) {
					// ver0.96追加 optional
					float contrast = Float.parseFloat(strContrast);
					param.setContrast(contrast);
				}
			}

			// rgb-replace
			Element elmRgbReplace = getFirstChildElement(layerElm,
					"rgb-replace");
			if (elmRgbReplace != null) {
				Float grayLevel = Float.parseFloat(elmRgbReplace
						.getAttribute("gray"));
				ColorConv colorType = ColorConv.valueOf(elmRgbReplace
						.getAttribute("replace-type"));
				param.setGrayLevel(grayLevel);
				param.setColorReplace(colorType);
			}

			infoList.add(info);
		}
		return infoList;
	}
}
