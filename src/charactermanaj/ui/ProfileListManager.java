package charactermanaj.ui;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.io.CharacterDataDefaultProvider;
import charactermanaj.model.io.CharacterDataDefaultProvider.DefaultCharacterDataVersion;
import charactermanaj.model.io.CharacterDataPersistent;
import charactermanaj.model.io.CharacterDataPersistent.ListProfileCallback;
import charactermanaj.model.io.CharacterDataPersistent.ProfileListErrorHandler;
import charactermanaj.model.io.PartsDataLoader;
import charactermanaj.model.io.PartsDataLoaderFactory;
import charactermanaj.model.io.PartsInfoXMLReader;
import charactermanaj.model.io.PartsManageDataDecorateLoader;
import charactermanaj.model.io.PartsSpecDecorateLoader;
import charactermanaj.model.io.RecentDataPersistent;
import charactermanaj.util.ErrorMessageHelper;

/**
 * プロファイルの選択・管理を行うクラス.
 * 
 * @author seraphy
 */
public final class ProfileListManager {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(ProfileListManager.class.getName());

	/**
	 * プライベートコンストラクタ
	 */
	private ProfileListManager() {
		super();
	}
	
	/**
	 * すべてのメインフレームで使用中のキャラクターデータのコレクション.<br>
	 */
	private static final HashMap<URI, Integer> activeCharacterDatas = new HashMap<URI, Integer>(); 
	
	/**
	 * キャラクターデータが使用中であるか?<br>
	 * キャラクターデータのDocBaseをもとに判断する.<br>
	 * nullを指定した場合は常にfalseを返す.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ、またはnull
	 * @return 使用中であればtrue
	 */
	public static boolean isUsingCharacterData(CharacterData characterData) {
		URI characterDocBase = (characterData == null) ? null
				: characterData.getDocBase();
		synchronized (activeCharacterDatas) {
			Integer cnt = (characterDocBase == null) ? null
					: activeCharacterDatas.get(characterDocBase);
			return cnt != null && cnt.intValue() > 0;
		}
	}
	
	/**
	 * キャラクターデータが使用中であることを登録する。
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 */
	public static void registerUsedCharacterData(CharacterData characterData) {
		if (characterData == null) {
			return;
		}
		synchronized (activeCharacterDatas) {
			URI characterDocBase = characterData.getDocBase();
			if (characterDocBase != null) {
				Integer cnt = activeCharacterDatas.get(characterDocBase);
				if (cnt == null) {
					cnt = Integer.valueOf(1);
				} else {
					cnt = Integer.valueOf(cnt.intValue() + 1);
				}
				activeCharacterDatas.put(characterDocBase, cnt);
			}
		}
	}
	
	/**
	 * キャラクターデータの使用中であることを登録解除する。
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 */
	public static void unregisterUsedCharacterData(CharacterData characterData) {
		if (characterData == null) {
			return;
		}
		// 使用中のキャラクターデータとしてのカウントを減らす
		synchronized (activeCharacterDatas) {
			URI characterDocBase = characterData.getDocBase();
			if (characterDocBase != null) {
				Integer cnt = activeCharacterDatas.get(characterDocBase);
				if (cnt != null) {
					cnt = Integer.valueOf(cnt.intValue() - 1);
					if (cnt.intValue() <= 0) {
						activeCharacterDatas.remove(characterDocBase);
					} else {
						activeCharacterDatas.put(characterDocBase, cnt);
					}
				}
			}
		}
	}
	
	/**
	 * プロファイル選択ダイアログを表示し、選択されたプロファイルのメインフレームを作成して返す.<br>
	 * プロファイルの選択をキャンセルした場合はnullを返す.<br>
	 * 
	 * @param parent
	 *            親フレーム
	 * @return メインフレーム、もしくはnull
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public static MainFrame openProfile(JFrame parent) throws IOException {
		// キャラクタープロファイルのリストをロード
		CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
		List<CharacterData> characterDatas = persist
				.listProfiles(new ProfileListErrorHandler() {
					public void occureException(File baseDir, Throwable ex) {
						logger.log(Level.WARNING, "invalid profile. :"
								+ baseDir, ex);
					}
				});

		// 選択ダイアログを表示
		ProfileSelectorDialog selDlg = new ProfileSelectorDialog(parent, characterDatas);
		selDlg.setVisible(true);

		CharacterData characterData = selDlg.getSelectedCharacterData();
		if (characterData == null || !characterData.isValid()) {
			// キャンセルしたか、開くことのできないデータ
			return null;
		}

		// メインフレームを準備
		MainFrame newFrame;
		parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			newFrame = openProfile(characterData);
			
		} finally {
			parent.setCursor(Cursor.getDefaultCursor());
		}
		return newFrame;
	}

	/**
	 * 指定したキャラクター定義で新しいメインフレームを作成して返す.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @return 作成されたメインフレーム
	 * @throws IOException
	 *             例外
	 */
	public static MainFrame openProfile(CharacterData characterData) throws IOException {
		if (characterData == null || !characterData.isValid()) {
			throw new IOException("開くことのできないキャラクターデータです。:" + characterData);
		}

		// キャラクターデータのロード
		loadCharacterData(characterData);
		loadFavorites(characterData);

		// メインフレームを構築
		MainFrame newFrame = new MainFrame(characterData);

		// 最後に使ったプロファイルとして登録
		saveRecent(characterData);
		
		return newFrame;
	}
	
