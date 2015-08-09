package charactermanaj.model.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;
import charactermanaj.util.ConfigurationDirUtilities;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.ResourceLoader;
import charactermanaj.util.ResourceNames;
import charactermanaj.util.SetupLocalization;

/**
 * デフォルトキャラクターセットのプロバイダ
 * 
 * @author seraphy
 */
public class CharacterDataDefaultProvider {

	/**
	 * リソースに格納されているデフォルトのキャラクター定義のリソースパスまでのプレフィックス.<br>
	 */
	public static final String DEFAULT_CHARACTER_PREFIX = "template/";

	/**
	 * テンプレートをリストしているXML形式のプロパティファイル名
	 */
	public static final String TEMPLATE_LIST_XML = "characterDataTemplates";

	/**
	 * デフォルトのキャラクターセット名(ver2)
	 */
	public static final String DEFAULT_CHARACTER_NAME_V2 = "character2.xml";

	/**
	 * デフォルトのキャラクターセット名(ver3)
	 */
	public static final String DEFAULT_CHARACTER_NAME_V3 = "character3.xml";

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(CharacterDataDefaultProvider.class.getName());

	public enum DefaultCharacterDataVersion {
		V2() {
			public String getResourceName() {
				return DEFAULT_CHARACTER_NAME_V2;
			}
		},
		V3() {
			public String getResourceName() {
				return DEFAULT_CHARACTER_NAME_V3;
			}
		};

		public abstract String getResourceName();

		public CharacterData create(CharacterDataDefaultProvider prov) {
			if (prov == null) {
				throw new IllegalArgumentException();
			}
			try {
				return prov.loadPredefinedCharacterData(getResourceName());

			} catch (IOException ex) {
				throw new RuntimeException(
						"can not create the default profile from application's resource",
						ex);
			}
		}
	}

	/**
	 * デフォルトのキャラクター定義を生成して返す.<br>
	 * 一度生成された場合はキャッシュされる.<br>
	 * 生成されたキャラクター定義のdocBaseはnullであるため、docBaseをセットすること.<br>
	 * 
	 * @param version
	 *            デフォルトキャラクターセットのバージョン
	 * @return キャラクター定義
	 */
	public synchronized CharacterData createDefaultCharacterData(
			DefaultCharacterDataVersion version) {
		if (version == null) {
			throw new IllegalArgumentException();
		}
		return version.create(this);
	}

	/**
	 * テンプレートリストの定義プロパティを読み込む.<br>
	 * neutral引数がfalseの場合は現在のロケールを優先する.<br>
	 * 引数がtrueの場合は読み込み順を逆転させ、言語中立を優先する.<br>
	 * 
	 * @param neutral
	 *            言語中立を優先する場合
	 * @return テンプレートリストのプロパティ
	 */
	private Properties getTemplateListProperties(boolean neutral) {
		// テンプレートリソースは実行中に増減する可能性があるため、
		// 共有キャッシュには入れない.
		LocalizedResourcePropertyLoader loader = new LocalizedResourcePropertyLoader(
				null);
		String name = DEFAULT_CHARACTER_PREFIX + TEMPLATE_LIST_XML;
		ResourceNames resNames = LocalizedResourcePropertyLoader
				.getResourceNames(name, null);
		if (neutral) {
			// 言語中立を優先する場合は、プロパティの重ね順を逆転させて言語中立で最後に上書きする.
			resNames = resNames.reverse();
		}
		return loader.getLocalizedProperties(name);
	}

	/**
	 * キャラクターデータのxmlファイル名をキーとし、表示名を値とするマップ.<br>
	 * 表示順序でアクセス可能.<br>
	 * 
	 * @return 順序付マップ
	 */
	public Map<String, String> getCharacterDataTemplates() {
		// キャラクターデータのxmlファイル名をキーとし、表示名を値とするマップ
		final LinkedHashMap<String, String> templateNameMap = new LinkedHashMap<String, String>();

		// テンプレートの定義プロパティのロード
		Properties props = getTemplateListProperties(false);

		// 順序優先
		String strOrders = props.getProperty("displayOrder");
		if (strOrders != null) {
			for (String key : strOrders.split(",")) {
				key = key.trim();
				String val = props.getProperty(key);
				if (val != null && val.trim().length() > 0) {
					String resKey = DEFAULT_CHARACTER_PREFIX + key;
					if (getResource(resKey) != null) {
						// 現存するテンプレートのみ登録
						templateNameMap.put(key, val);
					}
				}
			}
		}

		// 順序が指定されていないアイテムの追加
		Enumeration<?> enm = props.propertyNames();
		while (enm.hasMoreElements()) {
			String key = (String) enm.nextElement();
			String val = props.getProperty(key);
			if (key.endsWith(".xml")) {
				String resKey = DEFAULT_CHARACTER_PREFIX + key;
				if (getResource(resKey) != null) {
					// 現存するテンプレートのみ登録
					templateNameMap.put(key, val);
				}
			}
		}

		// フォルダにある未登録のxmlファイルもテンプレート一覧に加える
		// (ただし、テンプレートリストプロパティを除く)
		try {
			File templDir = getTemplateDir(false);
			if (templDir.isDirectory()) {
				File[] files = templDir.listFiles(new java.io.FileFilter() {
					public boolean accept(File pathname) {
						String name = pathname.getName();
						if (templateNameMap.containsKey(name)) {
							// すでに登録済みなのでスキップする.
							return false;
						}
						if (name.startsWith(TEMPLATE_LIST_XML)) {
							// テンプレートリストプロパティファイルは除外する.
							return false;
						}
						return pathname.isFile() && name.endsWith(".xml");
					}
				});
				if (files == null) {
					files = new File[0];
				}
				CharacterDataPersistent persist = CharacterDataPersistent
						.getInstance();
				for (File file : files) {
					try {
						URI docBase = file.toURI();
						CharacterData cd = persist.loadProfile(docBase);
						if (cd != null && cd.isValid()) {
							String name = file.getName();
							templateNameMap.put(name, cd.getName());
						}
					} catch (IOException ex) {
						logger.log(Level.WARNING, "failed to read templatedir."
								+ file, ex);
					}
				}
			}

		} catch (IOException ex) {
			// ディレクトリの一覧取得に失敗しても無視する.
			logger.log(Level.FINE, "failed to read templatedir.", ex);
		}

		return templateNameMap;
	}

