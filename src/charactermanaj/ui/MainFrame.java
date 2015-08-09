package charactermanaj.ui;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import charactermanaj.Main;
import charactermanaj.clipboardSupport.ClipboardUtil;
import charactermanaj.graphics.AsyncImageBuilder;
import charactermanaj.graphics.ColorConvertedImageCachedLoader;
import charactermanaj.graphics.ImageBuildJobAbstractAdaptor;
import charactermanaj.graphics.ImageBuilder.ImageOutput;
import charactermanaj.graphics.io.ImageSaveHelper;
import charactermanaj.graphics.io.OutputOption;
import charactermanaj.graphics.io.UkagakaImageSaveHelper;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.CharacterDataChangeEvent;
import charactermanaj.model.CharacterDataChangeListener;
import charactermanaj.model.CharacterDataChangeObserver;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.IndependentPartsSetInfo;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsColorManager;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.RecommendationURL;
import charactermanaj.model.WorkingSet;
import charactermanaj.model.WorkingSet2;
import charactermanaj.model.io.CharacterDataPersistent;
import charactermanaj.model.io.PartsImageDirectoryWatchAgent;
import charactermanaj.model.io.PartsImageDirectoryWatchAgentFactory;
import charactermanaj.model.io.PartsImageDirectoryWatchEvent;
import charactermanaj.model.io.PartsImageDirectoryWatchListener;
import charactermanaj.model.io.RecentDataPersistent;
import charactermanaj.model.io.WorkingSetPersist;
import charactermanaj.ui.ImageSelectPanel.ImageSelectPanelEvent;
import charactermanaj.ui.ImageSelectPanel.ImageSelectPanelListener;
import charactermanaj.ui.ManageFavoriteDialog.FavoriteManageCallback;
import charactermanaj.ui.PreviewPanel.PreviewPanelEvent;
import charactermanaj.ui.PreviewPanel.PreviewPanelListener;
import charactermanaj.ui.model.ColorChangeEvent;
import charactermanaj.ui.model.ColorChangeListener;
import charactermanaj.ui.model.ColorGroupCoordinator;
import charactermanaj.ui.model.FavoritesChangeEvent;
import charactermanaj.ui.model.FavoritesChangeListener;
import charactermanaj.ui.model.FavoritesChangeObserver;
import charactermanaj.ui.model.PartsColorCoordinator;
import charactermanaj.ui.model.PartsSelectionManager;
import charactermanaj.ui.model.WallpaperFactory;
import charactermanaj.ui.model.WallpaperFactoryErrorRecoverHandler;
import charactermanaj.ui.model.WallpaperFactoryException;
import charactermanaj.ui.model.WallpaperInfo;
import charactermanaj.ui.scrollablemenu.JScrollableMenu;
import charactermanaj.ui.util.FileDropTarget;
import charactermanaj.ui.util.WindowAdjustLocationSupport;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.SystemUtil;
import charactermanaj.util.UIHelper;


/**
 * メインフレーム.<br>
 * アプリケーションがアクティブである場合は最低でも1つのメインフレームが表示されている.<br>
 *
 * @author seraphy
 */