	/**
	 * キャラクター定義編集用ダイアログを生成して返す.
	 * 
	 * @author seraphy
	 */
	private interface ProfileEditorDialogFactory {
		
		ProfileEditDialog create(CharacterData characterData);
		
	}
	
	/**
	 * キャラクター定義を編集する.<br>
	 * 
	 * @param parent
	 *            親ダイアログ、もしくはnull
	 * @param characterData
	 *            キャラクター定義(参照のみ、変更されない.)
	 * @return 編集されたキャラクター定義、もしくはキャンセルされた場合はnull
	 * @throws IOException
	 *             失敗
	 */
	public static CharacterData editProfile(final JDialog parent, CharacterData characterData) throws IOException {
		return internalEditProfile(characterData, new ProfileEditorDialogFactory() {
			public ProfileEditDialog create(CharacterData characterData) {
				return new ProfileEditDialog(parent, characterData);
			}
		});
	}
	
	/**
	 * キャラクター定義を編集する.<br>
	 * 
	 * @param parent
	 *            親フレーム、もしくはnull
	 * @param characterData
	 *            キャラクター定義(参照のみ、変更されない.)
	 * @return 編集されたキャラクター定義、もしくはキャンセルされた場合はnull
	 * @throws IOException
	 *             失敗
	 */
	public static CharacterData editProfile(final JFrame parent, CharacterData characterData) throws IOException {
		return internalEditProfile(characterData, new ProfileEditorDialogFactory() {
			public ProfileEditDialog create(CharacterData characterData) {
				return new ProfileEditDialog(parent, characterData);
			}
		});
	}
	
	/**
	 * キャラクター定義を編集する.<br>
	 * 
	 * @param characterData
	 *            キャラクター定義(参照のみ、変更されない.)
	 * @param dialogFactory
	 *            キャラクター定義編集ダイアログを生成するファクトリ
	 * @return 編集されたキャラクター定義、もしくはキャンセルされた場合はnull
	 * @throws IOException
	 *             失敗
	 */
	private static CharacterData internalEditProfile(CharacterData characterData, ProfileEditorDialogFactory dialogFactory) throws IOException {
		if (characterData == null || !characterData.isValid()) {
			throw new IOException("開くことのできないキャラクターデータです。:" + characterData);
		}
		
		// キャラクターデータのコピーを作成する.(プリセットも含む)
		CharacterData original = characterData.duplicateBasicInfo(true);
		original.clearPartsSets(true); // プリセット以外のパーツセットはクリアする.
		
		try {
			loadFavorites(original);

		} catch (IOException ex) {
			ErrorMessageHelper.showErrorDialog(null, ex);
			// Favoritesの読み込みに失敗しても継続する.
		}
		
		// 編集用ダイアログを構築して開く
		ProfileEditDialog editDlg = dialogFactory.create(original);
		editDlg.setVisible(true);

		// 編集結果を得る.
		CharacterData newCd = editDlg.getResult();
		if (newCd == null) {
			// キャンセルされた場合
			return null;
		}
		
		// 保存する.
		CharacterDataPersistent persist = CharacterDataPersistent.getInstance();

		persist.updateProfile(newCd);
		persist.saveFavorites(newCd);

		return newCd;
	}
	
