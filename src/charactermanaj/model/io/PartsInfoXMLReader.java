package charactermanaj.model.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import charactermanaj.model.PartsAuthorInfo;
import charactermanaj.model.PartsManageData;

/**
 * パーツ管理情報のXMLの読み込み用クラス.
 * 
 * @author seraphy
 */
public class PartsInfoXMLReader {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(PartsInfoXMLReader.class.getName());

	/**
	 * 指定したDocBaseと同じフォルダにあるparts-info.xmlからパーツ管理情報を取得して返す.<br>
	 * ファイルが存在しない場合は空のインスタンスを返す.<br>
	 * 返されるインスタンスは編集可能です.<br>
	 * 
	 * @param docBase
	 *            character.xmlの位置
	 * @return パーツ管理情報、存在しない場合は空のインスタンス
	 * @throws IOException
	 *             読み込み中に失敗した場合
	 */
	public PartsManageData loadPartsManageData(URI docBase) throws IOException {
		if (docBase == null) {
			throw new IllegalArgumentException();
		}
		if (!"file".equals(docBase.getScheme())) {
			throw new IOException("ファイル以外はサポートしていません。:" + docBase);
		}
		File docBaseFile = new File(docBase);
		File baseDir = docBaseFile.getParentFile();

		// パーツ管理情報ファイルの確認
		final File partsInfoXML = new File(baseDir, "parts-info.xml");
		if (!partsInfoXML.exists()) {
			// ファイルが存在しなければ空を返す.
			return new PartsManageData();
		}

		PartsManageData partsManageData;
		InputStream is = new FileInputStream(partsInfoXML);
		try {
			partsManageData = loadPartsManageData(is);
		} finally {
			is.close();
		}
		return partsManageData;
	}

