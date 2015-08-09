package charactermanaj.model.io;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.io.CharacterDataDefaultProvider.DefaultCharacterDataVersion;

/**
 * character.iniファイルを読み込むためのクラス.
 * 
 * @author seraphy
 */
public class CharacterDataIniReader {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(CharacterDataIniReader.class.getName());

	/**
	 * character.iniファイルから、キャラクター定義を生成します.<br>
	 * docBaseは設定されていないため、戻り値に設定する必要があります.<br>
	 * 
	 * @param is
	 *            character.iniの入力ストリーム
	 * @param version
	 *            デフォルトキャラクターセットのバージョン
	 * @return キャラクターデータ
	 * @throws IOException
	 *             読み取りに失敗した場合
	 */
	public CharacterData readCharacterDataFromIni(InputStream is,
			DefaultCharacterDataVersion version)
			throws IOException {
		if (is == null || version == null) {
			throw new IllegalArgumentException();
		}

		CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
		CharacterData cd = defProv.createDefaultCharacterData(version);

		// イメージサイズ
		int siz_x = 0;
		int siz_y = 0;

		// パーツセット
		HashMap<String, String> plainPartsSet = new HashMap<String, String>();

		try {
			BufferedReader rd;
			try {
				rd = new BufferedReader(new InputStreamReader(is, "MS932")); // SJISで読み取る.
			} catch (UnsupportedEncodingException ex) {
				logger.log(Level.SEVERE, "SJIS encoded file cannot be read.",
						ex);
				rd = new BufferedReader(new InputStreamReader(is)); // システムデフォルトで読み込む
			}

			try {
				String line;
				int sectionMode = 0;
				while ((line = rd.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}

					if (line.startsWith("[")) {
						// セクションの判定
						if (line.toLowerCase().equals("[size]")) {
							// Sizeセクション
							sectionMode = 1;
						} else if (line.toLowerCase().equals("[parts]")) {
							// Partsセクション
							sectionMode = 2;
						} else {
							// それ以外のセクションなのでモードをリセット.
							// 色情報のセクション「Color」は現在のところサポートしていない.
							sectionMode = 0;
						}
					} else {
						int eqpos = line.indexOf('=');
						String key, val;
						if (eqpos >= 0) {
							// キーは小文字に揃える (大小を無視して比較できるようにするため)
							key = line.substring(0, eqpos).toLowerCase().trim();
							val = line.substring(eqpos + 1);
						} else {
							key = line.toLowerCase().trim();
							val = "";
						}

						if (sectionMode == 1) {
							// Sizeセクション
							try {
								if (key.equals("size_x")) {
									siz_x = Integer.parseInt(val);
								} else if (key.equals("size_y")) {
									siz_y = Integer.parseInt(val);
								}
							} catch (RuntimeException ex) {
								logger.log(Level.WARNING,
										"character.ini invalid. key=" + key
												+ "/val=" + val, ex);
								// 変換できないものは無視する.
							}
						} else if (sectionMode == 2) {
							// Partsセクション
							if (key.length() > 0) {
								plainPartsSet.put(key, val);
							}
						}
					}
				}
			} finally {
				rd.close();
			}

		} catch (IOException ex) {
			// エラーが発生したら、character.iniは無かったことにして続ける.
			logger.log(Level.WARNING, "character.ini invalid.", ex);
			return null;

		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "can't close file.", ex);
			}
		}

		// イメージサイズの設定
		if (siz_x > 0 && siz_y > 0) {
			cd.setImageSize(new Dimension(siz_x, siz_y));
		}

		// パーツセットを構築する.
		boolean existsPartsetParts = false;
		if (!plainPartsSet.isEmpty()) {
			PartsSet partsSet = new PartsSet("default", "default", true);
			for (Map.Entry<String, String> entry : plainPartsSet.entrySet()) {
				String categoryId = entry.getKey();
				String partsName = entry.getValue();

				PartsCategory partsCategory = cd.getPartsCategory(categoryId);
				if (partsCategory != null) {
					PartsIdentifier partsIdentifier;
					if (partsName == null || partsName.length() == 0) {
						partsIdentifier = null;
					} else {
						partsIdentifier = new PartsIdentifier(partsCategory,
								partsName, partsName);
						existsPartsetParts = true;
					}
					partsSet.appendParts(partsCategory, partsIdentifier, null);
				}
			}
			if (!partsSet.isEmpty() && existsPartsetParts) {
				// パーツセットが空でなく、
				// なにかしらのパーツが登録されている場合のみパーツセットを登録する.
				cd.addPartsSet(partsSet);
				cd.setDefaultPartsSetId("default");
			}
		}

		return cd;
	}
}
