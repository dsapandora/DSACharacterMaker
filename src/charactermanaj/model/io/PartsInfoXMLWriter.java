package charactermanaj.model.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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

import charactermanaj.model.PartsAuthorInfo;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.PartsManageData.PartsKey;

public class PartsInfoXMLWriter {

	/**
	 * パーツ定義XMLファイルの名前空間
	 */
	public static final String NS_PARTSDEF = "http://charactermanaj.sourceforge.jp/schema/charactermanaj-partsdef";

	/**
	 * パーツ管理情報をDocBaseと同じフォルダ上のparts-info.xmlに書き出す.<br>
	 * XML生成中に失敗した場合は既存の管理情報は残される.<br>
	 * (管理情報の書き込み中にI/O例外が発生した場合は管理情報は破壊される.)<br>
	 * 
	 * @param docBase
	 *            character.xmlの位置
	 * @param partsManageData
	 *            パーツ管理情報
	 * @throws IOException
	 *             出力に失敗した場合
	 */
	public void savePartsManageData(URI docBase, PartsManageData partsManageData)
			throws IOException {
		if (docBase == null || partsManageData == null) {
			throw new IllegalArgumentException();
		}

		if (!"file".equals(docBase.getScheme())) {
			throw new IOException("ファイル以外はサポートしていません: " + docBase);
		}
		File docBaseFile = new File(docBase);
		File baseDir = docBaseFile.getParentFile();

		// データからXMLを構築してストリームに出力する.
		// 完全に成功したXMLのみ書き込むようにするため、一旦バッファする。
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			savePartsManageData(partsManageData, bos);
		} finally {
			bos.close();
		}

		// バッファされたXMLデータを実際のファイルに書き込む
		File partsInfoXML = new File(baseDir, "parts-info.xml");
		FileOutputStream os = new FileOutputStream(partsInfoXML);
		try {
			os.write(bos.toByteArray());
		} finally {
			os.close();
		}
	}

	/**
	 * パーツ管理情報をXMLとしてストリームに書き出す.<br>
	 * 
	 * @param partsManageData
	 *            パーツ管理データ
	 * @param outstm
	 *            出力先ストリーム
	 * @throws IOException
	 *             出力に失敗した場合
	 */
	public void savePartsManageData(PartsManageData partsManageData,
			OutputStream outstm) throws IOException {
		if (partsManageData == null || outstm == null) {
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

		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		Element root = doc.createElementNS(NS_PARTSDEF, "parts-definition");

		root.setAttribute("xmlns:xml", XMLConstants.XML_NS_URI);
		root.setAttribute("xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute("xsi:schemaLocation", NS_PARTSDEF
				+ " parts-definition.xsd");
		doc.appendChild(root);

		// 作者情報を取得する
		Collection<PartsAuthorInfo> partsAuthors = partsManageData
				.getAuthorInfos();
		for (PartsAuthorInfo partsAuthorInfo : partsAuthors) {
			String author = partsAuthorInfo.getAuthor();
			if (author == null || author.length() == 0) {
				continue;
			}

			// 作者情報の登録
			Element nodeAuthor = doc.createElementNS(NS_PARTSDEF, "author");
			Element nodeAuthorName = doc.createElementNS(NS_PARTSDEF, "name");
			Attr attrLang = doc.createAttributeNS(XMLConstants.XML_NS_URI,
					"lang");
			attrLang.setValue(lang);
			nodeAuthorName.setAttributeNodeNS(attrLang);
			nodeAuthorName.setTextContent(author);
			nodeAuthor.appendChild(nodeAuthorName);

			String homepageURL = partsAuthorInfo.getHomePage();
			if (homepageURL != null && homepageURL.length() > 0) {
				Element nodeHomepage = doc.createElementNS(NS_PARTSDEF,
						"home-page");
				Attr attrHomepageLang = doc.createAttributeNS(
						XMLConstants.XML_NS_URI, "lang");
				attrHomepageLang.setValue(lang);
				nodeHomepage.setAttributeNodeNS(attrHomepageLang);
				nodeHomepage.setTextContent(homepageURL);
				nodeAuthor.appendChild(nodeHomepage);
			}

			root.appendChild(nodeAuthor);

			Collection<PartsKey> partsKeys = partsManageData
					.getPartsKeysByAuthor(author);

			// ダウンロード別にパーツキーの集約
			HashMap<String, List<PartsKey>> downloadMap = new HashMap<String, List<PartsKey>>();
			for (PartsKey partsKey : partsKeys) {
				PartsManageData.PartsVersionInfo versionInfo = partsManageData
						.getVersionStrict(partsKey);
				String downloadURL = versionInfo.getDownloadURL();
				if (downloadURL == null) {
					downloadURL = "";
				}
				List<PartsKey> partsKeyGrp = downloadMap.get(downloadURL);
				if (partsKeyGrp == null) {
					partsKeyGrp = new ArrayList<PartsKey>();
					downloadMap.put(downloadURL, partsKeyGrp);
				}
				partsKeyGrp.add(partsKey);
			}

			// ダウンロード別にパーツ情報の登録
			ArrayList<String> downloadURLs = new ArrayList<String>(
					downloadMap.keySet());
			Collections.sort(downloadURLs);

			// 日時コンバータ
			final SimpleDateFormat dateTimeFmt = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

			for (String downloadURL : downloadURLs) {
				List<PartsKey> partsKeyGrp = downloadMap.get(downloadURL);
				Collections.sort(partsKeyGrp);

				Element nodeDownload = doc.createElementNS(NS_PARTSDEF,
						"download-url");
				nodeDownload.setTextContent(downloadURL);
				root.appendChild(nodeDownload);

				for (PartsKey partsKey : partsKeyGrp) {
					PartsManageData.PartsVersionInfo versionInfo = partsManageData
							.getVersionStrict(partsKey);

					Element nodeParts = doc.createElementNS(NS_PARTSDEF,
							"parts");

					nodeParts.setAttribute("name", partsKey.getPartsName());
					if (partsKey.getCategoryId() != null) {
						nodeParts.setAttribute("category",
								partsKey.getCategoryId());
					}
					if (versionInfo.getVersion() > 0) {
						nodeParts.setAttribute("version",
								Double.toString(versionInfo.getVersion()));
					}

					if (versionInfo.getLastModified() != null) {
						nodeParts.setAttribute("lastModified", dateTimeFmt
								.format(versionInfo.getLastModified()));
					}

					String localizedName = partsManageData
							.getLocalizedName(partsKey);
					if (localizedName != null
							&& localizedName.trim().length() > 0) {
						Element nodeLocalizedName = doc.createElementNS(
								NS_PARTSDEF, "local-name");
						Attr attrLocalizedNameLang = doc.createAttributeNS(
								XMLConstants.XML_NS_URI, "lang");
						attrLocalizedNameLang.setValue(lang);
						nodeLocalizedName
								.setAttributeNodeNS(attrLocalizedNameLang);
						nodeLocalizedName.setTextContent(localizedName);
						nodeParts.appendChild(nodeLocalizedName);
					}

					root.appendChild(nodeParts);
				}
			}
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