	public PartsManageData loadPartsManageData(InputStream is)
			throws IOException {
		if (is == null) {
			throw new IllegalArgumentException();
		}

		// パーツ管理情報
		final PartsManageData partsManageData = new PartsManageData();

		// SAXParserの準備
		SAXParser saxParser;
		try {
			SAXParserFactory saxPartserFactory = SAXParserFactory.newInstance();
			saxPartserFactory.setNamespaceAware(true);
			saxParser = saxPartserFactory.newSAXParser();
		} catch (Exception ex) {
			throw new RuntimeException("JAXP Configuration failed.", ex);
		}

		// デフォルトのロケールから言語を取得
		final Locale locale = Locale.getDefault();
		final String lang = locale.getLanguage();

		try {
			// 要素のスタック
			final LinkedList<String> stack = new LinkedList<String>();

			// 日時コンバータ
			final SimpleDateFormat dateTimeFmt = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

			// DOMではなくSAXで読み流す.
			saxParser.parse(is, new DefaultHandler() {
				private StringBuilder buf = new StringBuilder();

				private PartsAuthorInfo partsAuthorInfo;

				private String authorName;
				private String homepageURL;
				private String authorNameLang;
				private String homepageLang;
				private String downloadURL;

				private String partsLocalNameLang;
				private String partsLocalName;
				private String partsCategoryId;
				private String partsName;
				private double partsVersion;

				private Timestamp partsLastModified;

				@Override
				public void startDocument() throws SAXException {
					logger.log(Level.FINEST, "parts-info : start");
				}

				@Override
				public void endDocument() throws SAXException {
					logger.log(Level.FINEST, "parts-info : end");
				}

				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {
					buf.append(ch, start, length);
				}
				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					stack.addFirst(qName);
					int mx = stack.size();
					if (mx >= 2 && stack.get(1).equals("parts")) {
						if ("local-name".equals(qName)) {
							partsLocalNameLang = attributes.getValue(
									XMLConstants.XML_NS_URI, "lang");
						}

					} else if (mx >= 2 && stack.get(1).equals("author")) {
						if ("name".equals(qName)) {
							authorNameLang = attributes.getValue(
									XMLConstants.XML_NS_URI, "lang");

						} else if ("home-page".equals(qName)) {
							homepageLang = attributes.getValue(
									XMLConstants.XML_NS_URI, "lang");
						}

					} else if ("author".equals(qName)) {
						partsAuthorInfo = null;
						authorName = null;
						authorNameLang = null;
						homepageURL = null;
						homepageLang = null;

					} else if ("download-url".equals(qName)) {
						downloadURL = null;

					} else if ("parts".equals(qName)) {
						partsLocalName = null;
						partsLocalNameLang = null;
						partsCategoryId = attributes.getValue("category");
						partsName = attributes.getValue("name");

						// バージョン
						String strVersion = attributes.getValue("version");
						try {
							if (strVersion == null || strVersion.length() == 0) {
								partsVersion = 0.;

							} else {
								partsVersion = Double.parseDouble(strVersion);
								if (partsVersion < 0) {
									partsVersion = 0;
								}
							}

						} catch (Exception ex) {
							logger.log(Level.INFO,
									"parts-info.xml: invalid version."
											+ strVersion);
							partsVersion = 0;
						}

						// 更新日時
						String strLastModified = attributes
								.getValue("lastModified");
						if (strLastModified != null
								&& strLastModified.trim().length() > 0) {
							try {
								partsLastModified = new Timestamp(dateTimeFmt
										.parse(strLastModified.trim())
										.getTime());

							} catch (Exception ex) {
								logger.log(Level.INFO,
										"parts-info.xml: invalid dateTime."
												+ strLastModified);
							}
						}
					}

					buf = new StringBuilder();
				}
				@Override
				public void endElement(String uri, String localName,
						String qName) throws SAXException {

					int mx = stack.size();

					if (mx >= 2 && "parts".equals(stack.get(1))) {
						if ("local-name".equals(qName)) {
							if (partsLocalName == null
									|| lang.equals(partsLocalNameLang)) {
								partsLocalName = buf.toString();
							}
						}

					} else if (mx >= 2 && "author".equals(stack.get(1))) {
						if ("name".equals(qName)) {
							if (authorName == null
									|| lang.equals(authorNameLang)) {
								authorName = buf.toString();
							}

						} else if ("home-page".equals(qName)) {
							if (homepageURL == null
									|| lang.equals(homepageLang)) {
								homepageURL = buf.toString();
							}
						}

					} else if ("author".equals(qName)) {
						logger.log(Level.FINE, "parts-info: author: "
								+ authorName + " /homepage:" + homepageURL);
						if (authorName != null && authorName.length() > 0) {
							partsAuthorInfo = new PartsAuthorInfo();
							partsAuthorInfo.setAuthor(authorName);
							partsAuthorInfo.setHomePage(homepageURL);

						} else {
							partsAuthorInfo = null;
						}

					} else if ("download-url".equals(qName)) {
						downloadURL = buf.toString();
						logger.log(Level.FINE, "parts-info: download-url: "
								+ downloadURL);

					} else if ("parts".equals(qName)) {
						if (logger.isLoggable(Level.FINE)) {
							logger.log(Level.FINE,
									"parts-info.xml: parts-name: " + partsName
											+ " /category: " + partsCategoryId
											+ " /parts-local-name: "
											+ partsLocalName + " /version:"
											+ partsVersion + "/lastModified:"
											+ partsLastModified);
						}

						PartsManageData.PartsVersionInfo versionInfo = new PartsManageData.PartsVersionInfo();
						versionInfo.setVersion(partsVersion);
						versionInfo.setDownloadURL(downloadURL);
						versionInfo.setLastModified(partsLastModified);

						PartsManageData.PartsKey partsKey = new PartsManageData.PartsKey(
								partsName, partsCategoryId);

						partsManageData.putPartsInfo(partsKey, partsLocalName,
								partsAuthorInfo, versionInfo);

					}
					stack.removeFirst();
				}
			});

		} catch (SAXException ex) {
			IOException ex2 = new IOException("parts-info.xml read failed.");
			ex2.initCause(ex);
			throw ex2;
		}

		return partsManageData;
	}
}