	/**
	 * 最後にしようしたプロファイル、それがなければデフォルトプロファイルを開いて、そのメインフレームを返す.
	 * 
	 * @return メインフレーム
	 * @throws IOException
	 *             開けなかった場合
	 */
	public static MainFrame openDefaultProfile() throws IOException {

		CharacterDataPersistent persistent = CharacterDataPersistent.getInstance();

		CharacterData characterData;

		// 最後に使用したプロファイルのロードを試行する.
		try {
			characterData = loadRecent();
			if (characterData != null) {
				// キャラクターデータを読み込む
				loadCharacterData(characterData);
				loadFavorites(characterData);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(null, ex);
			characterData = null;
		}

		// 最後に使用したプロファイルの記録がないか、プロファイルのロードに失敗した場合は
		// プロファイル一覧から最初のプロファイルを選択する.
		if (characterData == null) {
			final ArrayList<CharacterData> profiles = new ArrayList<CharacterData>();
			Future<?> future = persistent
					.listProfileAsync(new ListProfileCallback() {
						public boolean receiveCharacterData(
								CharacterData characterData) {
							try {
								loadCharacterData(characterData);
								loadFavorites(characterData);
								synchronized (profiles) {
									profiles.add(characterData);
								}
								// 読み込めたものが1つでもあれば、以降は不要なので打ち切り
								return false;

							} catch (Exception ex) {
								logger.log(Level.SEVERE, "プロファイルのロードに失敗しました。"
										+ characterData, ex);
								// プロファイルの読み込みに失敗した場合は次を試行する.
								return true;
							}
						}
						public boolean occureException(File baseDir,
								Exception ex) {
							logger.log(Level.WARNING, "invalid profile. :"
									+ baseDir, ex);
							// エラーでも継続する
							return true;
						}
					});
			try {
				future.get();
				synchronized (profiles) {
					if (!profiles.isEmpty()) {
						characterData = profiles.get(0);
					}
				}
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "プロファイルのロードに失敗しました。" + ex, ex);
			}
		}

		// プロファイルが一個もなければ、デフォルトのプロファイルの生成を試行する.
		if (characterData == null) {
			logger.info("オープンできるプロファイルがないため、新規プロファイルを作成します。");
			try {
				CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
				characterData = defProv
						.createDefaultCharacterData(DefaultCharacterDataVersion.V3);
				persistent.createProfile(characterData);

			} catch (IOException ex) {
				// デフォルトのプロファイルが作成できないことは致命的であるが、
				// アプリケーションを起動させるために継続する.
				logger.log(Level.SEVERE, "default profile creation failed.", ex);
				
				// キャラクター定義として無効なダミーのインスタンスを生成して返す.
				// 何もできないが、メインフレームを空の状態で表示させることは可能.
				characterData = new CharacterData();
			}
		}

		// 最後に使用したプロファイルとして記録
		saveRecent(characterData);

		// メインフレームを生成して返す
		return new MainFrame(characterData);
	}
	
	/**
	 * キャラクターデータに、パーツデータをロードする.<br>
	 * お気に入りはロードされないので、必要ならば、このあとで{@link #loadFavorites(CharacterData)}を呼び出す.<br>
	 * 
	 * @param characterData
	 * @throws IOException
	 *             開けなかった場合
	 */
	public static void loadCharacterData(final CharacterData characterData) throws IOException {
		if (characterData != null && characterData.isValid()) {
			final PartsInfoXMLReader xmlReader = new PartsInfoXMLReader();
			
			PartsDataLoaderFactory loaderFactory = PartsDataLoaderFactory.getInstance();
			PartsDataLoader loader = loaderFactory.createPartsLoader(characterData.getDocBase());
			PartsDataLoader colorGroupInfoDecorater = new PartsSpecDecorateLoader(loader, characterData.getColorGroups());
			PartsManageDataDecorateLoader partsMngDecorater
					= new PartsManageDataDecorateLoader(colorGroupInfoDecorater,
							new PartsManageDataDecorateLoader.PartsManageDataFactory() {
						public PartsManageData createPartsManageData() {
							try {
								return xmlReader
										.loadPartsManageData(characterData
												.getDocBase());
							} catch (Exception ex) {
								logger.log(Level.WARNING, "parts-info.xml loading failed.", ex);
								return new PartsManageData();
							}
						}
					});
			
			characterData.loadPartsData(partsMngDecorater);
		}
	}
	
	/**
	 * キャラクターデータに、お気に入りをロードする.<br>
	 * 
	 * @param characterData
	 * @throws IOException
	 *             開けなかった場合
	 */
	public static void loadFavorites(final CharacterData characterData) throws IOException {
		if (characterData != null && characterData.isValid()) {
			final CharacterDataPersistent persistent = CharacterDataPersistent.getInstance();
			persistent.loadFavorites(characterData);
		}
	}

	/**
	 * 最後に使用したキャラクターデータとして記録する.
	 * 
	 * @param characterData
	 * @throws IOException
	 *             保存できなった場合
	 */
	public static void saveRecent(CharacterData characterData) throws IOException {
		RecentDataPersistent recentPersist = RecentDataPersistent.getInstance();
		recentPersist.saveRecent(characterData);
	}
	
	/**
	 * 最後に使用したキャラクターデータを取得する.
	 * 
	 * @return キャラクターデータ。最後に使用したデータが存在しない場合はnull
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public static CharacterData loadRecent() throws IOException {
		RecentDataPersistent recentPersist = RecentDataPersistent.getInstance();
		return recentPersist.loadRecent();
	}
	
}
