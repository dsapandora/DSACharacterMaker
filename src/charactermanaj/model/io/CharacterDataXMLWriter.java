package charactermanaj.model.io;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import charactermanaj.graphics.filters.ColorConv;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.model.CharacterData;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.ColorInfo;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.RecommendationURL;

/**
 * パーツ管理情報のXMLへの書き込み用クラス.
 * 
 * @author seraphy
 */
public class CharacterDataXMLWriter {

	/**
	 * キャラクター定義バージョン
	 */
	private static final String VERSION_SIG_1_0 = "1.0";

	/**
	 * キャラクター定義XMLファイルの名前空間
	 */
	private static final String DEFAULT_NS = "http://charactermanaj.sourceforge.jp/schema/charactermanaj";

	private final String NS;
	
	public CharacterDataXMLWriter() {
		this(DEFAULT_NS);
	}
	
	public CharacterDataXMLWriter(String ns) {
		this.NS = ns;
	}
	
	/**
	 * キャラクターデータのXMLの書き込み.
	 * 
	 * @param characterData
	 * @param outstm
	 * @throws IOException
	 */
	public void writeXMLCharacterData(CharacterData characterData,
			OutputStream outstm) throws IOException {
		if (outstm == null || characterData == null) {
			throw new IllegalArgumentException();
		}

		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration failed.", ex);
		}

		Element root = doc.createElementNS(NS, "character");
		root.setAttribute("version", VERSION_SIG_1_0);

