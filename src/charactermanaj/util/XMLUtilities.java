package charactermanaj.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XML用のユーテリティ.<br>
 * 
 * @author seraphy
 */
public final class XMLUtilities {

	/**
	 * プライベートコンストラクタ
	 */
	private XMLUtilities() {
		super();
	}

	/**
	 * XMLドキュメントをロードして返します. 名前空間を有効とします.
	 * 
	 * @param is
	 *            ロードするXMLドキュメントの入力ストリーム
	 * @return ドキュメント
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public static Document loadDocument(InputStream is) throws IOException {
		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);

			DocumentBuilder builder = factory.newDocumentBuilder();
			final ArrayList<SAXParseException> errors = new ArrayList<SAXParseException>();
			builder.setErrorHandler(new ErrorHandler() {
				public void error(SAXParseException exception)
						throws SAXException {
					errors.add(exception);
				}
				public void fatalError(SAXParseException exception)
						throws SAXException {
					errors.add(exception);
				}
				public void warning(SAXParseException exception)
						throws SAXException {
					errors.add(exception);
				}
			});

			doc = builder.parse(is);
			if (errors.size() > 0) {
				throw errors.get(0);
			}

			return doc;

		} catch (ParserConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration Exception.", ex);

		} catch (SAXException ex) {
			IOException ex2 = new IOException("xml read failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}

	/**
	 * 指定した名前の子要素で、lang属性が一致するものの値を返す.<br>
	 * langが一致するものがない場合は最初の要素の値を返す.<br>
	 * 一つも要素がない場合はnullを返す.
	 * 
	 * @param parent
	 *            親要素
	 * @param elementName
	 *            子要素の名前
	 * @param lang
	 *            言語属性
	 * @return 言語属性が一致する子要素の値、もしくは最初の子要素の値、もしくはnull
	 */
	public static String getLocalizedElementText(Element parent,
			String elementName, String lang) {
		String text = null;
		for (Element childelm : getChildElements(parent, elementName)) {
			String val = childelm.getTextContent();

			// 最初の定義をデフォルト値として用いる.
			if (text == null) {
				text = val;
			}

			// lang指定が一致すれば、それを優先する.
			String langNm = childelm.getAttributeNS(
					"http://www.w3.org/XML/1998/namespace", "lang");
			if (lang.equals(langNm) && val.length() > 0) {
				text = val;
				break;
			}
		}
		return text;
	}

	/**
	 * 指定した名前の子要素の最初の値を返す.
	 * 
	 * @param parent
	 *            親要素
	 * @param elementName
	 *            子要素の名前
	 * @return 値、要素がなければnull
	 */
	public static String getElementText(Element parent, String elementName) {
		for (Element childelm : getChildElements(parent, elementName)) {
			return childelm.getTextContent();
		}
		return null;
	}

	/**
	 * 指定した名前の最初の子要素を返す. なければnull.
	 * 
	 * @param parent
	 *            親要素
	 * @param name
	 *            子要素の名前
	 * @return 最初の子要素、もしくはnull
	 */
	public static Element getFirstChildElement(Element parent, final String name) {
		for (Element elm : getChildElements(parent, name)) {
			return elm;
		}
		return null;
	}

	/**
	 * 指定した名前の子要素の列挙子を返す. nullの場合は、すべての子要素を返す.
	 * 
	 * @param elm
	 *            親要素
	 * @param name
	 *            子要素の名前、もしくはnull
	 * @return 子要素の列挙子、該当がない場合は空の列挙子が返される
	 */
	public static Iterable<Element> getChildElements(Element elm,
			final String name) {
		return iterable(elm.getChildNodes(), new Filter() {
			public boolean isAccept(Node node) {
				if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
					if (name == null || name.equals(node.getNodeName())) {
						return true;
					}
				}
				return false;
			}
		});
	}

	/**
	 * すべての子ノードを列挙子として返す
	 * 
	 * @param nodeList
	 * @return
	 */
	public static <T extends Node> Iterable<T> iterable(final NodeList nodeList) {
		return iterable(nodeList, null);
	}

	/**
	 * フィルタ
	 */
	public interface Filter {
		boolean isAccept(Node node);
	}

	/**
	 * 指定したノードリストからフィルタ条件にマッチするものだけを列挙子として返す.
	 * 
	 * @param nodeList
	 *            ノードリスト、nullの場合は空とみなす
	 * @param filter
	 *            フィルタ条件、nullの場合はすべて合致とみなす
	 * @return 合致するものだけを列挙する列挙子
	 */
	public static <T extends Node> Iterable<T> iterable(
			final NodeList nodeList, final Filter filter) {
		final int mx;
		if (nodeList == null) {
			mx = 0;
		} else {
			mx = nodeList.getLength();
		}
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private int idx = 0;

					private Node nextNode = getNextNode();

					private Node getNextNode() {
						while (idx < mx) {
							Node node = nodeList.item(idx++);
							if (filter == null || filter.isAccept(node)) {
								return node;
							}
						}
						return null;
					}

					public boolean hasNext() {
						return nextNode != null;
					}

					@SuppressWarnings("unchecked")
					public T next() {
						Node cur = nextNode;
						if (cur == null) {
							throw new NoSuchElementException();
						}
						nextNode = getNextNode();
						return (T) cur;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}