	/**
	 * XMLリソースファイルから、定義済みのキャラクターデータを生成して返す.<br>
	 * (現在のロケールの言語に対応するデータを取得し、なければ最初の言語で代替する.)<br>
	 * 生成されたキャラクター定義のdocBaseはnullであるため、使用する場合はdocBaseをセットすること.<br>
	 * 都度、XMLファイルから読み込まれる.<br>
	 * 
	 * @return デフォルトキャラクターデータ
	 * @throws IOException
	 *             失敗
	 */
	public CharacterData loadPredefinedCharacterData(String name)
			throws IOException {
		CharacterData cd;
		String resKey = DEFAULT_CHARACTER_PREFIX + name;
		URL predefinedCharacter = getResource(resKey);
		if (predefinedCharacter == null) {
			throw new FileNotFoundException(resKey);
		}
		InputStream is = predefinedCharacter.openStream();
		try {
			logger.log(Level.INFO, "load a predefined characterdata. resKey="
					+ resKey);
			CharacterDataXMLReader characterDataXmlReader = new CharacterDataXMLReader();
			cd = characterDataXmlReader.loadCharacterDataFromXML(is, null);

		} finally {
			is.close();
		}
		return cd;
	}

	/**
	 * リソースを取得する.<br>
	 * 
	 * @param resKey
	 *            リソースキー
	 * @return リソース、なければnull
	 */
	protected URL getResource(String resKey) {
		ResourceLoader resourceLoader = new ResourceLoader();
		return resourceLoader.getResource(resKey);
	}

	/**
	 * カスタマイズ用のテンプレートディレクトリを取得する.<br>
	 * まだ作成されていない場合で、prepareが指示されている場合はフォルダを準備し、 既定のファイルを作成する.<br>
	 * 
	 * @param prepare
	 *            実際にディレクトリを準備する場合はtrue
	 * @return テンプレートディレクトリ
	 */
	public File getTemplateDir(boolean prepare) throws IOException {
		File baseDir = ConfigurationDirUtilities.getUserDataDir();
		SetupLocalization setup = new SetupLocalization(baseDir);
		File resourceDir = setup.getResourceDir();

		if (prepare) {
			// テンプレートリソースが未設定であれば設定する.
			setup.setupToLocal(
					EnumSet.of(SetupLocalization.Resources.Template), false);

			// ディレクトリがなければ作成しておく
			if (resourceDir.exists()) {
				resourceDir.mkdirs();
			}
		}
		return new File(resourceDir, DEFAULT_CHARACTER_PREFIX);
	}

	/**
	 * "characterDataTemplates*.xml"のファイルは管理ファイルのため、 <br>
	 * ユーザによる書き込みは禁止とする.<br>
	 * 
	 * @param name
	 * @return 書き込み可能であるか？
	 */
	public boolean canFileSave(String name) {
		if (name.trim().startsWith("characterDataTemplates")) {
			return false;
		}
		return true;
	}

	/**
	 * 指定したキャラクターデータをテンプレートとして保存する.<br>
	 * 
	 * @param name
	 *            保存するテンプレートファイル名
	 * @param cd
	 *            キャラクターデータ
	 * @param localizedName
	 *            表示名
	 * @throws IOException
	 */
	public void saveTemplate(String name, CharacterData cd, String localizedName)
			throws IOException {
		if (name == null || !canFileSave(name)) {
			throw new IllegalArgumentException();
		}

		// テンプレートファイル位置の準備
		// (リソースが、まだファイルに展開されていなれば展開する)
		File templDir = getTemplateDir(true);
		File templFile = new File(templDir, name);

		// キャラクターデータをXML形式でテンプレートファイルへ保存
		CharacterDataXMLWriter characterDataXmlWriter = new CharacterDataXMLWriter();
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(templFile));
		try {
			// パーツセットなしの状態とし、名前をローカライズ名に設定する.
			CharacterData templCd = cd.duplicateBasicInfo(false);
			templCd.setName(localizedName);

			characterDataXmlWriter.writeXMLCharacterData(templCd, bos);

		} finally {
			bos.close();
		}

		// テンプレートの定義プロパティのロード(言語中立を優先)
		Properties neutralProps = getTemplateListProperties(true);

		// テンプレート一覧の更新
		neutralProps.put(name, localizedName);

		// テンプレート一覧の保存
		File neutralPropsFile = new File(templDir, TEMPLATE_LIST_XML + ".xml");
		BufferedOutputStream fos = new BufferedOutputStream(
				new FileOutputStream(neutralPropsFile));
		try {
			neutralProps.storeToXML(fos,
					new Timestamp(System.currentTimeMillis()).toString());

		} finally {
			bos.close();
		}
	}
}