		root.setAttribute("xmlns:xml", XMLConstants.XML_NS_URI);
		root.setAttribute("xmlns:xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
		root.setAttribute("xsi:schemaLocation", NS + " character.xsd");
		root.setAttribute("id", characterData.getId());
		root.setAttribute("rev", characterData.getRev());
		doc.appendChild(root);

		// name
		Element nodeName = doc.createElementNS(NS, "name");
		Attr attrLang = doc.createAttributeNS(XMLConstants.XML_NS_URI, "lang");
		attrLang.setValue(lang);
		nodeName.setAttributeNodeNS(attrLang);
		nodeName.setTextContent(characterData.getName());
		root.appendChild(nodeName);

		// information
		String author = characterData.getAuthor();
		String description = characterData.getDescription();
		if ((author != null && author.length() > 0)
				|| (description != null && description.length() > 0)) {
			Element nodeInfomation = doc.createElementNS(NS, "information");
			if (author != null && author.length() > 0) {
				Element nodeAuthor = doc.createElementNS(NS, "author");
				Attr attrNodeAuthorLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrNodeAuthorLang.setValue(lang);
				nodeAuthor.setAttributeNodeNS(attrNodeAuthorLang);
				nodeAuthor.setTextContent(author);
				nodeInfomation.appendChild(nodeAuthor);
			}
			if (description != null && description.length() > 0) {

				// 説明の改行コードはXML上ではLFとする.
				description = description.replace("\r\n", "\n");
				description = description.replace("\r", "\n");

				Element nodeDescription = doc
						.createElementNS(NS, "description");
				Attr attrNodeDescriptionLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrNodeDescriptionLang.setValue(lang);
				nodeDescription.setAttributeNodeNS(attrNodeDescriptionLang);
				nodeDescription.setTextContent(description);
				nodeInfomation.appendChild(nodeDescription);
			}
			root.appendChild(nodeInfomation);
		}

		// size
		Element nodeSize = doc.createElementNS(NS, "image-size");
		Element nodeWidth = doc.createElementNS(NS, "width");
		nodeWidth.setTextContent(Integer.toString((int) characterData
				.getImageSize().getWidth()));
		Element nodeHeight = doc.createElementNS(NS, "height");
		nodeHeight.setTextContent(Integer.toString((int) characterData
				.getImageSize().getHeight()));
		nodeSize.appendChild(nodeWidth);
		nodeSize.appendChild(nodeHeight);
		root.appendChild(nodeSize);

		// settings
		Element nodeSettings = doc.createElementNS(NS, "settings");
		root.appendChild(nodeSettings);
		for (String settingsEntryName : characterData.getPropertyNames()) {
			String value = characterData.getProperty(settingsEntryName);
			if (value != null) {
				Element nodeEntry = doc.createElementNS(NS, "entry");
				nodeEntry.setAttribute("key", settingsEntryName);
				nodeEntry.setTextContent(value);
				nodeSettings.appendChild(nodeEntry);
			}
		}

		// categories
		Element nodeCategories = doc.createElementNS(NS, "categories");
		for (PartsCategory category : characterData.getPartsCategories()) {
			// category
			Element nodeCategory = doc.createElementNS(NS, "category");
			nodeCategory.setAttribute("id", category.getCategoryId());
			nodeCategory.setAttribute("multipleSelectable",
					category.isMultipleSelectable() ? "true" : "false");

			// visible-rows
			Element nodeVisibleRows = doc.createElementNS(NS, "visible-rows");
			nodeVisibleRows.setTextContent(Integer.toString(category
					.getVisibleRows()));
			nodeCategory.appendChild(nodeVisibleRows);

			// category name
			Element nodeCategoryName = doc.createElementNS(NS, "display-name");
			Attr attrCategoryNameLang = doc.createAttributeNS(
					XMLConstants.XML_NS_URI, "lang");
			attrCategoryNameLang.setValue(lang);
			nodeCategoryName.setAttributeNodeNS(attrCategoryNameLang);
			nodeCategoryName
					.setTextContent(category.getLocalizedCategoryName());
			nodeCategory.appendChild(nodeCategoryName);

			// layers
			Element nodeLayers = doc.createElementNS(NS, "layers");
			for (Layer layer : category.getLayers()) {
				// layer
				Element nodeLayer = doc.createElementNS(NS, "layer");
				nodeLayer.setAttribute("id", layer.getId());

				Element nodeLayerName = doc.createElementNS(NS, "display-name");
				Attr attrLayerNameLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrLayerNameLang.setValue(lang);
				nodeLayerName.setAttributeNodeNS(attrLayerNameLang);
				nodeLayerName.setTextContent(layer.getLocalizedName());
				nodeLayer.appendChild(nodeLayerName);

				Element nodeOrder = doc.createElementNS(NS, "order");
				nodeOrder.setTextContent(Integer.toString(layer.getOrder()));
				nodeLayer.appendChild(nodeOrder);

				ColorGroup colorGroup = layer.getColorGroup();
				if (colorGroup != null && colorGroup.isEnabled()) {
					Element nodeColorGroup = doc.createElementNS(NS,
							"colorGroup");
					nodeColorGroup.setAttribute("refid", colorGroup.getId());
					nodeColorGroup.setAttribute("init-sync", layer.isInitSync()
							? "true"
							: "false");
					nodeLayer.appendChild(nodeColorGroup);
				}

				Element nodeDir = doc.createElementNS(NS, "dir");
				nodeDir.setTextContent(layer.getDir());
				nodeLayer.appendChild(nodeDir);

				String colorModelName = layer.getColorModelName();
				if (colorModelName != null && colorModelName.length() > 0) {
					Element nodeColorModel = doc.createElementNS(NS,
							"colorModel");
					nodeColorModel.setTextContent(layer.getColorModelName());
					nodeLayer.appendChild(nodeColorModel);
				}

				nodeLayers.appendChild(nodeLayer);
			}
			nodeCategory.appendChild(nodeLayers);

			nodeCategories.appendChild(nodeCategory);
		}
		root.appendChild(nodeCategories);

		// ColorGroupを構築する
		Collection<ColorGroup> colorGroups = characterData.getColorGroups();
		if (colorGroups.size() > 0) {
			Element nodeColorGroups = doc.createElementNS(NS, "colorGroups");
			int colorGroupCount = 0;
			for (ColorGroup colorGroup : colorGroups) {
				if (!colorGroup.isEnabled()) {
					continue;
				}
				Element nodeColorGroup = doc.createElementNS(NS, "colorGroup");
				nodeColorGroup.setAttribute("id", colorGroup.getId());
				Element nodeColorGroupName = doc.createElementNS(NS,
						"display-name");
				Attr attrColorGroupNameLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrColorGroupNameLang.setValue(lang);
				nodeColorGroupName.setAttributeNodeNS(attrColorGroupNameLang);
				nodeColorGroupName
						.setTextContent(colorGroup.getLocalizedName());
				nodeColorGroup.appendChild(nodeColorGroupName);
				nodeColorGroups.appendChild(nodeColorGroup);
				colorGroupCount++;
			}
			if (colorGroupCount > 0) {
				root.appendChild(nodeColorGroups);
			}
		}

		// Recommendations
		List<RecommendationURL> recommendations = characterData
				.getRecommendationURLList();
		if (recommendations != null) {
			Element nodeRecommendations = doc.createElementNS(NS,
					"recommendations");
			for (RecommendationURL recommendation : recommendations) {
				Element nodeRecommendation = doc.createElementNS(NS,
						"recommendation");
				String displayName = recommendation.getDisplayName();
				String url = recommendation.getUrl();

				Element nodeDescription = doc
						.createElementNS(NS, "description");
				Attr attrRecommendationDescriptionLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrRecommendationDescriptionLang.setValue(lang);
				nodeDescription
						.setAttributeNodeNS(attrRecommendationDescriptionLang);
				nodeDescription.setTextContent(displayName);

				Element nodeURL = doc.createElementNS(NS, "URL");
				Attr attrRecommendationURLLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrRecommendationURLLang.setValue(lang);
				nodeURL.setAttributeNodeNS(attrRecommendationURLLang);
				nodeURL.setTextContent(url);

				nodeRecommendation.appendChild(nodeDescription);
				nodeRecommendation.appendChild(nodeURL);

				nodeRecommendations.appendChild(nodeRecommendation);
			}
			root.appendChild(nodeRecommendations);
		}

		// presetsのelementを構築する.
		Element nodePresets = doc.createElementNS(NS, "presets");
		if (writePartsSetElements(doc, nodePresets, characterData, true, false) > 0) {
			root.appendChild(nodePresets);
		}

		// output xml
		TransformerFactory txFactory = TransformerFactory.newInstance();
		txFactory.setAttribute("indent-number", Integer.valueOf(4));
		Transformer tfmr;
		try {
			tfmr = txFactory.newTransformer();
		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration Failed.", ex);
		}
		tfmr.setOutputProperty(OutputKeys.INDENT, "yes");

		// JDK-4504745 : javax.xml.transform.Transformer encoding does not work properly
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504745
		final String encoding = "UTF-8";
		tfmr.setOutputProperty("encoding", encoding);
		try {
			tfmr.transform(new DOMSource(doc), new StreamResult(
					new OutputStreamWriter(outstm, Charset.forName(encoding))));

		} catch (TransformerException ex) {
			IOException ex2 = new IOException("XML Convert failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}

	/**
	 * キャラクターデータ内のPresetおよびFavotiesのPartssetの双方共通のパーツセット要素のリストを構築する.
	 * 
	 * @param doc
	 *            ドキュメントオブジェクト(createElementNS用)
	 * @param baseElement
	 *            親要素、キャラクターデータの場合はPreset、Favoritesの場合はPartssetを示す要素
	 * @param characterData
	 *            キャラクターデータ
	 * @param writePresets
	 *            Preset属性のパーツセットを登録する場合はtrue、Preset属性時はデフォルトプリセット属性も(あれば)登録される
	 * @param writeFavorites
	 *            Preset属性のないパーツセットを登録する場合はtrue
	 * @return 登録したパーツセットの個数
	 */
	protected int writePartsSetElements(Document doc, Element baseElement,
			CharacterData characterData, boolean writePresets,
			boolean writeFavorites) {
		Map<String, PartsSet> partsSetMap = characterData.getPartsSets();

		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		HashMap<String, PartsSet> registeredPartsSetMap = new HashMap<String, PartsSet>();

		for (Map.Entry<String, PartsSet> partsSetsEntry : partsSetMap
				.entrySet()) {
			PartsSet partsSet = partsSetsEntry.getValue();
			if (partsSet.isPresetParts() && !writePresets) {
				continue;
			}
			if (!partsSet.isPresetParts() && !writeFavorites) {
				continue;
			}

			if (partsSet.isEmpty()) {
				// 空のパーツセットは登録しない.
				continue;
			}

			Element nodePreset = createPartsSetXML(doc, lang, partsSet);
			baseElement.appendChild(nodePreset);
			registeredPartsSetMap.put(partsSet.getPartsSetId(), partsSet);
		}

		// プリセット登録時はデフォルトのプリセットIDがあれば、それも登録する.
		// (ただし、該当パーツセットが書き込み済みである場合のみ)
		if (writePresets) {
			String defaultPresetId = characterData.getDefaultPartsSetId();
			if (defaultPresetId != null && defaultPresetId.length() > 0) {
				PartsSet defaultPartsSet = registeredPartsSetMap
						.get(defaultPresetId);
				if (defaultPartsSet != null && defaultPartsSet.isPresetParts()) {
					baseElement.setAttribute("default-preset", defaultPresetId);
				}
			}
		}

		return registeredPartsSetMap.size();
	}
	
	/**
	 * パーツセットのXM要素を生成して返す.
	 * 
	 * @param doc
	 *            ノードを生成するためのファクトリ
	 * @param lang
	 *            言語識別用(パーツセット名などの登録時のlang属性に必要)
	 * @param partsSet
	 *            パーツセット、nullの場合はxsi:nul="true"が返される.
	 * @return パーツセット1つ分のXML要素
	 */
	public Element createPartsSetXML(Document doc, String lang, PartsSet partsSet) {
		if (doc == null || lang == null) {
			throw new IllegalArgumentException();
		}

		String partsSetId = partsSet.getPartsSetId();
		String localizedName = partsSet.getLocalizedName();

		Element nodePreset = doc.createElementNS(NS, "preset");
		if (partsSet == null || partsSet.isEmpty()) {
			// 指定されていないか有効でない場合は無しとみなす.
			nodePreset.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			return nodePreset;
		}

		nodePreset.setAttribute("id", partsSetId);

		// display-name
		Element nodeName = doc.createElementNS(NS, "display-name");
		Attr attrLang = doc.createAttributeNS(XMLConstants.XML_NS_URI,
				"lang");
		attrLang.setValue(lang);
		nodeName.setAttributeNode(attrLang);
		nodeName.setTextContent(localizedName);
		nodePreset.appendChild(nodeName);

		// bgColor
		Color bgColor = partsSet.getBgColor();
		if (bgColor != null) {
			Element nodeBgColor = doc.createElementNS(NS,
					"background-color");
			nodeBgColor.setAttribute("color",
					"#" + Integer.toHexString(bgColor.getRGB() & 0xffffff));
			nodePreset.appendChild(nodeBgColor);
		}

		// affine transform parameter
		double[] affineTransformParameter = partsSet
				.getAffineTransformParameter();
		if (affineTransformParameter != null) {
			Element nodeAffineTransform = doc.createElementNS(NS,
					"affine-transform-parameter");
			StringBuilder tmp = new StringBuilder();
			for (double affineItem : affineTransformParameter) {
				if (tmp.length() > 0) {
					tmp.append(" ");
				}
				tmp.append(Double.toString(affineItem));
			}
			nodeAffineTransform.setTextContent(tmp.toString());
			nodePreset.appendChild(nodeAffineTransform);
		}

		// categories
		for (Map.Entry<PartsCategory, List<PartsIdentifier>> entry : partsSet
				.entrySet()) {
			PartsCategory partsCategory = entry.getKey();

			// category
			Element nodeCategory = doc.createElementNS(NS, "category");
			nodeCategory.setAttribute("refid",
					partsCategory.getCategoryId());
			nodePreset.appendChild(nodeCategory);

			List<PartsIdentifier> partsIdentifiers = entry.getValue();
			for (PartsIdentifier partsIdentifier : partsIdentifiers) {
				String partsName = partsIdentifier.getPartsName();
				Element nodeParts = doc.createElementNS(NS, "parts");
				nodeParts.setAttribute("name", partsName);
				nodeCategory.appendChild(nodeParts);

				PartsColorInfo partsColorInfo = partsSet
						.getColorInfo(partsIdentifier);
				if (partsColorInfo != null) {
					Element nodeColor = createPartsColorInfoXML(doc, partsColorInfo);
					nodeParts.appendChild(nodeColor);
				}
			}
		}
		
		return nodePreset;
	}
	
	/**
	 * パーツカラー情報のXML要素を生成して返す.<br>
	 * 
	 * @param doc
	 *            要素を作成するためのファクトリ
	 * @param partsColorInfo
	 *            パーツカラー情報
	 * @return パーツカラー情報の要素
	 */
	public Element createPartsColorInfoXML(Document doc, PartsColorInfo partsColorInfo) {
		if (doc == null || partsColorInfo == null) {
			throw new IllegalArgumentException();
		}

		Element nodeColor = doc.createElementNS(NS, "color");

		for (Map.Entry<Layer, ColorInfo> colorInfoEntry : partsColorInfo
				.entrySet()) {
			Layer layer = colorInfoEntry.getKey();
			ColorInfo colorInfo = colorInfoEntry.getValue();

			Element nodeLayer = doc
					.createElementNS(NS, "layer");
			nodeLayer.setAttribute("refid", layer.getId());
			nodeColor.appendChild(nodeLayer);

			// ColorGroup
			ColorGroup colorGroup = colorInfo.getColorGroup();
			boolean colorSync = colorInfo.isSyncColorGroup();

			if (colorGroup.isEnabled()) {
				Element nodeColorGroup = doc.createElementNS(
						NS, "color-group");
				nodeColorGroup.setAttribute("group",
						colorGroup.getId());
				nodeColorGroup.setAttribute("synchronized",
						colorSync ? "true" : "false");
				nodeLayer.appendChild(nodeColorGroup);
			}

			// RGB
			ColorConvertParameter param = colorInfo
					.getColorParameter();

			Element nodeRGB = doc.createElementNS(NS, "rgb");
			Object[][] rgbArgss = {
					{"red", param.getOffsetR(),
							param.getFactorR(),
							param.getGammaR()},
					{"green", param.getOffsetG(),
							param.getFactorG(),
							param.getGammaG()},
					{"blue", param.getOffsetB(),
							param.getFactorB(),
							param.getGammaB()},
					{"alpha", param.getOffsetA(),
							param.getFactorA(),
							param.getGammaA()},};
			for (Object[] rgbArgs : rgbArgss) {
				Element nodeRGBItem = doc.createElementNS(NS,
						rgbArgs[0].toString());
				nodeRGBItem.setAttribute("offset",
						rgbArgs[1].toString());
				nodeRGBItem.setAttribute("factor",
						rgbArgs[2].toString());
				nodeRGBItem.setAttribute("gamma",
						rgbArgs[3].toString());
				nodeRGB.appendChild(nodeRGBItem);
			}
			nodeLayer.appendChild(nodeRGB);

			// HSB
			Element nodeHSB = doc.createElementNS(NS, "hsb");
			nodeHSB.setAttribute("hue",
					Float.toString(param.getHue()));
			nodeHSB.setAttribute("saturation",
					Float.toString(param.getSaturation()));
			nodeHSB.setAttribute("brightness",
					Float.toString(param.getBrightness()));
			if (param.getContrast() != 0.f) {
				// ver0.96追加、optional
				// ぴったり0.0fだったら省略する.
				nodeHSB.setAttribute("contrast",
						Float.toString(param.getContrast()));
			}
			nodeLayer.appendChild(nodeHSB);

			// RGB Replace
			Element nodeRGBReplace = doc.createElementNS(NS,
					"rgb-replace");
			ColorConv colorConv = param.getColorReplace();
			if (colorConv == null) {
				colorConv = ColorConv.NONE;
			}
			nodeRGBReplace.setAttribute("replace-type",
					colorConv.name());
			nodeRGBReplace.setAttribute("gray",
					Float.toString(param.getGrayLevel()));
			nodeLayer.appendChild(nodeRGBReplace);
		}
		
		return nodeColor;
	}

	public void saveFavorites(CharacterData characterData,
			OutputStream outstm) throws IOException {
		if (characterData == null || outstm == null) {
			throw new IllegalArgumentException();
		}

		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();

		} catch (ParserConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration Exception.", ex);
		}

		Element root = doc.createElementNS(NS, "partssets");

		root.setAttribute("xmlns:xml", XMLConstants.XML_NS_URI);
		root.setAttribute("xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute("xsi:schemaLocation", NS + " partsset.xsd");
		doc.appendChild(root);

		// presetsのelementを構築する.(Presetは除く)
		writePartsSetElements(doc, root, characterData, false, true);

		// output xml
		TransformerFactory txFactory = TransformerFactory.newInstance();
		txFactory.setAttribute("indent-number", Integer.valueOf(4));
		Transformer tfmr;
		try {
			tfmr = txFactory.newTransformer();
		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration Failed.", ex);
		}
		tfmr.setOutputProperty(OutputKeys.INDENT, "yes");

		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504745
		final String encoding = "UTF-8";
		tfmr.setOutputProperty("encoding", encoding);
		try {
			tfmr.transform(new DOMSource(doc), new StreamResult(
					new OutputStreamWriter(outstm, Charset.forName(encoding))));

		} catch (TransformerException ex) {
			IOException ex2 = new IOException("XML Convert failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
}