public class MainFrame extends JFrame
		implements
			FavoritesChangeListener,
			CharacterDataChangeListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(MainFrame.class.getName());


	protected static final String STRINGS_RESOURCE = "languages/mainframe";

	protected static final String MENU_STRINGS_RESOURCE = "menu/menu";

	/**
	 * メインフレームのアイコン.<br>
	 */
	protected BufferedImage icon;


	/**
	 * 現在アクティブなメインフレーム.<br>
	 * フォーカスが切り替わるたびにアクティブフレームを追跡する.<br>
	 * Mac OS XのAbout/Preferences/Quitのシステムメニューからよびだされた場合に
	 * オーナーたるべきメインフレームを識別するためのもの.<br>
	 */
	private static volatile MainFrame activedMainFrame;


	/**
	 * このメインフレームが対象とするキャラクターデータ.<br>
	 */
	protected CharacterData characterData;


	/**
	 * プレビューペイン
	 */
	private PreviewPanel previewPane;

	/**
	 * パーツ選択マネージャ
	 */
	protected PartsSelectionManager partsSelectionManager;

	/**
	 * パネルの最小化モード
	 */
	private boolean minimizeMode;


	/**
	 * パーツ選択パネルリスト
	 */
	protected ImageSelectPanelList imageSelectPanels;

	/**
	 * パーツ選択パネルを納めるスクロールペイン
	 */
	protected JScrollPane imgSelectPanelsPanelSp;

	/**
	 * カラーグループのマネージャ
	 */
	protected ColorGroupCoordinator colorGroupCoordinator;

	/**
	 * パーツカラーのマネージャ
	 */
	protected PartsColorCoordinator partsColorCoordinator;


	/**
	 * キャッシュつきのイメージローダ.<br>
	 */
	private ColorConvertedImageCachedLoader imageLoader;

	/**
	 * パーツを組み立てて1つのプレビュー可能なイメージを構築するためのビルダ
	 */
	private AsyncImageBuilder imageBuilder;


	/**
	 * パーツイメージを画像として保存する場合のヘルパー.<br>
	 * 最後に使ったディレクトリを保持するためのメンバ変数としている.<br>
	 */
	private ImageSaveHelper imageSaveHelper = new ImageSaveHelper();

	/**
	 * 伺か用出力ヘルパ.<br>
	 * 最後に使ったディレクトリ、ファイル名、モードなどを保持するためのメンバ変数としている.<br>
	 */
	private UkagakaImageSaveHelper ukagakaImageSaveHelper = new UkagakaImageSaveHelper();

	/**
	 * パーツディレクトリを定期的にチェックし、パーツイメージが変更・追加・削除されている場合に パーツリストを更新するためのウォッチャー
	 */
	private PartsImageDirectoryWatchAgent watchAgent;

	/**
	 * デフォルトのパーツセット表示名
	 */
	private String defaultPartsSetTitle;

	/**
	 * 最後に使用したプリセット.<br>
	 * (一度もプリセットを使用していなければnull).
	 */
	private PartsSet lastUsePresetParts;

	/**
	 * 最後に使用した検索ダイアログ.<br>
	 * nullであれば一度も使用していない.<br>
	 * (nullでなくとも閉じられている可能性がある.)<br>
	 */
	private SearchPartsDialog lastUseSearchPartsDialog;

	/**
	 * 最後に使用したお気に入りダイアログ.<br>
	 * nullであれば一度も使用していない.<br>
	 * (nullでなくとも閉じられている可能性がある.)
	 */
	private ManageFavoriteDialog lastUseManageFavoritesDialog;

	/**
	 * 最後に使用したパーツのランダム選択ダイアログ.<br>
	 * nullであれば一度も使用していない.<br>
	 * (nullでなくとも閉じられている可能性がある.)
	 */
	private PartsRandomChooserDialog lastUsePartsRandomChooserDialog;

	/**
	 * 最後に使用した壁紙情報
	 */
	private WallpaperInfo wallpaperInfo;


	/**
	 * アクティブなメインフレームを設定する.
	 *
	 * @param mainFrame
	 *            メインフレーム
	 */
	public static void setActivedMainFrame(MainFrame mainFrame) {
		if (mainFrame == null) {
			throw new IllegalArgumentException();
		}
		activedMainFrame = mainFrame;
	}

	/**
	 * 現在アクティブなメインフレームを取得する. まだメインフレームが開かれていない場合はnull.<br>
	 * 最後のメインフレームが破棄中、もしくは破棄済みであれば破棄されたフレームを示すことに注意.<br>
	 *
	 * @return メインフレーム、もしくはnull
	 */
	public static MainFrame getActivedMainFrame() {
		return activedMainFrame;
	}

	/**
	 * キャラクターデータが変更された場合に通知される.
	 */
	public void notifyChangeCharacterData(final CharacterDataChangeEvent e) {
		final CharacterData cd = e.getCharacterData();
		if (cd != null
				&& cd.getDocBase().equals(
						MainFrame.this.characterData.getDocBase())) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						Cursor oldCur = getCursor();
						setCursor(Cursor
								.getPredefinedCursor(Cursor.WAIT_CURSOR));
						try {
							if (e.isChangeStructure()) {
								// 現在情報の保存
								saveWorkingSet();

								// 画面構成の再構築
								initComponent(cd);
							}

							if (e.isReloadPartsAndFavorites()) {
								// パーツとお気に入りのリロード
								reloadPartsAndFavorites(cd, true);
							}

						} finally {
							setCursor(oldCur != null ? oldCur : Cursor
									.getDefaultCursor());
						}

					} catch (Exception ex) {
						ErrorMessageHelper.showErrorDialog(MainFrame.this, ex);
					}
				}
			});
		}
	}

	/**
	 * お気に入りデータが変更された場合に通知される.
	 *
	 * @param e
	 */
	public void notifyChangeFavorites(FavoritesChangeEvent e) {
		CharacterData cd = e.getCharacterData();
		if (cd != null
				&& cd.getDocBase().equals(
						MainFrame.this.characterData.getDocBase())) {
			if (!MainFrame.this.equals(e.getSource())
					&& !characterData.equals(cd)) {
				// プリセットとお気に入りを最新化する.
				// ただし、自分自身から送信したイベントの場合は最新化は不要.
				characterData.clearPartsSets(false);
				for (Map.Entry<String, PartsSet> entry : cd.getPartsSets()
						.entrySet()) {
					PartsSet partsSet = entry.getValue();
					characterData.addPartsSet(partsSet);
				}
			}

			// お気に入り管理ダイアログ上のお気に入り一覧を最新に更新する.
			if (lastUseManageFavoritesDialog != null
					&& lastUseManageFavoritesDialog.isDisplayable()) {
				lastUseManageFavoritesDialog.initListModel();
			}
		}
	}

	/**
	 * メインフレームを構築する.
	 *
	 * @param characterData
	 *            キャラクターデータ
	 */
	public MainFrame(CharacterData characterData) {
		try {
			if (characterData == null) {
				throw new IllegalArgumentException();
			}

			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					onCloseProfile();
				}
				@Override
				public void windowClosed(WindowEvent e) {
					stopAgents();
				}
				@Override
				public void windowActivated(WindowEvent e) {
					setActivedMainFrame(MainFrame.this);
				}
				@Override
				public void windowOpened(WindowEvent e) {
					// do nothing.
				}
			});

			// アイコンの設定
			icon = UIHelper.getInstance().getImage("icons/icon.png");
			setIconImage(icon);

			// 画面コンポーネント作成
			initComponent(characterData);
			JMenuBar menuBar = createMenuBar();
			setJMenuBar(menuBar);

			// お気に入り変更通知を受け取る
			FavoritesChangeObserver.getDefault().addFavoritesChangeListener(
					this);
			// キャラクターデータの変更通知を受け取る
			CharacterDataChangeObserver.getDefault()
					.addCharacterDataChangeListener(this);

			// メインスクリーンサイズを取得する.
			GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
			Rectangle desktopSize = genv.getMaximumWindowBounds(); // メインスクリーンのサイズ(デスクトップ領域のみ)
			logger.log(Level.CONFIG, "desktopSize=" + desktopSize);

			Dimension imageSize = characterData.getImageSize();
			// 画像サイズ300x400を基準サイズとして、それ以下にはならない.
			// アプリケーション設定の最大サイズ以上の場合はウィンドウサイズは固定してスクロールバーに任せる
			AppConfig appConfig = AppConfig.getInstance();
			int maxWidth = min(desktopSize.width, appConfig.getMainFrameMaxWidth());
			int maxHeight = min(desktopSize.height, appConfig.getMainFrameMaxHeight());
			int imageWidth = min(maxWidth, max(300, imageSize != null ? imageSize.width : 0));
			int imageHeight = min(maxHeight, max(400, imageSize != null ? imageSize.height : 0));
			// 300x400の画像の場合にメインフレームが600x550だとちょうどいい感じ.
			// それ以上大きい画像の場合は増えた分だけフレームを大きくしておく.
			setSize(imageWidth - 300 + 600, imageHeight - 400 + 550);

			// 次回表示時にプラットフォーム固有位置に表示するように予約
			setLocationByPlatform(true);

		} catch (RuntimeException ex) {
			logger.log(Level.SEVERE, "メインフレームの構築中に予期せぬ例外が発生しました。", ex);
			dispose(); // コンストラクタが呼ばれた時点でJFrameは構築済みなのでdisposeの必要がある.
			throw ex;
		} catch (Error ex) {
			logger.log(Level.SEVERE, "メインフレームの構築中に致命的な例外が発生しました。", ex);
			dispose(); // コンストラクタが呼ばれた時点でJFrameは構築済みなのでdisposeの必要がある.
			throw ex;
		}
	}

	/**
	 * メインフレームを表示する.<br>
	 * デスクトップ領域からはみ出した場合は位置を補正する.<br>
	 */
	public void showMainFrame() {
		// メインスクリーンサイズを取得する.
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle desktopSize = genv.getMaximumWindowBounds(); // メインスクリーンのサイズ(デスクトップ領域のみ)
		logger.log(Level.CONFIG, "desktopSize=" + desktopSize);

		// プラットフォーム固有の位置あわせで表示する.
		// 表示した結果、はみ出している場合は0,0に補正する.
		setVisible(true);
		Point loc = getLocation();
		logger.log(Level.CONFIG, "windowLocation=" + loc);
		Dimension windowSize = getSize();
		if (loc.y + windowSize.height >= desktopSize.height) {
			loc.y = 0;
		}
		if (loc.x + windowSize.width >= desktopSize.width) {
			loc.x = 0;
		}
		if (loc.x == 0 || loc.y == 0) {
			setLocation(loc);
		}

		// デスクトップよりも大きい場合は小さくする.
		boolean resize = false;
		Dimension dim = getSize();
		if (dim.height > desktopSize.height) {
			dim.height = desktopSize.height;
			resize = true;
		}
		if (dim.width > desktopSize.width) {
			dim.width = desktopSize.width;
			resize = true;
		}
		if (resize) {
			setSize(dim);
		}
	}

	/**
	 * このメインフレームに関連づけられているエージェントスレッドを停止します.<br>
	 * すでに停止している場合は何もしません。
	 */
	protected void stopAgents() {
		// エージェントを停止
		if (watchAgent != null) {
			try {
				watchAgent.disconnect();

			} catch (Throwable ex) {
				logger.log(Level.SEVERE, "フォルダ監視スレッドの停止に失敗しました。", ex);
			}
			watchAgent = null;
		}
		// イメージビルダを停止
		if (imageBuilder != null) {
			try {
				imageBuilder.stop();

			} catch (Throwable ex) {
				logger.log(Level.SEVERE, "非同期イメージビルダスレッドの停止に失敗しました。", ex);
			}
			imageBuilder = null;
		}
	}

	/**
	 * メインフレームを破棄します.<br>
	 */
	@Override
	public void dispose() {
		FavoritesChangeObserver.getDefault()
				.removeFavoritesChangeListener(this);
		CharacterDataChangeObserver.getDefault()
				.removeCharacterDataChangeListener(this);
	    imageLoader.close();
		stopAgents();
		super.dispose();
	}

	/**
	 * 画面コンポーネントを設定します.<br>
	 * すでに設定されている場合は一旦削除されたのちに再作成されます.<br>
	 */
	private void initComponent(CharacterData characterData) {

		CharacterData oldCd;
		synchronized (this) {
			oldCd = this.characterData;
			if (oldCd != null) {
				// 使用中のキャラクターデータであることを登録解除する。
				ProfileListManager.unregisterUsedCharacterData(oldCd);
			}
			this.characterData = characterData;

			// 使用中のキャラクターデータであることを登録する.
			ProfileListManager.registerUsedCharacterData(characterData);
		}

		// 設定まわり準備
		AppConfig appConfig = AppConfig.getInstance();
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		// タイトル表示
		String title;
		if (Main.isMacOSX()) {
			// Mac OS Xの場合はウィンドウにタイトルはつけない。
			title = "";
		} else {
			title = strings.getProperty("title");
		}
		setTitle(title + characterData.getName());

		// デフォルトのパーツセット表示名
		defaultPartsSetTitle = strings.getProperty("defaultPartsSetTitle");

		// エージェントの停止
		stopAgents();

		// コンポーネント配置
		Container contentPane = getContentPane();

		// すでにあるコンポーネントを削除
		for (Component comp : contentPane.getComponents()) {
			contentPane.remove(comp);
		}
		// 開いている検索ダイアログを閉じる
		closeSearchDialog();

		// 開いているお気に入り管理ダイアログを閉じる
		closeManageFavoritesDialog();

		// 開いているランダム選択ダイアログを閉じる.
		closePartsRandomChooserDialog();

		PartsColorManager partsColorManager = characterData.getPartsColorManager();

		// デフォルトの背景色の設定
		Color bgColor = appConfig.getDefaultImageBgColor();
		wallpaperInfo = new WallpaperInfo();
		wallpaperInfo.setBackgroundColor(bgColor);

		if (imageLoader != null) {
		    imageLoader.close();
		}
		imageLoader = new ColorConvertedImageCachedLoader();
		imageBuilder = new AsyncImageBuilder(imageLoader);
		partsSelectionManager = new PartsSelectionManager(partsColorManager,
				new PartsSelectionManager.ImageBgColorProvider() {
			public Color getImageBgColor() {
				return wallpaperInfo.getBackgroundColor();
			}
			public void setImageBgColor(Color imageBgColor) {
				applyBackgroundColorOnly(imageBgColor);
			}
		});
		colorGroupCoordinator = new ColorGroupCoordinator(partsSelectionManager, partsColorManager);
		partsColorCoordinator = new PartsColorCoordinator(characterData, partsColorManager, colorGroupCoordinator);
		PartsImageDirectoryWatchAgentFactory agentFactory = PartsImageDirectoryWatchAgentFactory.getFactory();
		watchAgent = agentFactory.getAgent(characterData);

		previewPane = new PreviewPanel();
		previewPane.setTitle(defaultPartsSetTitle);
		previewPane.addPreviewPanelListener(new PreviewPanelListener() {
			public void addFavorite(PreviewPanelEvent e) {
				if (!e.isShiftKeyPressed()) {
					// お気に入り登録
					onRegisterFavorite();

				} else {
					// シフトキーにて、お気に入りの管理を開く
					onManageFavorites();
				}
			}
			public void changeBackgroundColor(PreviewPanelEvent e) {
				if ( !e.isShiftKeyPressed()) {
					// 壁紙選択
					onChangeWallpaper();

				} else {
					// シフトキーにて背景色変更
					onChangeBgColor();
				}
			}
			public void copyPicture(PreviewPanelEvent e) {
				onCopy(e.isShiftKeyPressed());
			}
			public void savePicture(PreviewPanelEvent e) {
				if ( !e.isShiftKeyPressed()) {
					// 画像出力
					onSavePicture();

				} else {
					// シフトキーにて「伺か」用出力
					onSaveAsUkagaka();
				}
			}
			public void showInformation(PreviewPanelEvent e) {
				onInformation();
			}
			public void flipHorizontal(PreviewPanelEvent e) {
				onFlipHolizontal();
			}
		});

		imageSelectPanels = new ImageSelectPanelList();

		JPanel imgSelectPanelsPanel = new JPanel();
		BoxLayout bl = new BoxLayout(imgSelectPanelsPanel, BoxLayout.PAGE_AXIS);
		imgSelectPanelsPanel.setLayout(bl);
		for (PartsCategory category : characterData.getPartsCategories()) {
			final ImageSelectPanel imageSelectPanel = new ImageSelectPanel(category, characterData);
			imgSelectPanelsPanel.add(imageSelectPanel);
			imageSelectPanels.add(imageSelectPanel);
			partsSelectionManager.register(imageSelectPanel);
		}

		imgSelectPanelsPanelSp = new JScrollPane(imgSelectPanelsPanel) {
			private static final long serialVersionUID = 1L;
			@Override
			public JScrollBar createVerticalScrollBar() {
				JScrollBar sb = super.createVerticalScrollBar();
				sb.setUnitIncrement(12);
				return sb;
			}
		};
		imgSelectPanelsPanelSp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, imgSelectPanelsPanelSp, previewPane);
		contentPane.add(splitPane, BorderLayout.CENTER);


		imgSelectPanelsPanelSp.requestFocus();

		ArrayList<ColorGroup> colorGroups = new ArrayList<ColorGroup>();
		colorGroups.addAll(characterData.getColorGroups());

		final ColorChangeListener colorChangeListener = new ColorChangeListener() {
			public void onColorGroupChange(ColorChangeEvent event) {
				// do nothing.
			}
			public void onColorChange(ColorChangeEvent event) {
				MainFrame.this.requestPreview();
			}
		};
		colorGroupCoordinator.addColorChangeListener(colorChangeListener);

		for (int idx = 0; idx < imageSelectPanels.size(); idx++) {
			ImageSelectPanel imageSelectPanel = imageSelectPanels.get(idx);
			final PartsCategory partsCategory = imageSelectPanel.getPartsCategory();
			final ColorDialog colorDialog = new ColorDialog(this, partsCategory, colorGroups);
			colorGroupCoordinator.registerColorDialog(colorDialog);
			partsColorCoordinator.register(imageSelectPanel, colorDialog);
			final int curidx = idx;
			imageSelectPanel.addImageSelectListener(new ImageSelectPanelListener() {
				public void onChangeColor(ImageSelectPanelEvent event) {
							WindowAdjustLocationSupport.alignRight(
									MainFrame.this, colorDialog, curidx, false);
					colorDialog.setVisible(!colorDialog.isVisible());
				}
				public void onPreferences(ImageSelectPanelEvent event) {
					// do nothing. (not supported)
				}
				public void onChange(ImageSelectPanelEvent event) {
					MainFrame.this.requestPreview();
				}
				public void onSelectChange(ImageSelectPanelEvent event) {
					// do nothing.
				}
				public void onTitleClick(ImageSelectPanelEvent event) {
					PartsCategory partsCategory = (event != null) ?
							event.getImageSelectPanel().getPartsCategory() : null;
					MainFrame.this.onClickPartsCategoryTitle(partsCategory, false);
				}
				public void onTitleDblClick(ImageSelectPanelEvent event) {
					PartsCategory partsCategory = (event != null) ?
							event.getImageSelectPanel().getPartsCategory() : null;
					MainFrame.this.onClickPartsCategoryTitle(partsCategory, true);
				}
			});
			imageSelectPanel.addAncestorListener(new AncestorListener() {
				public void ancestorAdded(AncestorEvent event) {
				}
				public void ancestorMoved(AncestorEvent event) {
				}
				public void ancestorRemoved(AncestorEvent event) {
					// パネルもしくは、その親が削除されたときにダイアログも非表示とする。
					colorDialog.setVisible(false);
				}
			});
		}

		// 全パーツのロード
		partsSelectionManager.loadParts();

		// 保存されているワーキングセットを復元する.
		// 復元できなかった場合はパーツセットを初期選択する.
		if ( !loadWorkingSet()) {
			if (showDefaultParts(true)) {
				requestPreview();
			}
		}

		// 選択されているパーツを見える状態にする
		scrollToSelectedParts();

		// 非同期イメージローダの処理開始
		if (!imageBuilder.isAlive()) {
			imageBuilder.start();
		}

		// ドロップターゲットの設定
		new DropTarget(imgSelectPanelsPanelSp, new FileDropTarget() {
			@Override
			protected void onDropFiles(final List<File> dropFiles) {
				if (dropFiles == null || dropFiles.isEmpty()) {
					return;
				}
				// インポートダイアログを開く.
				// ドロップソースの処理がブロッキングしないように、
				// ドロップハンドラの処理を終了してからインポートダイアログが開くようにする.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						onImport(dropFiles);
					}
				});
			}
			@Override
			protected void onException(Exception ex) {
				ErrorMessageHelper.showErrorDialog(MainFrame.this, ex);
			}
		});

		// ディレクトリを監視し変更があった場合にパーツをリロードするリスナ
		watchAgent.addPartsImageDirectoryWatchListener(new PartsImageDirectoryWatchListener() {
			public void detectPartsImageChange(PartsImageDirectoryWatchEvent e) {
				Runnable refreshJob = new Runnable() {
					public void run() {
						onDetectPartsImageChange();
					}
				};
				if (SwingUtilities.isEventDispatchThread()) {
					refreshJob.run();
				} else {
					SwingUtilities.invokeLater(refreshJob);
				}
			}
		});

		// 監視が有効であれば、ディレクトリの監視をスタートする
		if (appConfig.isEnableDirWatch() && characterData.isWatchDirectory()) {
			watchAgent.connect();
		}

		// パーツカテゴリの自動縮小が設定されている場合
		minimizeMode = false;
		if (appConfig.isEnableAutoShrinkPanel()) {
			onClickPartsCategoryTitle(null, true);
		}

		// コンポーネントの再構築の場合
		if (oldCd != null) {
			validate();
		}
	}

	/**
	 * パーツが変更されたことを検知した場合.<br>
	 * パーツデータをリロードし、各カテゴリのパーツ一覧を再表示させ、プレビューを更新する.<br>
	 */
	protected void onDetectPartsImageChange() {
		try {
			reloadPartsAndFavorites(null, true);

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "parts reload failed. " + characterData, ex);
		}
	}

	/**
	 * すべてのカテゴリのリストで選択中のアイテムが見えるようにスクロールする.
	 */
	protected void scrollToSelectedParts() {
		partsSelectionManager.scrollToSelectedParts();
	}

	/**
	 * 指定したパーツカテゴリ以外のパーツ選択パネルを最小化する.
	 *
	 * @param partsCategory
	 *            パーツカテゴリ、nullの場合は全て最小化する.
	 * @param dblClick
	 *            ダブルクリック
	 */
	protected void onClickPartsCategoryTitle(PartsCategory partsCategory, boolean dblClick) {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "onClickPartsCategoryTitle category="
					+ partsCategory + "/clickCount=" + dblClick);
		}
		if (dblClick) {
			minimizeMode = !minimizeMode;
			if (!minimizeMode) {
				partsSelectionManager.setMinimizeModeIfOther(null, false);
				return;
			}
		}
		if (minimizeMode) {
			if (partsSelectionManager.isNotMinimizeModeJust(partsCategory)) {
				partsSelectionManager.setMinimizeModeIfOther(null, true); // 全部縮小

			} else {
				partsSelectionManager.setMinimizeModeIfOther(partsCategory, true);
				if (partsCategory != null) {
					// 対象のパネルがスクロールペイン内に見える用にスクロールする.
					// スクロールバーの位置指定などの座標系の操作は「要求」であり、実際に適用されるまで本当の位置は分らない。
					// よって以下の処理は非同期に行い、先に座標を確定させたものに対して行う必要がある。
					final ImageSelectPanel panel = imageSelectPanels.findByPartsCategory(partsCategory);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							final Point pt = panel.getLocation();
							JViewport viewPort = imgSelectPanelsPanelSp.getViewport();
							viewPort.setViewPosition(pt);
							viewPort.revalidate();
						}
					});
				}
			}
		}
	}

	/**
	 * デフォルトパーツを選択する.<br>
	 * デフォルトパーツがなければお気に入りの最初のものを選択する.<br>
	 * それもなければ空として表示する.<br>
	 * パーツの適用に失敗した場合はfalseを返します.(例外は返されません.)<br>
	 *
	 * @param force
	 *            すでに選択があっても選択しなおす場合はtrue、falseの場合は選択があれば何もしない.
	 * @return パーツ選択された場合。force=trueの場合はエラーがなければ常にtrueとなります。
	 */
	protected boolean showDefaultParts(boolean force) {
		try {
			if (!force) {
				// 現在選択中のパーツを取得する.(なければ空)
				PartsSet sel = partsSelectionManager.createPartsSet();
				if (!sel.isEmpty()) {
					// 強制選択でない場合、すでに選択済みのパーツがあれば何もしない.
					return false;
				}
			}

			// デフォルトのパーツセットを取得する
			String defaultPresetId = characterData.getDefaultPartsSetId();
			PartsSet partsSet = null;
			if (defaultPresetId != null) {
				partsSet = characterData.getPartsSets().get(defaultPresetId);
			}

			// デフォルトのパーツセットがなければ、お気に入りの最初を選択する.
			if (partsSet == null) {
				List<PartsSet> partssets = getPartsSetList();
				if (!partssets.isEmpty()) {
					partsSet = partssets.get(0);
				}
			}

			// パーツセットがあれば、それを表示要求する.
			// パーツセットがなければカラーダイアログを初期化するのみ
			if (partsSet == null) {
				partsColorCoordinator.initColorDialog();

			} else {
				selectPresetParts(partsSet);
			}

		} catch (Exception ex) {
			logger.log(Level.WARNING, "パーツのデフォルト適用に失敗しました。", ex);
			return false;
		}
		return true;
	}

	/**
	 * プリセットを適用しキャラクターイメージを再構築します.<br>
	 * 実行時エラーは画面のレポートされます.<br>
	 *
	 * @param presetParts
	 *            パーツセット, nullの場合は何もしない.
	 */
	protected void selectPresetParts(PartsSet presetParts) {
		if (presetParts == null) {
			return;
		}
		try {
			// 最後に使用したプリセットとして記憶する.
			lastUsePresetParts = presetParts;
			// プリセットパーツで選択を変える
			partsSelectionManager.selectPartsSet(presetParts);
			// カラーパネルを選択されているアイテムをもとに再設定する
			partsColorCoordinator.initColorDialog();
			// 再表示
			requestPreview();

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * プリセットとお気に入りを表示順に並べて返す.
	 *
	 * @return プリセットとお気に入りのリスト(表示順)
	 */
	protected List<PartsSet> getPartsSetList() {
		ArrayList<PartsSet> partssets = new ArrayList<PartsSet>();
		partssets.addAll(characterData.getPartsSets().values());
		Collections.sort(partssets, PartsSet.DEFAULT_COMPARATOR);
		return partssets;
	}

	protected static final class TreeLeaf implements Comparable<TreeLeaf> {

		public enum TreeLeafType {
			NODE, LEAF
		}

		private String name;

		private TreeLeafType typ;

		public TreeLeaf(TreeLeafType typ, String name) {
			if (name == null) {
				name = "";
			}
			this.typ = typ;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public TreeLeafType getTyp() {
			return typ;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof TreeLeaf) {
				TreeLeaf o = (TreeLeaf) obj;
				return typ == o.typ && name.equals(o.name);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return typ.hashCode() ^ name.hashCode();
		}

		public int compareTo(TreeLeaf o) {
			int ret = name.compareTo(o.name);
			if (ret == 0) {
				ret = (typ.ordinal() - o.typ.ordinal());
			}
			return ret;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	protected TreeMap<TreeLeaf, Object> buildFavoritesItemTree(
			List<PartsSet> partssets) {
		if (partssets == null) {
			partssets = Collections.emptyList();
		}
		TreeMap<TreeLeaf, Object> favTree = new TreeMap<TreeLeaf, Object>();
		for (PartsSet partsSet : partssets) {
			String flatname = partsSet.getLocalizedName();
			String[] tokens = flatname.split("\\|");
			if (tokens.length == 0) {
				continue;
			}

			TreeMap<TreeLeaf, Object> r = favTree;
			for (int idx = 0; idx < tokens.length - 1; idx++) {
				String name = tokens[idx];
				TreeLeaf leafName = new TreeLeaf(TreeLeaf.TreeLeafType.NODE,
						name);
				@SuppressWarnings("unchecked")
				TreeMap<TreeLeaf, Object> n = (TreeMap<TreeLeaf, Object>) r
						.get(leafName);
				if (n == null) {
					n = new TreeMap<TreeLeaf, Object>();
					r.put(leafName, n);
				}
				r = n;
			}
			String lastName = tokens[tokens.length - 1];
			TreeLeaf lastLeafName = new TreeLeaf(TreeLeaf.TreeLeafType.LEAF,
					lastName);
			@SuppressWarnings("unchecked")
			List<PartsSet> leafValue = (List<PartsSet>) r.get(lastLeafName);
			if (leafValue == null) {
				leafValue = new ArrayList<PartsSet>();
				r.put(lastLeafName, leafValue);
			}
			leafValue.add(partsSet);
		}
		return favTree;
	}

	protected interface FavoriteMenuItemBuilder {
		JMenuItem createFavoriteMenuItem(String name, PartsSet partsSet);
		JMenu createSubMenu(String name);
	}

	private void buildFavoritesMenuItems(List<JMenuItem> menuItems,
			FavoriteMenuItemBuilder favMenuItemBuilder,
			TreeMap<TreeLeaf, Object> favTree) {
		for (Map.Entry<TreeLeaf, Object> entry : favTree.entrySet()) {
			TreeLeaf treeLeaf = entry.getKey();
			String name = treeLeaf.getName();
			if (treeLeaf.getTyp() == TreeLeaf.TreeLeafType.LEAF) {
				// 葉ノードには、JMenuItemを設定する.
				@SuppressWarnings("unchecked")
				List<PartsSet> leafValue = (List<PartsSet>) entry.getValue();
				for (final PartsSet partsSet : leafValue) {
					JMenuItem favoriteMenu = favMenuItemBuilder
							.createFavoriteMenuItem(name, partsSet);
					menuItems.add(favoriteMenu);
				}

			} else if (treeLeaf.getTyp() == TreeLeaf.TreeLeafType.NODE) {
				// 枝ノードは、サブメニューを作成し、子ノードを設定する
				@SuppressWarnings("unchecked")
				TreeMap<TreeLeaf, Object> childNode = (TreeMap<TreeLeaf, Object>) entry
						.getValue();
				JMenu subMenu = favMenuItemBuilder.createSubMenu(name);
				menuItems.add(subMenu);
				ArrayList<JMenuItem> subMenuItems = new ArrayList<JMenuItem>();
				buildFavoritesMenuItems(subMenuItems, favMenuItemBuilder, childNode);
				for (JMenuItem subMenuItem : subMenuItems) {
					subMenu.add(subMenuItem);
				}

			} else {
				throw new RuntimeException("unknown type: " + treeLeaf);
			}
		}
	}

	/**
	 * お気に入りのJMenuItemを作成するファンクションオブジェクト
	 */
	private FavoriteMenuItemBuilder favMenuItemBuilder = new FavoriteMenuItemBuilder() {
		private MenuBuilder menuBuilder = new MenuBuilder();

		/**
		 * お気に入りメニューの作成
		 */
		public JMenuItem createFavoriteMenuItem(final String name,
				final PartsSet partsSet) {
			JMenuItem favoriteMenu = menuBuilder.createJMenuItem();
			favoriteMenu.setName(partsSet.getPartsSetId());
			favoriteMenu.setText(name);
			if (partsSet.isPresetParts()) {
				Font font = favoriteMenu.getFont();
				favoriteMenu.setFont(font.deriveFont(Font.BOLD));
			}
			favoriteMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectPresetParts(partsSet);
				}
			});

			// メニューアイテム上でマウスホイールを動かした場合は上下にスクロールさせる.
			// (ただし、OSXのスクリーンメニュー使用時は無視する.)
			addMouseWheelListener(favoriteMenu);

			return favoriteMenu;
		}

		/**
		 * サブメニューの作成
		 */
		public JMenu createSubMenu(String name) {
			JMenu menu = menuBuilder.createJMenu();
			menu.setText(name);

			// メニューアイテム上でマウスホイールを動かした場合は上下にスクロールさせる.
			// (ただし、OSXのスクリーンメニュー使用時は無視する.)
			addMouseWheelListener(menu);

			return menu;
		}

		/**
		 * メニューアイテム上でホイールを上下させたときにメニューをスクロールさせるためのホイールハンドラを設定する.
		 *
		 * @param favoriteMenu
		 */
		protected void addMouseWheelListener(final JMenuItem favoriteMenu) {
			if (JScrollableMenu.isScreenMenu()) {
				return;
			}
			favoriteMenu.addMouseWheelListener(new MouseWheelListener() {
				public void mouseWheelMoved(MouseWheelEvent e) {
					int rotation = e.getWheelRotation();
					JPopupMenu popupMenu = (JPopupMenu) favoriteMenu
							.getParent();
					JMenu parentMenu = (JMenu) popupMenu.getInvoker();
					if (parentMenu != null
							&& parentMenu instanceof JScrollableMenu) {
						final JScrollableMenu favMenu = (JScrollableMenu) parentMenu;
						favMenu.doScroll(rotation < 0);
					}
					e.consume();
				}
			});
		}
	};

	/**
	 * お気に入りメニューが開いたとき
	 *
	 * @param menu
	 */
	protected void onSelectedFavoriteMenu(JMenu menu) {
		// 表示順にソート
		List<PartsSet> partssets = getPartsSetList();
		TreeMap<TreeLeaf, Object> favTree = buildFavoritesItemTree(partssets);

		// メニューの再構築
		ArrayList<JMenuItem> favoritesMenuItems = new ArrayList<JMenuItem>();
		buildFavoritesMenuItems(favoritesMenuItems, favMenuItemBuilder, favTree);

		if (menu instanceof JScrollableMenu) {
			// スクロールメニューの場合
			JScrollableMenu favMenu = (JScrollableMenu) menu;

			// スクロールメニューの初期化
			favMenu.initScroller();

			// スクロールメニューアイテムの設定
			favMenu.setScrollableItems(favoritesMenuItems);

			// 高さを補正する
			// お気に入りメニューが選択された場合、
			// お気に入りアイテム一覧を表示するよりも前に
			// 表示可能なアイテム数を現在のウィンドウの高さから算定する.
			Toolkit tk = Toolkit.getDefaultToolkit();
			Dimension scrsiz = tk.getScreenSize();
			int height = scrsiz.height; // MainFrame.this.getHeight();
			favMenu.adjustMaxVisible(height);
			logger.log(Level.FINE,
					"scrollableMenu maxVisible=" + favMenu.getMaxVisible());

		} else {
			// 通常メニューの場合
			// 既存メニューの位置をセパレータより判断する.
			int mx = menu.getMenuComponentCount();
			int separatorIdx = -1;
			for (int idx = 0; idx < mx; idx++) {
				Component item = menu.getMenuComponent(idx);
				if (item instanceof JSeparator) {
					separatorIdx = idx;
					break;
				}
			}
			// 既存メニューの削除
			if (separatorIdx > 0) {
				while (menu.getMenuComponentCount() > separatorIdx + 1) {
					menu.remove(separatorIdx + 1);
				}
			}

			// お気に入りアイテムのメニューを登録する.
			for (JMenuItem menuItem : favoritesMenuItems) {
				menu.add(menuItem);
			}
		}

	}

	/**
	 * ヘルプメニューを開いたときにお勧めメニューを構築する.
	 *
	 * @param menu
	 */
	protected void onSelectedRecommendationMenu(JMenu mnuRecomendation) {
		// 現在のお勧めメニューを一旦削除
		while (mnuRecomendation.getMenuComponentCount() > 0) {
			mnuRecomendation.remove(0);
		}

		// お勧めリンクの定義がない場合はデフォルトを用いる.(明示的な空の場合は何もしない.)
		CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
		persist.compensateRecommendationList(characterData);

		// お勧めリンクメニューを作成する.
		List<RecommendationURL> recommendations = characterData.getRecommendationURLList();
		if (recommendations != null) {
			MenuBuilder menuBuilder = new MenuBuilder();
			for (RecommendationURL recommendation : recommendations) {
				String displayName = recommendation.getDisplayName();
				String url = recommendation.getUrl();

				JMenuItem mnuItem = menuBuilder.createJMenuItem();
				mnuItem.setText(displayName);
				mnuItem.addActionListener(
						DesktopUtilities.createBrowseAction(MainFrame.this, url, displayName)
						);
				mnuRecomendation.add(mnuItem);
			}
		}

		// お勧めリンクメニューのリストがnullでなく空でもない場合は有効、そうでなければ無効にする.
		mnuRecomendation.setEnabled(recommendations != null && !recommendations.isEmpty());
	}


	/**
	 * 最後に選択されたお気に入りと同じ構成であれば、 このお気に入りの名前をプレビューペインのタイトルに設定する.<br>
	 * そうでなければデフォルトのパーツセット名(no titleとか)を表示する.<br>
	 * 色情報が異なる場合に末尾に「*」マークがつけられる.<br>
	 *
	 * @param requestPartsSet
	 *            表示するパーツセット(名前は設定されていなくて良い。お気に入り側を使うので。), nullの場合はデフォルトのパーツ名
	 */
	protected void showPresetName(PartsSet requestPartsSet) {
		String title = getSuggestPartsSetName(requestPartsSet, true);
		if (title == null) {
			title = defaultPartsSetTitle;
		}
		previewPane.setTitle(title);
	}

	/**
	 * パーツセット名を推定する.<br>
	 * 最後に選択されたお気に入りと同じ構成であれば、 このお気に入りの名前を返す.<br>
	 * お気に入りが選択されていないか構成が異なる場合、お気に入りに名前がない場合はnullを返す.<br>
	 *
	 * @param requestPartsSet
	 *            表示するパーツセット(名前は設定されていなくて良い。お気に入り側を使うので。)
	 * @param markColorChange
	 *            色情報が異なる場合に末尾に「*」マークをつける場合はtrue
	 */
	private String getSuggestPartsSetName(PartsSet requestPartsSet, boolean markColorChange) {
		String partsSetTitle = null;
		if (lastUsePresetParts != null &&
				PartsSet.isSameStructure(requestPartsSet, lastUsePresetParts)) {
			partsSetTitle = lastUsePresetParts.getLocalizedName();
			if (markColorChange && !PartsSet.isSameColor(requestPartsSet, lastUsePresetParts)) {
				if (partsSetTitle != null) {
					partsSetTitle += "*";
				}
			}
		}
		if (partsSetTitle != null && partsSetTitle.trim().length() > 0) {
			return partsSetTitle;
		}
		return null;
	}

	/**
	 * プレビューの更新を要求する. 更新は非同期に行われる.
	 */
	protected void requestPreview() {
		if (!characterData.isValid()) {
			return;
		}

		// 選択されているパーツの各イメージを取得しレイヤー順に並び替えて合成する.
		// 合成は別スレッドにて非同期に行われる.
		// リクエストは随時受け付けて、最新のリクエストだけが処理される.
		// (処理がはじまる前に新しいリクエストで上書きされた場合、前のリクエストは単に捨てられる.)
		imageBuilder.requestJob(new ImageBuildJobAbstractAdaptor(characterData) {

					/**
					 * 構築するパーツセット情報
					 */
			private PartsSet requestPartsSet;

					/**
					 * 非同期のイメージ構築要求の番号.<br>
					 */
			private long ticket;

			@Override
			public void onQueueing(long ticket) {
				this.ticket = ticket;
				previewPane.setLoadingRequest(ticket);
			}
			@Override
			public void buildImage(ImageOutput output) {
						// 合成結果のイメージを引数としてイメージビルダから呼び出される.
				final BufferedImage img = output.getImageOutput();
				Runnable refreshJob = new Runnable() {
					public void run() {
						previewPane.setPreviewImage(img);
						previewPane.setLoadingComplete(ticket);
						showPresetName(requestPartsSet);
					}
				};
				if (SwingUtilities.isEventDispatchThread()) {
					refreshJob.run();
				} else {
					try {
						SwingUtilities.invokeAndWait(refreshJob);
					} catch (Exception ex) {
						logger.log(Level.WARNING, "build image failed.", ex);
					}
				}
			}
			@Override
			public void handleException(final Throwable ex) {
						// 合成中に例外が発生した場合、イメージビルダから呼び出される.
				Runnable showExceptionJob = new Runnable() {
					public void run() {
						ErrorMessageHelper.showErrorDialog(MainFrame.this, ex);
					}
				};
				if (SwingUtilities.isEventDispatchThread()) {
					showExceptionJob.run();
				} else {
					SwingUtilities.invokeLater(showExceptionJob);
				}
			}
			@Override
			protected PartsSet getPartsSet() {
						// 合成できる状態になった時点でイメージビルダから呼び出される.
				final PartsSet[] result = new PartsSet[1];
				Runnable collectPartsSetJob = new Runnable() {
					public void run() {
						PartsSet partsSet = partsSelectionManager.createPartsSet();
						result[0] = partsSet;
					}
				};
				if (SwingUtilities.isEventDispatchThread()) {
					collectPartsSetJob.run();
				} else {
					try {
								// スレッドによるSwingのイベントディスパッチスレッド以外からの呼び出しの場合、
								// Swingディスパッチスレッドでパーツの選択状態を取得する.
						SwingUtilities.invokeAndWait(collectPartsSetJob);

					} catch (InvocationTargetException e) {
						throw new RuntimeException(e.getMessage(), e);
					} catch (InterruptedException e) {
						throw new RuntimeException("interrupted:" + e, e);
					}
				}
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "preview: " + result[0]);
				}
				requestPartsSet = result[0];
				return requestPartsSet;
			}
		});
	}

	/**
	 * プロファイルを開く
	 */
	protected void onOpenProfile() {
		try {
			MainFrame main2 = ProfileListManager.openProfile(this);
			if (main2 != null) {
				main2.showMainFrame();
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * 背景色を変更する.
	 */
	protected void onChangeBgColor() {
		getJMenuBar().setEnabled(false);
		try {
			Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
					.getLocalizedProperties(STRINGS_RESOURCE);

			Color color = wallpaperInfo.getBackgroundColor();
			color = JColorChooser.showDialog(this, strings.getProperty("chooseBgColor"), color);
			if (color != null) {
				applyBackgroundColorOnly(color);
			}
		} finally {
			getJMenuBar().setEnabled(true);
		}
	}

	/**
	 * 壁紙を変更する.
	 */
	protected void onChangeWallpaper() {
		try {
			WallpaperDialog wallpaperDialog = new WallpaperDialog(this);

			// 最後に使用した壁紙情報でダイアログを設定する.
			wallpaperDialog.setWallpaperInfo(this.wallpaperInfo);

			// 壁紙情報を設定するモーダルダイアログを開く
			WallpaperInfo wallpaperInfo = wallpaperDialog.showDialog();
			if (wallpaperInfo == null) {
				return;
			}

			// 壁紙情報を保存し、その情報をもとに背景を再描画する.
			applyWallpaperInfo(wallpaperInfo, false);

		} catch (WallpaperFactoryException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);

		} catch (RuntimeException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * 背景色のみ変更し、背景を再描画する.<br>
	 * 壁紙情報全体の更新よりも効率化するためのメソッドである.<br>
	 *
	 * @param bgColor
	 *            背景色
	 */
	protected void applyBackgroundColorOnly(Color bgColor) {
		wallpaperInfo.setBackgroundColor(bgColor);
		previewPane.getWallpaper()
			.setBackgroundColor(wallpaperInfo.getBackgroundColor());
	}

	/**
	 * 壁紙情報を保存し、その情報をもとに背景を再描画する.<br>
	 * ignoreErrorがtrueである場合、適用に失敗した場合はログに記録するのみで、 壁紙情報は保存されず、壁紙も更新されない.<br>
	 *
	 * @param wallpaperInfo
	 *            壁紙情報、null不可
	 * @param ignoreError
	 *            失敗を無視する場合
	 * @throws IOException
	 *             失敗
	 */
	protected void applyWallpaperInfo(WallpaperInfo wallpaperInfo, boolean ignoreError) throws WallpaperFactoryException {
		if (wallpaperInfo == null) {
			throw new IllegalArgumentException();
		}
		// 壁紙情報から壁紙インスタンスを生成する.
		WallpaperFactory wallpaperFactory = WallpaperFactory.getInstance();
		Wallpaper wallpaper = null;

		try {
			// 壁紙情報の構築時に問題が発生した場合、
			// 回復処理をして継続するかエラーとするか?
			WallpaperFactoryErrorRecoverHandler handler = null;
			if (ignoreError) {
				handler = new WallpaperFactoryErrorRecoverHandler();
			}

			// 壁紙情報
			wallpaper = wallpaperFactory.createWallpaper(wallpaperInfo, handler);

		} catch (WallpaperFactoryException ex) {
			logger.log(Level.WARNING, "壁紙情報の適用に失敗しました。", ex);
			if ( !ignoreError) {
				throw ex;
			}

		} catch (RuntimeException ex) {
			logger.log(Level.WARNING, "壁紙情報の適用に失敗しました。", ex);
			if ( !ignoreError) {
				throw ex;
			}
		}

		if (wallpaper == null) {
			return;
		}

		// 壁紙を更新する.
		previewPane.setWallpaper(wallpaper);

		// 壁紙情報として記憶する.
		this.wallpaperInfo = wallpaperInfo;
	}

	/**
	 * プリビューしている画像をファイルに保存する。 サポートしているのはPNG/JPEGのみ。
	 */
	protected void onSavePicture() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		BufferedImage img = previewPane.getPreviewImage();
		Color imgBgColor = wallpaperInfo.getBackgroundColor();
		if (img == null) {
			tk.beep();
			return;
		}

		try {
			// 出力オプションの調整
			OutputOption outputOption = imageSaveHelper.getOutputOption();
			outputOption.setZoomFactor(previewPane.getZoomFactor());
			outputOption.changeRecommend();
			imageSaveHelper.setOutputOption(outputOption);

			// ファイルダイアログ表示
			File outFile = imageSaveHelper.showSaveFileDialog(this);
			if (outFile == null) {
				return;
			}
			logger.log(Level.FINE, "savePicture: " + outFile);
			logger.log(Level.FINE, "outputOption: " + outputOption);

			// 画像のファイルへの出力
			StringBuilder warnings = new StringBuilder();

			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				imageSaveHelper.savePicture(img, imgBgColor, outFile, warnings);

			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
			if (warnings.length() > 0) {
				JOptionPane.showMessageDialog(this, warnings.toString(), "WARNINGS", JOptionPane.WARNING_MESSAGE);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * 伺か用PNG/PNAの出力.
	 */
	protected void onSaveAsUkagaka() {
		BufferedImage img = previewPane.getPreviewImage();
		Color bgColor = wallpaperInfo.getBackgroundColor();
		if (img == null) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		try {
			ukagakaImageSaveHelper.save(this, img, bgColor);

		} catch (IOException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * 伺か用PNG/PNAの変換
	 */
	protected void onConvertUkagaka() {
		try {
			Color colorKey = wallpaperInfo.getBackgroundColor();
			ukagakaImageSaveHelper.convertChooseFiles(this, colorKey);

		} catch (IOException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * プロファイルの場所を開く
	 */
	protected void onBrowseProfileDir() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		try {
			DesktopUtilities.browseBaseDir(characterData.getDocBase());

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * このプロファイルを編集する.
	 */
	protected void onEditProfile() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		try {
			CharacterData cd = this.characterData;
			CharacterData newCd = ProfileListManager.editProfile(this, cd);
			if (newCd != null) {
				CharacterDataChangeObserver.getDefault()
						.notifyCharacterDataChange(this, newCd, true, true);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * パーツの管理ダイアログを開く.<br>
	 */
	protected void onManageParts() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		PartsManageDialog mrgDlg = new PartsManageDialog(this, characterData);
		mrgDlg.setVisible(true);

		if (mrgDlg.isUpdated()) {
			// パーツ管理情報が更新された場合、
			// パーツデータをリロードする.
			if (characterData.reloadPartsData()) {
				partsSelectionManager.loadParts();
				requestPreview();
			}
		}
	}

	/**
	 * 「パーツ検索」ダイアログを開く.<br>
	 * すでに開いているダイアログがあれば、それにフォーカスを当てる.<br>
	 */
	protected void openSearchDialog() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		if (lastUseSearchPartsDialog != null) {
			// 開いているダイアログがあれば、それにフォーカスを当てる.
			if (lastUseSearchPartsDialog.isDisplayable() && lastUseSearchPartsDialog.isVisible()) {
				lastUseSearchPartsDialog.requestFocus();
				return;
			}
		}

		SearchPartsDialog searchPartsDlg = new SearchPartsDialog(this, characterData, partsSelectionManager);
		WindowAdjustLocationSupport.alignRight(this, searchPartsDlg, 0, true);
		searchPartsDlg.setVisible(true);
		lastUseSearchPartsDialog = searchPartsDlg;
	}

	/**
	 * 「パーツ検索」ダイアログを閉じる.<br>
	 */
	protected void closeSearchDialog() {
		lastUseSearchPartsDialog = null;
		for (SearchPartsDialog dlg : SearchPartsDialog.getDialogs()) {
			if (dlg != null && dlg.isDisplayable() && dlg.getParent() == this) {
				dlg.dispose();
			}
		}
	}

	/**
	 * 「お気に入りの管理」ダイアログを閉じる
	 */
	protected void closeManageFavoritesDialog() {
		if (lastUseManageFavoritesDialog != null) {
			if (lastUseManageFavoritesDialog.isDisplayable()) {
				lastUseManageFavoritesDialog.dispose();
			}
			lastUseManageFavoritesDialog = null;
		}
	}

	/**
	 * 「パーツのランダム選択ダイアログ」を閉じる
	 */
	protected void closePartsRandomChooserDialog() {
		if (lastUsePartsRandomChooserDialog != null) {
			if (lastUsePartsRandomChooserDialog.isDisplayable()) {
				lastUsePartsRandomChooserDialog.dispose();
			}
			lastUsePartsRandomChooserDialog = null;
		}
	}

	/**
	 * クリップボードにコピー
	 *
	 * @param screenImage
	 *            スクリーンイメージ
	 */
	protected void onCopy(boolean screenImage) {
		try {
			BufferedImage img = previewPane.getPreviewImage();
			if (img == null) {
				Toolkit tk = Toolkit.getDefaultToolkit();
				tk.beep();
				return;
			}

			if (screenImage) {
				// 表示している内容をそのままコピーする.
				img = previewPane.getScreenImage();
			}

			Color imgBgColor = wallpaperInfo.getBackgroundColor();
			ClipboardUtil.setImage(img, imgBgColor);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * アプリケーションの設定ダイアログを開く
	 */
	protected void onPreferences() {
		AppConfigDialog appConfigDlg = new AppConfigDialog(this);
		appConfigDlg.setVisible(true);
	}

	/**
	 * 新規モードでインポートウィザードを実行する.<br>
	 */
	protected void onImportNew() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		try {
			// インポートウィザードの実行(新規モード)
			ImportWizardDialog importWizDialog = new ImportWizardDialog(this, null, null);
			importWizDialog.setVisible(true);
			int exitCode = importWizDialog.getExitCode();
			if (exitCode == ImportWizardDialog.EXIT_PROFILE_CREATED) {
				CharacterData cd = importWizDialog.getImportedCharacterData();
				if (cd != null && cd.isValid()) {
					// インポートしたキャラクターデータのプロファイルを開く.
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						MainFrame mainFrame = ProfileListManager.openProfile(cd);
						mainFrame.setVisible(true);

					} finally {
						setCursor(Cursor.getDefaultCursor());
					}
				}
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * 現在のプロファイルに対するインポートウィザードを実行する.<br>
	 * インポートが実行された場合は、パーツをリロードする.<br>
	 * インポートウィザード表示中は監視スレッドは停止される.<br>
	 *
	 * @param initFile
	 *            アーカイブファィルまたはディレクトリ、指定がなければnull
	 */
	protected void onImport(List<File> initFiles) {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		try {
			watchAgent.suspend();
			try {
				// インポートウィザードの実行
				ImportWizardDialog importWizDialog = new ImportWizardDialog(this, characterData, initFiles);
				importWizDialog.setVisible(true);

				if (importWizDialog.getExitCode() == ImportWizardDialog.EXIT_PROFILE_UPDATED) {
					CharacterData importedCd = importWizDialog.getImportedCharacterData();
					CharacterDataChangeObserver.getDefault()
							.notifyCharacterDataChange(this, importedCd,
									false, true);
				}

			} finally {
				watchAgent.resume();
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * パーツとお気に入りをリロードする.<br>
	 * まだロードされていない場合はあらたにロードする.<br>
	 * 引数newCdが指定されている場合は、現在のキャラクター定義の説明文を更新する.<br>
	 * (説明文の更新以外には使用されない.)<br>
	 *
	 * @param newCd
	 *            説明文更新のための更新されたキャラクターデータを指定する。null可
	 * @param forceRepaint
	 *            必ず再描画する場合
	 * @throws IOException
	 *             失敗
	 */
	protected synchronized void reloadPartsAndFavorites(CharacterData newCd,
			boolean forceRepaint) throws IOException {
		if (newCd != null) {
			// (インポート画面では説明文のみ更新するので、それだけ取得)
			characterData.setDescription(newCd.getDescription());
		}

		if ( !characterData.isPartsLoaded()) {
			// キャラクターデータが、まだ読み込まれていなければ読み込む.
			ProfileListManager.loadCharacterData(characterData);
			ProfileListManager.loadFavorites(characterData);
			partsSelectionManager.loadParts();

		} else {
			// パーツデータをリロードする.
			if (characterData.reloadPartsData()) {
				partsSelectionManager.loadParts();
			}

			// お気に入りをリロードする.
			CharacterDataPersistent persiste = CharacterDataPersistent.getInstance();
			persiste.loadFavorites(characterData);

			// お気に入りが更新されたことを通知する.
			FavoritesChangeObserver.getDefault().notifyFavoritesChange(
					MainFrame.this, characterData);
		}

		// 現在選択されているパーツセットがない場合はデフォルトのパーツセットを選択する.
		if (showDefaultParts(false) || forceRepaint) {
			requestPreview();
		}
	}

	protected void onExport() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		ExportWizardDialog exportWizDlg = new ExportWizardDialog(this, characterData, previewPane.getPreviewImage());
		exportWizDlg.setVisible(true);
	}

	protected void onResetColor() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		if (JOptionPane.showConfirmDialog(this, strings.get("confirm.resetcolors"), strings.getProperty("confirm"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
			return;
		}
		characterData.getPartsColorManager().resetPartsColorInfo();
		partsColorCoordinator.initColorDialog();
		requestPreview();
	}

	/**
	 * プロファイルを閉じる.
	 */
	protected void onCloseProfile() {
		saveWorkingSet();
		ProfileListManager.unregisterUsedCharacterData(characterData);

		if (characterData.isValid()) {

			// 最後に使用したキャラクターデータとして記憶する.
			try {
				RecentDataPersistent recentPersist = RecentDataPersistent.getInstance();
				recentPersist.saveRecent(characterData);

			} catch (Exception ex) {
				logger.log(Level.WARNING, "recent data saving failed.", ex);
				// recent情報の記録に失敗しても致命的ではないので、これは無視する.
			}
		}

		// イメージビルダスレッド・ディレクトリ監視スレッドを停止する.
		stopAgents();

		// フレームウィンドウを破棄する.
		dispose();

		// 破棄されたことをロギングする.
		logger.log(Level.FINE, "dispose mainframe.");
	}

	/**
	 * 開いている、すべてのプロファイルを閉じる.<br>
	 * (Mac OS Xのcmd+Qで閉じる場合などで使用される.)<br>
	 */
	public static void closeAllProfiles() {
		// ウィンドウが閉じられることでアクティブなフレームが切り替わる場合を想定し、
		// 現在のアクティブなウィンドウをあらかじめ記憶しておく
		MainFrame mainFrame = activedMainFrame;

		// gcをかけてファイナライズを促進させる
		SystemUtil.gc();

		// ファイナライズされていないFrameのうち、ネイティブリソースと関連づけられている
		// フレームについて、それがMainFrameのインスタンスであれば閉じる.
		// ただし、現在アクティブなものは除く
		for (Frame frame : JFrame.getFrames()) {
			try {
				if (frame.isDisplayable()) {
					// ネイティブリソースと関連づけられているフレーム
					if (frame instanceof MainFrame && frame != mainFrame) {
						// MainFrameのインスタンスであるので閉じる処理が可能.
						// (現在アクティブなメインフレームは最後に閉じるため、ここでは閉じない.)
						((MainFrame) frame).onCloseProfile();
					}
				}

			} catch (Throwable ex) {
				logger.log(Level.SEVERE, "mainframe closing failed.", ex);
				// フレームを閉じるときに失敗した場合、通常、致命的問題だが
				// クローズ処理は継続しなければならない.
			}
		}

		// 現在アクティブなフレームを閉じる.
		// 最後に閉じることで「最後に使ったプロファイル」として記憶させる.
		if (activedMainFrame != null && activedMainFrame.isDisplayable()) {
			try {
				activedMainFrame.onCloseProfile();

			} catch (Throwable ex) {
				logger.log(Level.SEVERE, "mainframe closing failed.", ex);
				// フレームを閉じるときに失敗した場合、通常、致命的問題だが
				// クローズ処理は継続しなければならない.
			}
		}
	}

	/**
	 * 画面の作業状態を保存する.
	 */
	protected void saveWorkingSet() {
		if (!characterData.isValid()) {
			return;
		}
		try {
			// ワーキングセットの作成
			WorkingSet workingSet = new WorkingSet();
			workingSet.setCharacterDocBase(characterData.getDocBase());
			workingSet.setCharacterDataRev(characterData.getRev());
			PartsSet partsSet = partsSelectionManager.createPartsSet();
			workingSet.setPartsSet(partsSet);
			workingSet.setPartsColorInfoMap(characterData
					.getPartsColorManager().getPartsColorInfoMap());
			workingSet.setLastUsedSaveDir(imageSaveHelper.getLastUsedSaveDir());
			workingSet.setLastUsedExportDir(ExportWizardDialog.getLastUsedDir());
			workingSet.setLastUsePresetParts(lastUsePresetParts);
			workingSet
					.setCharacterData(characterData.duplicateBasicInfo(false)); // パーツセットは保存しない.
			workingSet.setWallpaperInfo(wallpaperInfo);

			// XML形式でのワーキングセットの保存
			WorkingSetPersist workingSetPersist = WorkingSetPersist
					.getInstance();
			workingSetPersist.saveWorkingSet(workingSet);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * 画面の作業状態を復元する.
	 *
	 * @return ワーキングセットを読み込んだ場合はtrue、そうでなければfalse
	 */
	protected boolean loadWorkingSet() {
		if (!characterData.isValid()) {
			return false;
		}
		try {
			WorkingSetPersist workingSetPersist = WorkingSetPersist
					.getInstance();
			WorkingSet2 workingSet2 = workingSetPersist
					.loadWorkingSet(characterData);
			if (workingSet2 == null) {
				// ワーキングセットがない場合.
				return false;
			}

			URI docBase = characterData.getDocBase();
			if (docBase != null
					&& !docBase.equals(workingSet2.getCharacterDocBase())) {
				// docBaseが一致せず
				return false;
			}
			String sig = characterData.toSignatureString();
			if (!sig.equals(workingSet2.getCharacterDataSig())) {
				// 構造が一致せず.
				return false;
			}

			// パーツの色情報を復元する.
			Map<PartsIdentifier, PartsColorInfo> partsColorInfoMap = characterData
					.getPartsColorManager().getPartsColorInfoMap();
			workingSet2.createCompatible(characterData, partsColorInfoMap);

			// 選択されているパーツの復元
			IndependentPartsSetInfo partsSetInfo = workingSet2
					.getCurrentPartsSet();
			if (partsSetInfo != null) {
				PartsSet partsSet = IndependentPartsSetInfo.convertPartsSet(
						partsSetInfo, characterData, false);
				selectPresetParts(partsSet);

				// 最後に選択したお気に入り情報の復元
				IndependentPartsSetInfo lastUsePresetPartsInfo = workingSet2
						.getLastUsePresetParts();
				if (lastUsePresetPartsInfo != null
						&& lastUsePresetPartsInfo.getId() != null
						&& lastUsePresetPartsInfo.getId().trim().length() > 0) {
					PartsSet lastUsePresetParts = IndependentPartsSetInfo
							.convertPartsSet(lastUsePresetPartsInfo,
									characterData, false);
					if (lastUsePresetParts.isSameStructure(partsSet)) {
						this.lastUsePresetParts = lastUsePresetParts;
						showPresetName(lastUsePresetParts);
					}
				}
			}

			// 最後に保存したディレクトリを復元する.
			imageSaveHelper.setLastUseSaveDir(workingSet2.getLastUsedSaveDir());
			ExportWizardDialog.setLastUsedDir(workingSet2
					.getLastUsedExportDir());

			// 壁紙情報を取得する.
			WallpaperInfo wallpaperInfo = workingSet2.getWallpaperInfo();
			if (wallpaperInfo != null) {
				// 壁紙情報を保存し、その情報をもとに背景を再描画する.
				// (適用に失敗した場合はエラーは無視し、壁紙情報は保存しない.)
				applyWallpaperInfo(wallpaperInfo, true);
			}
			return true;

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
		return false;
	}


	protected void onAbout() {
		try {
			AboutBox aboutBox = new AboutBox(this);
			aboutBox.showAboutBox();

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	protected void onHelp() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(STRINGS_RESOURCE);
		String helpURL = strings.getProperty("help.url");
		String helpDescription = strings.getProperty("help.show");
		DesktopUtilities.browse(this, helpURL, helpDescription);
	}

	protected void onFlipHolizontal() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		double[] affineTransformParameter = partsSelectionManager.getAffineTransformParameter();
		if (affineTransformParameter == null) {
			// 左右フリップするアフィン変換パラメータを構築する.
			Dimension siz = characterData.getImageSize();
			if (siz != null) {
				affineTransformParameter = new double[] {-1., 0, 0, 1., siz.width, 0};
			}
		} else {
			// アフィン変換パラメータをクリアする.
			affineTransformParameter = null;
		}
		partsSelectionManager.setAffineTransformParameter(affineTransformParameter);
		requestPreview();
	}

	protected void onSetDefaultPicture() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		try {
			BufferedImage samplePicture = previewPane.getPreviewImage();
			if (samplePicture != null) {
				CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
				persist.saveSamplePicture(characterData, samplePicture);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	protected void onInformation() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		PartsSet partsSet = partsSelectionManager.createPartsSet();
		InformationDialog infoDlg = new InformationDialog(this, characterData, partsSet);
		infoDlg.setVisible(true);
	}

	protected void onManageFavorites() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		if (lastUseManageFavoritesDialog != null) {
			// 開いているダイアログがあれば、それにフォーカスを当てる.
			if (lastUseManageFavoritesDialog.isDisplayable()
					&& lastUseManageFavoritesDialog.isVisible()) {
				lastUseManageFavoritesDialog.requestFocus();
				return;
			}
		}

		// お気に入り編集ダイアログを開く
		ManageFavoriteDialog dlg = new ManageFavoriteDialog(this, characterData);
		dlg.setFavoriteManageCallback(new FavoriteManageCallback() {

			public void selectFavorites(PartsSet partsSet) {
				// お気に入り編集ダイアログで選択されたパーツを選択表示する.
				selectPresetParts(partsSet);
			}

			public void updateFavorites(CharacterData characterData,
					boolean savePreset) {
				// お気に入りを登録する.
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						CharacterDataPersistent persiste = CharacterDataPersistent
								.getInstance();
						if (savePreset) {
							persiste.updateProfile(characterData);
						}

						persiste.saveFavorites(characterData);

						// お気に入りが更新されたことを通知する.
						FavoritesChangeObserver.getDefault()
								.notifyFavoritesChange(MainFrame.this,
										characterData);

					} finally {
						setCursor(Cursor.getDefaultCursor());
					}

				} catch (Exception ex) {
					ErrorMessageHelper.showErrorDialog(MainFrame.this, ex);
				}
			}
		});
		WindowAdjustLocationSupport.alignRight(this, dlg, 0, true);
		dlg.setVisible(true);
		lastUseManageFavoritesDialog = dlg;
	}

	protected void onRegisterFavorite() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		try {
			// パーツセットを生成
			PartsSet partsSet = partsSelectionManager.createPartsSet();
			if (partsSet.isEmpty()) {
				// 空のパーツセットは登録しない.
				return;
			}

			Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
					.getLocalizedProperties(STRINGS_RESOURCE);

			// お気に入りに登録するパーツセットが最後に使用したお気に入りと同じ構成であれば、
			// そのお気に入り名を使用する.
			String initName = getSuggestPartsSetName(partsSet, false);

			// カラー情報の有無のチェックボックス.
			JCheckBox chkColorInfo = new JCheckBox(strings.getProperty("input.favoritesColorInfo"));
			chkColorInfo.setSelected(true);
			String partsSetId = null;
			if (initName != null && lastUsePresetParts != null) {
				partsSetId = lastUsePresetParts.getPartsSetId();
			}

			// 上書き保存の可否のチェックボックス
			JCheckBox chkOverwrite = new JCheckBox(strings.getProperty("input.favoritesOverwrite"));
			chkOverwrite.setSelected(partsSetId != null && partsSetId.length() > 0);
			chkOverwrite.setEnabled(partsSetId != null && partsSetId.length() > 0);

			// チェックボックスパネル
			Box checkboxsPanel = new Box(BoxLayout.PAGE_AXIS);
			checkboxsPanel.add(chkColorInfo);
			checkboxsPanel.add(chkOverwrite);

			// 入力ダイアログを開く
			String name = (String) JOptionPane.showInputDialog(this,
					checkboxsPanel,
					strings.getProperty("input.favorites"),
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					initName == null ? "" : initName);
			if (name == null || name.trim().length() == 0) {
				return;
			}

			boolean includeColorInfo = chkColorInfo.isSelected();
			if (!includeColorInfo) {
				// カラー情報を除去する.
				partsSet.removeColorInfo();
			}

			// 新規の場合、もしくは上書きしない場合はIDを設定する.
			if (partsSetId == null || !chkOverwrite.isSelected()) {
				partsSetId = "ps" + UUID.randomUUID().toString();
			}
			partsSet.setPartsSetId(partsSetId);

			// 名前を設定する.
			partsSet.setLocalizedName(name);

			// ファイルに保存
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				CharacterDataPersistent persiste = CharacterDataPersistent.getInstance();
				// 現在の最新情報を取り出す.
				characterData.clearPartsSets(true);
				persiste.loadFavorites(characterData);

				// お気に入りコレクションに登録
				characterData.addPartsSet(partsSet);

				persiste.saveFavorites(characterData);

				// お気に入りが更新されたことを通知する.
				FavoritesChangeObserver.getDefault().notifyFavoritesChange(
						MainFrame.this, characterData);

			} finally {
				setCursor(Cursor.getDefaultCursor());
			}

			// 最後に選択したお気に入りにする
			lastUsePresetParts = partsSet;
			showPresetName(partsSet);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * ランダム選択ダイアログを開く.
	 */
	protected void onToolRandom() {
		if (!characterData.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		if (lastUsePartsRandomChooserDialog != null) {
			// 開いているダイアログがあれば、それにフォーカスを当てる.
			if (lastUsePartsRandomChooserDialog.isDisplayable()
					&& lastUsePartsRandomChooserDialog.isVisible()) {
				lastUsePartsRandomChooserDialog.requestFocus();
				return;
			}
		}

		// お気に入り編集ダイアログを開く
		PartsRandomChooserDialog dlg = new PartsRandomChooserDialog(this,
				characterData,
				new PartsRandomChooserDialog.PartsSetSynchronizer() {
					public PartsSet getCurrentPartsSet() {
						// 現在のパーツセットを生成
						return partsSelectionManager.createPartsSet();
					}

					public void setPartsSet(PartsSet partsSet) {
						selectPresetParts(partsSet);
					}

					public boolean
							isExcludePartsIdentifier(PartsIdentifier partsIdentifier) {
						Boolean exclude = randomExcludePartsIdentifierMap
								.get(partsIdentifier);
						return exclude != null && exclude.booleanValue();
					}

					public void
							setExcludePartsIdentifier(PartsIdentifier partsIdentifier,
									boolean exclude) {
						randomExcludePartsIdentifierMap.put(partsIdentifier,
								exclude);
					}
				});

		WindowAdjustLocationSupport.alignRight(this, dlg, 0, true);
		dlg.setVisible(true);
		lastUsePartsRandomChooserDialog = dlg;
	}

	/**
	 * ランダム選択パーツで選択候補から除外するパーツのマップ.
	 */
	private HashMap<PartsIdentifier, Boolean> randomExcludePartsIdentifierMap =
			new HashMap<PartsIdentifier, Boolean>();

	/**
	 * すべての解除可能なパーツの選択を解除する。
	 */
	protected void onDeselectAll() {
		partsSelectionManager.deselectAll();
	}

	/**
	 * 単一選択カテゴリのパーツの解除を許可する。
	 */
	protected void onDeselectableAllCategory() {
		partsSelectionManager
				.setDeselectableSingleCategory( !partsSelectionManager
						.isDeselectableSingleCategory());
	}

	/**
	 * プレビューのズームボックスの表示制御
	 */
	protected void onEnableZoom() {
		previewPane.setVisibleZoomBox( !previewPane.isVisibleZoomBox());
	}

	/**
	 * メニューバーを構築します.
	 *
	 * @return メニューバー
	 */
	protected JMenuBar createMenuBar() {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		MenuDataFactory[] menus = new MenuDataFactory[] {
				new MenuDataFactory("menu.file", new MenuDataFactory[] {
						new MenuDataFactory("file.openProfile", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onOpenProfile();
							}
						}),
						new MenuDataFactory("file.savePicture", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onSavePicture();
							}
						}),
						new MenuDataFactory("file.ukagaka", new MenuDataFactory[] {
								new MenuDataFactory("file.saveAsUkagaka", new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										onSaveAsUkagaka();
									};
								}),
								new MenuDataFactory("file.convertUkagaka", new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										onConvertUkagaka();
									};
								}),
						}),
						null,
						new MenuDataFactory("file.editprofile", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onEditProfile();
							}
						}),
						new MenuDataFactory("file.opendir", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onBrowseProfileDir();
							}
						}),
						new MenuDataFactory("file.import", new MenuDataFactory[] {
								new MenuDataFactory("file.importMe", new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										onImport(null);
									};
								}),
								new MenuDataFactory("file.importNew", new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										onImportNew();
									};
								}),
						}),
						new MenuDataFactory("file.export", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onExport();
							};
						}),
						new MenuDataFactory("file.manageParts", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onManageParts();
							}
						}),
						new MenuDataFactory("file.preferences", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onPreferences();
							};
						}),
						null,
						new MenuDataFactory("file.closeProfile", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onCloseProfile();
							}
						}),
				}),
				new MenuDataFactory("menu.edit", new MenuDataFactory[] {
						new MenuDataFactory("edit.search", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								openSearchDialog();
							}
						}),
						new MenuDataFactory("edit.copy", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onCopy((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
							}
						}),
						new MenuDataFactory("edit.flipHorizontal", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onFlipHolizontal();
							}
						}),
						new MenuDataFactory("edit.resetcolor", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onResetColor();
							}
						}),
						null,
						new MenuDataFactory("edit.setDefaultPicture", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onSetDefaultPicture();
							}
						}),
						new MenuDataFactory("edit.information", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onInformation();
							}
						}),
						null,
						new MenuDataFactory("edit.deselectall", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onDeselectAll();
							}
						}),
						new MenuDataFactory("edit.deselectparts", true, new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onDeselectableAllCategory();
							}
						}),
						new MenuDataFactory("edit.enableAutoShrink", true, new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onClickPartsCategoryTitle(null, true);
							}
						}),
						null,
						new MenuDataFactory("edit.enableZoomBox", true, new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onEnableZoom();
							}
						}),
						null,
						new MenuDataFactory("edit.changeBgColor", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onChangeBgColor();
							}
						}),
						new MenuDataFactory("edit.changeWallpaper", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onChangeWallpaper();
							}
						}),
				}),
				new MenuDataFactory("menu.favorite", new MenuDataFactory[] {
						new MenuDataFactory("favorite.register", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onRegisterFavorite();
							}
						}),
						new MenuDataFactory("favorite.manage", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onManageFavorites();
							}
						}),
						null,
				}),
				new MenuDataFactory("menu.tool",
						new MenuDataFactory[]{new MenuDataFactory(
								"tool.random", new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										onToolRandom();
									}
								}),}),
				new MenuDataFactory("menu.help", new MenuDataFactory[] {
						new MenuDataFactory("help.recommendations", (ActionListener) null),
						null,
						new MenuDataFactory("help.help", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onHelp();
							}
						}),
						new MenuDataFactory("help.forum",
								DesktopUtilities.createBrowseAction(
										MainFrame.this,
										strings.getProperty("help.forum.url"),
										strings.getProperty("help.forum.description"))
						),
						new MenuDataFactory("help.bugreport",
								DesktopUtilities.createBrowseAction(
										MainFrame.this,
										strings.getProperty("help.reportbugs.url"),
										strings.getProperty("help.reportbugs.description"))
						),
						new MenuDataFactory("help.about", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								onAbout();
							}
						}),
				}), };

		final MenuBuilder menuBuilder = new MenuBuilder();

		JMenuBar menuBar = menuBuilder.createMenuBar(menus);

		menuBuilder.getJMenu("menu.edit").addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {
				// do nothing.
			}
			public void menuDeselected(MenuEvent e) {
				// do nothing.
			}
			public void menuSelected(MenuEvent e) {
				menuBuilder.getJMenuItem("edit.copy").setEnabled(previewPane.getPreviewImage() != null);
				menuBuilder.getJMenuItem("edit.deselectparts").setSelected(
						partsSelectionManager.isDeselectableSingleCategory());
				menuBuilder.getJMenuItem("edit.enableAutoShrink").setSelected(minimizeMode);
				menuBuilder.getJMenuItem("edit.enableZoomBox").setSelected(previewPane.isVisibleZoomBox());
			}
		});
		final JMenu mnuFavorites = menuBuilder.getJMenu("menu.favorite");
		mnuFavorites.addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {
				// do nothing.
			}
			public void menuDeselected(MenuEvent e) {
				// do nothing.
			}
			public void menuSelected(MenuEvent e) {
				onSelectedFavoriteMenu(mnuFavorites);
			}
		});

		// J2SE5の場合は「パーツディレクトリを開く」コマンドは使用不可とする.
		if (System.getProperty("java.version").startsWith("1.5")) {
			menuBuilder.getJMenuItem("file.opendir").setEnabled(false);
		}

		// お勧めサイトメニュー構築
		final JMenu mnuRecomendation = menuBuilder.getJMenu("help.recommendations");
		JMenu mnuHelp = menuBuilder.getJMenu("menu.help");
		mnuHelp.addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {
				// do nothing.
			}
			public void menuDeselected(MenuEvent e) {
				// do nothing.
			}
			public void menuSelected(MenuEvent e) {
				onSelectedRecommendationMenu(mnuRecomendation);
			}
		});

		return menuBar;
	}

}
