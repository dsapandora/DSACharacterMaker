package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import charactermanaj.Main;
import charactermanaj.graphics.io.PNGFileImageHeader;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsAuthorInfo;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.PartsSet;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.io.AbstractCharacterDataArchiveFile.CategoryLayerPair;
import charactermanaj.model.io.AbstractCharacterDataArchiveFile.PartsImageContent;
import charactermanaj.model.io.CharacterDataPersistent;
import charactermanaj.model.io.ImportModel;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.ui.progress.ProgressHandle;
import charactermanaj.ui.progress.Worker;
import charactermanaj.ui.progress.WorkerException;
import charactermanaj.ui.progress.WorkerWithProgessDialog;
import charactermanaj.ui.util.FileDropTarget;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;


/**
 * インポートウィザードダイアログ.<br>
 * 
 * @author seraphy
 */
public class ImportWizardDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	
	protected static final String STRINGS_RESOURCE = "languages/importwizdialog";
	

	public static final int EXIT_PROFILE_UPDATED = 1;
	
	public static final int EXIT_PROFILE_CREATED = 2;
	
	public static final int EXIT_CANCELED = 0;

	
	/**
	 * インポートウィザードの実行結果.<br>
	 */
	private int exitCode = EXIT_CANCELED;
	
	/**
	 * インポートされたキャラクターデータ.<br>
	 */
	private CharacterData importedCharacterData;


	/**
	 * 現在表示中もしくは選択中のプロファイル.<br>
	 * 新規の場合はnull
	 */
	protected CharacterData current;
	
	
	private CardLayout mainPanelLayout;

	private ImportWizardCardPanel activePanel;

	private AbstractAction actNext;
	
	private AbstractAction actPrev;

	private AbstractAction actFinish;
	

	protected ImportFileSelectPanel importFileSelectPanel;
	
	protected ImportTypeSelectPanel importTypeSelectPanel;
	
	protected ImportPartsSelectPanel importPartsSelectPanel;
	
	protected ImportPresetSelectPanel importPresetSelectPanel;

	protected ImportModel importModel = new ImportModel();
	
	/**
	 * プロファイルにパーツデータ・プリセットデータをインポートします.<br>
	 * 
	 * @param parent
	 *            親フレーム
	 * @param current
	 *            更新対象となる現在のプロファイル(新規インポートの場合はnull)
	 * @param initFiles
	 *            アーカイブファィルまたはディレクトリの初期選択、なければnullまたは空
	 */
	public ImportWizardDialog(JFrame parent, CharacterData current, List<File> initFiles) {
		super(parent, true);
		initComponent(parent, current);

		importFileSelectPanel.setSelectFile(initFiles);
	}
	
	/**
	 * プロファイルにパーツデータ・プリセットデータをインポートします.<br>
	 * 
	 * @param parent
	 *            親ダイアログ
	 * @param current
	 *            選択していてるプロファイル、新規インポートの場合はnull
	 */
	public ImportWizardDialog(JDialog parent, CharacterData current) {
		super(parent, true);
		initComponent(parent, current);
	}
	
	/**
	 * ウィザードダイアログのコンポーネントを初期化します.<br>
	 * currentがnullの場合は新規インポート、そうでない場合は更新インポートとります。
	 * 
	 * @param parent
	 *            親コンテナ
	 * @param current
	 *            インポート対象プロファイル、新規の場合はnull
	 */
	private void initComponent(Component parent, CharacterData current) {
		this.current = current;

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		// タイトル
		if (current == null) {
			setTitle(strings.getProperty("title.new"));
		} else {
			setTitle(strings.getProperty("title.update"));
		}

		// メインパネル
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		final JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEtchedBorder());
		this.mainPanelLayout = new CardLayout(5, 5);
		mainPanel.setLayout(mainPanelLayout);
		contentPane.add(mainPanel, BorderLayout.CENTER);
		
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateBtnPanelState();
			}
		};
		
		ComponentListener componentListener = new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				onComponentShown((ImportWizardCardPanel) e.getComponent());
			}
		};

		
		// アクション
		
		this.actNext = new AbstractAction(strings.getProperty("btn.next")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				setEnableButtons(false);
				String nextPanelName = doNext();
				if (nextPanelName != null) {
					mainPanelLayout.show(mainPanel, nextPanelName);
				} else {
					// 移動先ページ名なければ、現在のページでボタン状態を再設定する.
					// 移動先ページ名がある場合、実際に移動し表示されるまでディセーブルのままとする.
					updateBtnPanelState();
				}
			}
		};
		this.actPrev = new AbstractAction(strings.getProperty("btn.prev")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				setEnableButtons(false);
				String prevPanelName = doPrevious();
				if (prevPanelName != null) {
					mainPanelLayout.show(mainPanel, prevPanelName);
				} else {
					updateBtnPanelState();
				}
			}
		};
		this.actFinish = new AbstractAction(strings.getProperty("btn.finish")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onFinish();
			}
		};
		AbstractAction actCancel = new AbstractAction(strings.getProperty("btn.cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};

		// ImportFileSelectPanel
		this.importFileSelectPanel = new ImportFileSelectPanel();
		this.importFileSelectPanel.addComponentListener(componentListener);
		this.importFileSelectPanel.addChangeListener(changeListener);
		mainPanel.add(this.importFileSelectPanel, ImportFileSelectPanel.PANEL_NAME);
		
		// ImportTypeSelectPanel
		this.importTypeSelectPanel = new ImportTypeSelectPanel();
		this.importTypeSelectPanel.addComponentListener(componentListener);
		this.importTypeSelectPanel.addChangeListener(changeListener);
		mainPanel.add(this.importTypeSelectPanel, ImportTypeSelectPanel.PANEL_NAME);
		
		// ImportPartsSelectPanel
		this.importPartsSelectPanel = new ImportPartsSelectPanel();
		this.importPartsSelectPanel.addComponentListener(componentListener);
		this.importPartsSelectPanel.addChangeListener(changeListener);
		mainPanel.add(this.importPartsSelectPanel, ImportPartsSelectPanel.PANEL_NAME);
		
		// ImportPresetSelectPanel
		this.importPresetSelectPanel = new ImportPresetSelectPanel();
		this.importPresetSelectPanel.addComponentListener(componentListener);
		this.importPresetSelectPanel.addChangeListener(changeListener);
		mainPanel.add(this.importPresetSelectPanel, ImportPresetSelectPanel.PANEL_NAME);
		
		
		// button panel

		JPanel btnPanel = new JPanel();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 45));
		GridBagLayout btnPanelLayout = new GridBagLayout();
		btnPanel.setLayout(btnPanelLayout);

		actPrev.setEnabled(false);
		actNext.setEnabled(false);
		actFinish.setEnabled(false);
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 2 : 1;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		btnPanel.add(new JButton(this.actPrev), gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 3 : 2;
		gbc.gridy = 0;
		JButton btnNext = new JButton(this.actNext);
		btnPanel.add(btnNext, gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 4 : 3;
		gbc.gridy = 0;
		btnPanel.add(new JButton(this.actFinish), gbc);

		gbc.gridx = Main.isLinuxOrMacOSX() ? 1 : 4;
		gbc.gridy = 0;
		JButton btnCancel = new JButton(actCancel);
		btnPanel.add(btnCancel, gbc);

		contentPane.add(btnPanel, BorderLayout.SOUTH);
		
		// インプットマップ/アクションマップ
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		
		rootPane.setDefaultButton(btnNext);
		
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeImportWizDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeImportWizDialog");
		am.put("closeImportWizDialog", actCancel);

		// 表示

		setSize(500, 550);
		setLocationRelativeTo(parent);
		
		mainPanelLayout.first(mainPanel);
		updateBtnPanelState();
	}
	
	protected void onComponentShown(JPanel panel) {
		ImportWizardCardPanel activePanel = (ImportWizardCardPanel) panel;
		activePanel.onActive(this, this.activePanel);
		this.activePanel = activePanel;
		updateBtnPanelState();
	}
	
	protected void updateBtnPanelState() {
		if (activePanel != null) {
			actPrev.setEnabled(activePanel.isReadyPrevious());
			actNext.setEnabled(activePanel.isReadyNext());
			actFinish.setEnabled(activePanel.isReadyFinish());
			
		} else {
			setEnableButtons(false);
		}
	}
	
	public void setEnableButtons(boolean enabled) {
		actPrev.setEnabled(enabled);
		actNext.setEnabled(enabled);
		actFinish.setEnabled(enabled);
	}
	
	protected String doNext() {
		if (activePanel == null) {
			throw new IllegalStateException();
		}
		String nextPanelName;

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			nextPanelName = activePanel.doNext();
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
		return nextPanelName;
	}
	
	protected String doPrevious() {
		if (activePanel == null) {
			throw new IllegalStateException();
		}
		return activePanel.doPrevious();
	}
	
	protected void onClose() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);
		
		if (JOptionPane.showConfirmDialog(this,
				strings.getProperty("confirm.close"),
				strings.getProperty("confirm"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
			return;
		}

		// アーカイブを閉じる.
		importFileSelectPanel.closeArchive();

		// キャンセル
		this.exitCode = EXIT_CANCELED;
		this.importedCharacterData = null;
		
		// ウィンドウを閉じる
		dispose();
	}

	/**
	 * インポートの実行.<br>
	 */
	protected void onFinish() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		try {
			// 新規プロファイル作成、または更新の実行
			setEnableButtons(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			int exitCode;
			CharacterData importedCharacterData;
			try {
				if (current == null) {
					// 新規作成
					importedCharacterData = createNewProfile();
					exitCode = EXIT_PROFILE_CREATED;
				} else {
					// 更新
					importedCharacterData = updateProfile();
					exitCode = EXIT_PROFILE_UPDATED;
				}
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
			
			// アーカイブを閉じる
			importFileSelectPanel.closeArchive();
			
			// 完了メッセージ
			JOptionPane.showMessageDialog(this, strings.getProperty("complete"));
			
			// 完了後、ウィンドウを閉じる.
			this.exitCode = exitCode;
			this.importedCharacterData = importedCharacterData;
			dispose();
			
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			// ディセーブルにしていたボタンをパネルの状態に戻す.
			updateBtnPanelState();
		}
	}
	
	/**
	 * ウィザードが閉じられた場合の終了コード. {@link #EXIT_PROFILE_UPDATED}であればプロファイルが更新されており,<br>
	 * {@link #EXIT_PROFILE_CREATED}であればプロファイルが作成されている.<br>
	 * {@link #EXIT_CANCELED}であればキャンセルされている.<br>
	 * 
	 * @return 終了コード
	 */
	public int getExitCode() {
		return exitCode;
	}
	
	/**
	 * 新規または更新されたプロファイル、キャンセルされた場合はnull
	 * 
	 * @return プロファイル
	 */
	public CharacterData getImportedCharacterData() {
		return importedCharacterData;
	}
	
	/**
	 * アーカイブからの新規プロファイルの作成
	 * 
	 * @return 作成された新規プロファイル
	 * @throws IOException
	 *             失敗
	 */
	protected CharacterData createNewProfile() throws IOException {
		CharacterData cd = importModel.getCharacterData();
		if (cd == null || !cd.isValid()) {
			throw new IllegalStateException("imported caharcer data is invalid." + cd);
		}
		
		CharacterDataPersistent persist = CharacterDataPersistent.getInstance();

		CharacterData characterData = cd.duplicateBasicInfo();

		// キャラクターセット名と作者名を設定する
		characterData.setName(importTypeSelectPanel.getCharacterName());
		characterData.setAuthor(importTypeSelectPanel.getAuthor());
		
		// プリセットをインポートする場合
		characterData.clearPartsSets(false);
		if (importTypeSelectPanel.isImportPreset()) {
			for (PartsSet partsSet : importPresetSelectPanel.getSelectedPartsSets()) {
				PartsSet ps = partsSet.clone();
				ps.setPresetParts(true);
				characterData.addPartsSet(ps);
			}
			characterData.setDefaultPartsSetId(importPresetSelectPanel.getPrefferedDefaultPartsSetId());
		}
		
		// プロファイルの新規作成
		// docBaseが設定されて返される.
		persist.createProfile(characterData);

		// インポートするパーツの更新
		if (importTypeSelectPanel.isImportPartsImages()) {
			// パーツのコピー
			Collection<PartsImageContent> partsImageContents
				= importPartsSelectPanel.getSelectedPartsImageContents();
			importModel.copyPartsImageContents(partsImageContents, characterData);

			// パーツ管理情報の登録
			PartsManageData partsManageData = importModel.getPartsManageData();
			importModel.updatePartsManageData(partsImageContents, partsManageData, null, characterData);
		}
		
		// インポートするピクチャの更新
		if (importTypeSelectPanel.isImportSampleImage()) {
			BufferedImage samplePicture = importModel.getSamplePicture();
			if (samplePicture != null) {
				persist.saveSamplePicture(characterData, samplePicture);
			}
		}
		
		return characterData;
	}
	
	/**
	 * プロファイルの更新
	 * 
	 * @return 更新されたプロファイル
	 * @throws IOException
	 *             失敗
	 */
	protected CharacterData updateProfile() throws IOException {
		if (current == null || !current.isValid()) {
			throw new IllegalStateException("current profile is not valid. :" + current);
		}

		CharacterDataPersistent persist = CharacterDataPersistent.getInstance();

		CharacterData characterData = current.duplicateBasicInfo();
		
		boolean imported = false;
		boolean modCharacterDef = false;
		boolean modFavories = false;
		
		// インポートするパーツの更新
		if (importTypeSelectPanel.isImportPartsImages()) {
			// パーツのコピー
			Collection<PartsImageContent> partsImageContents
				= importPartsSelectPanel.getSelectedPartsImageContents();
			importModel.copyPartsImageContents(partsImageContents, characterData);
			
			// パーツ管理情報の追記・更新
			PartsManageData partsManageData = importModel.getPartsManageData();
			importModel.updatePartsManageData(partsImageContents, partsManageData, characterData, characterData);
			
			imported = true;
		}
		
		// インポートするピクチャの更新
		if (importTypeSelectPanel.isImportSampleImage()) {
			BufferedImage samplePicture = importModel.getSamplePicture();
			if (samplePicture != null) {
				persist.saveSamplePicture(characterData, samplePicture);
				imported = true;
			}
		}
		
		// インポートするパーツセットの更新
		if (importTypeSelectPanel.isImportPreset()) {
			for (PartsSet partsSet : importPresetSelectPanel.getSelectedPartsSets()) {
				PartsSet ps = partsSet.clone();
				ps.setPresetParts(false);
				characterData.addPartsSet(ps);
			}
			imported = true;
			modCharacterDef = true;
			modFavories = true;
		}
		
		// 説明の更新
		if (importTypeSelectPanel.isAddDescription() && imported) {
			URI archivedFile = importModel.getImportSource();
			String note = importTypeSelectPanel.getAdditionalDescription();
			if (note != null && note.length() > 0) {
				String description = characterData.getDescription();
				if (description == null) {
					description = "";
				}
				String lf = System.getProperty("line.separator");
				Timestamp tm = new Timestamp(System.currentTimeMillis());
				description += lf + "--- import: " + tm + " : " + archivedFile + " ---" + lf;
				description += note + lf;
				characterData.setDescription(description);
				modCharacterDef = true;
			}
		}
		
		// キャラクター定義の更新
		if (modCharacterDef) {
			persist.updateProfile(characterData); // キャラクター定義の構造に変化なし
			current.setDescription(characterData.getDescription());
		}
		// お気に入りの更新
		if (modFavories) {
			persist.saveFavorites(characterData);
		}
		
		return characterData;
	}

	
}

/**
 * タブの抽象基底クラス.<br>
 * 
 * @author seraphy
 */
abstract class ImportWizardCardPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();
	
	public void addChangeListener(ChangeListener l) {
		if (l != null) {
			listeners.add(l);
		}
	}
	
	public void removeChangeListener(ChangeListener l) {
		if (l != null) {
			listeners.remove(l);
		}
	}
	
	public void fireChangeEvent() {
		ChangeEvent e = new ChangeEvent(this);
		for (ChangeListener l : listeners) {
			l.stateChanged(e);
		}
	}
	
	public void onActive(ImportWizardDialog parent, ImportWizardCardPanel previousPanel) {
		// なにもしない
	}
	
	public boolean isReadyPrevious() {
		return false;
	}
	
	public boolean isReadyNext() {
		return false;
	}
	
	public boolean isReadyFinish() {
		return false;
	}

	public String doNext() {
		throw new UnsupportedOperationException();
	}
	
	public String doPrevious() {
		throw new UnsupportedOperationException();
	}
}

/**
 * ファイル選択パネル
 * 
 * @author seraphy
 */
class ImportFileSelectPanel extends ImportWizardCardPanel {

	private static final long serialVersionUID = 1L;
	
	public static final String PANEL_NAME = "fileSelectPanel";

	/**
	 * アーカイブ用ファイルダイアログ
	 */
	private static ArchiveFileDialog archiveFileDialog = new ArchiveFileDialog();

	private ImportWizardDialog parent;
	
	/**
	 * ファイル名を指定してインポート
	 */
	private JRadioButton radioArchiveFile;
	
	/**
	 * ファイル名入力ボックス
	 */
	private JTextField txtArchiveFile;
	
	/**
	 * ファイル選択ボタン
	 */
	private Action actChooseFile;


	/**
	 * ディレクトリを指定してインポート
	 */
	private JRadioButton radioDirectory;
	
	/**
	 * ディレクトリ入力ボックス
	 */
	private JTextField txtDirectory;
	
	/**
	 * ディレクトリ選択ボタン
	 */
	private Action actChooseDirectory;

	
	
	/* 以下、対象ファイルの読み取り結果 */
	
	
	public ImportFileSelectPanel() {
		setLayout(new BorderLayout());
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		DocumentListener documentListener = new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				fireEvent();
			}
			public void insertUpdate(DocumentEvent e) {
				fireEvent();
			}
			public void changedUpdate(DocumentEvent e) {
				fireEvent();
			}
			protected void fireEvent() {
				fireChangeEvent();
			}
		};
		
		txtArchiveFile = new JTextField();
		txtDirectory = new JTextField();

		txtArchiveFile.getDocument().addDocumentListener(documentListener);
		txtDirectory.getDocument().addDocumentListener(documentListener);
		
		actChooseFile = new AbstractAction(strings.getProperty("browse")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onChooseFile();
			}
		};
		
		actChooseDirectory = new AbstractAction(strings.getProperty("browse")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onChooseDirectory();
			}
		};
		

		JPanel fileChoosePanel = new JPanel();
		GridBagLayout fileChoosePanelLayout = new GridBagLayout();
		fileChoosePanel.setLayout(fileChoosePanelLayout);
		
		radioArchiveFile = new JRadioButton(strings.getProperty("importingArchiveFile"));
		radioDirectory = new JRadioButton(strings.getProperty("importingDirectory"));

		ChangeListener radioChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateUIState();
				fireChangeEvent();
			}
		};
		radioArchiveFile.addChangeListener(radioChangeListener);
		radioDirectory.addChangeListener(radioChangeListener);

		ButtonGroup btnGroup = new ButtonGroup();
		btnGroup.add(radioArchiveFile);
		btnGroup.add(radioDirectory);
		
		// アーカイブからのインポートをデフォルトとする
		radioArchiveFile.setSelected(true);
		
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		gbc.gridheight = 1;
		gbc.gridwidth = 3;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		fileChoosePanel.add(radioArchiveFile, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.ipadx = 45;
		gbc.weightx = 0;
		fileChoosePanel.add(Box.createHorizontalGlue(), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.ipadx = 0;
		gbc.weightx = 1.;
		fileChoosePanel.add(txtArchiveFile, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.ipadx = 0;
		gbc.weightx = 0.;
		fileChoosePanel.add(new JButton(actChooseFile), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.ipadx = 0;
		gbc.gridwidth = 3;
		gbc.weightx = 1.;
		fileChoosePanel.add(radioDirectory, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.ipadx = 45;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		fileChoosePanel.add(Box.createHorizontalGlue(), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.ipadx = 0;
		gbc.weightx = 1.;
		fileChoosePanel.add(txtDirectory, gbc);

		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.ipadx = 0;
		gbc.weightx = 0.;
		fileChoosePanel.add(new JButton(actChooseDirectory), gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.ipadx = 0;
		gbc.gridwidth = 3;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		fileChoosePanel.add(Box.createGlue(), gbc);
		
		add(fileChoosePanel, BorderLayout.CENTER);
		
		// ドロップターゲット
		new DropTarget(this, new FileDropTarget() {
			@Override
			protected void onDropFiles(List<File> dropFiles) {
				if (dropFiles == null || dropFiles.isEmpty()) {
					return;
				}
				setSelectFile(dropFiles);
			}

			@Override
			protected void onException(Exception ex) {
				ErrorMessageHelper.showErrorDialog(ImportFileSelectPanel.this, ex);
			}
		});
		
		updateUIState();
	}

	/**
	 * アーカイブファイルまたはディレクトリを選択状態とする.<br>
	 * nullの場合は選択を解除する.
	 * 
	 * @param dropFile
	 *            アーカイブファイルまたはディレクトリ、もしくはnull
	 */
	public void setSelectFile(List<File> dropFiles) {

		File dropFile = null;
		if (dropFiles != null && dropFiles.size() > 0) {
			dropFile = dropFiles.get(0);
		}

		if (dropFile == null) {
			// 選択なしの場合
			txtDirectory.setText("");
			txtArchiveFile.setText("");
			radioDirectory.setSelected(false);
			radioArchiveFile.setSelected(false);

		} else if (dropFile.isDirectory()) {
			// ディレクトリの場合
			txtDirectory.setText(dropFile.getPath());
			radioDirectory.setSelected(true);

		} else if (dropFile.isFile()) {
			// ファイルの場合
			txtArchiveFile.setText(dropFile.getPath());
			radioArchiveFile.setSelected(true);
		}
	}
	
	protected void updateUIState() {
		boolean enableArchiveFile = radioArchiveFile.isSelected();
		boolean enableDirectory = radioDirectory.isSelected();
		
		txtArchiveFile.setEnabled(enableArchiveFile);
		actChooseFile.setEnabled(enableArchiveFile);
		
		txtDirectory.setEnabled(enableDirectory);
		actChooseDirectory.setEnabled(enableDirectory);
	}
	
	protected void onChooseFile() {
		File initFile = null;
		if (txtArchiveFile.getText().trim().length() > 0) {
			initFile = new File(txtArchiveFile.getText());
		}
		File file = archiveFileDialog.showOpenDialog(this, initFile);
		if (file != null) {
			txtArchiveFile.setText(file.getPath());
			fireChangeEvent();
		}
	}
	
	protected void onChooseDirectory() {
		String directoryTxt = txtDirectory.getText();
		JFileChooser dirChooser = new JFileChooser(directoryTxt);
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (dirChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File dir = dirChooser.getSelectedFile();
		if (dir != null) {
			txtDirectory.setText(dir.getPath());
			fireChangeEvent();
		}
	}

	@Override
	public boolean isReadyNext() {
		if (radioArchiveFile.isSelected()) {
			String fileTxt = txtArchiveFile.getText();
			if (fileTxt != null && fileTxt.trim().length() > 0) {
				return true;
			}
		} else if (radioDirectory.isSelected()) {
			String directoryTxt = txtDirectory.getText();
			if (directoryTxt != null && directoryTxt.trim().length() > 0) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onActive(ImportWizardDialog parent, ImportWizardCardPanel previousPanel) {
		this.parent = parent;
		
		// 開いているアーカイブがあれば閉じる
		closeArchive();
	}
	
	/**
	 * 開いているアーカイブがあればクローズする.
	 */
	public void closeArchive() {
		try {
			parent.importModel.closeImportSource();

		} catch (IOException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			// エラーが発生しても、とりあえず無視する.
		}
	}
	
	@Override
	public String doNext() {
		if (!isReadyNext()) {
			return null;
		}

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		URI importArchive;
		if (radioArchiveFile.isSelected()) {
			// ファイルによるインポート
			File file = new File(txtArchiveFile.getText());
			if (!file.exists() || !file.isFile()) {
				JOptionPane.showMessageDialog(this, strings
						.getProperty("fileNotFound"), "ERROR",
						JOptionPane.ERROR_MESSAGE);
				return null;
			}
			importArchive = file.toURI();
		
		} else if (radioDirectory.isSelected()) {
			// ディレクトリによるインポート
			File file = new File(txtDirectory.getText());
			if ( !file.exists() || !file.isDirectory()) {
				JOptionPane.showMessageDialog(this, strings
						.getProperty("directoryNotFound"), "ERROR",
						JOptionPane.ERROR_MESSAGE);
				return null;
			}
			importArchive = file.toURI();

		} else {
			// それ以外はサポートしていない.
			return null;
		}
		
		try {
			parent.importModel.openImportSource(importArchive, parent.current);

			// ワーカースレッドでアーカイブの読み込みを行う.
			Worker<Object> worker = new Worker<Object>() {
				public Void doWork(ProgressHandle progressHandle) throws IOException {
					parent.importModel.loadContents(progressHandle);
					return null;
				}
			};
			
			WorkerWithProgessDialog<Object> dlg
				= new WorkerWithProgessDialog<Object>(parent, worker);
			
			dlg.startAndWait();
			
			// 読み込めたら次ページへ
			return ImportTypeSelectPanel.PANEL_NAME;

		} catch (WorkerException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex.getCause());
			
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
		
		return null;
	}
}

class URLTableRow implements Serializable {
	
	private static final long serialVersionUID = 3452190266438145049L;

	private String downloadURL;
	
	private String author;
	
	public String getAuthor() {
		return author;
	}
	
	public String getDownloadURL() {
		return downloadURL;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}
	
}

class URLTableModel extends AbstractTableModelWithComboBoxModel<URLTableRow> {

	private static final long serialVersionUID = 7075478118793390224L;

	private static final String[] COLUMN_NAMES;
	
	private static final int[] COLUMN_WIDTHS;
	
	static {
		COLUMN_NAMES = new String[] {
"作者",
				"URL",
		};
		COLUMN_WIDTHS = new int[] {
				100,
				300,
		};
	}
	
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		URLTableRow row = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return row.getAuthor();
		case 1:
			return row.getDownloadURL();
		}
		return "";
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}
	

	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < COLUMN_WIDTHS.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(COLUMN_WIDTHS[idx]);
		}
	}
	
	public void initModel(CharacterData characterData) {
		clear();
		HashMap<String, String> downloadUrlsMap = new HashMap<String, String>();
		if (characterData != null) {
			for (PartsCategory category : characterData.getPartsCategories()) {
				for (Map.Entry<PartsIdentifier, PartsSpec> entry : characterData
						.getPartsSpecMap(category).entrySet()) {
					PartsSpec partsSpec = entry.getValue();
					String author = partsSpec.getAuthor();
					String downloadURL = partsSpec.getDownloadURL();
					if (downloadURL != null && downloadURL.trim().length() > 0) {
						if (author == null || author.trim().length() == 0) {
							author = "";
						}
						downloadUrlsMap.put(downloadURL, author);
					}
				}
			}
		}
		
		for (Map.Entry<String, String> entry : downloadUrlsMap.entrySet()) {
			String downloadURL = entry.getKey();
			String author = entry.getValue();
			URLTableRow row = new URLTableRow();
			row.setDownloadURL(downloadURL);
			row.setAuthor(author);
			addRow(row);
		}
		
		Collections.sort(elements, new Comparator<URLTableRow>() {
			public int compare(URLTableRow o1, URLTableRow o2) {
				int ret = o1.getAuthor().compareTo(o2.getAuthor());
				if (ret == 0) {
					ret = o1.getDownloadURL().compareTo(o2.getDownloadURL());
				}
				return ret;
			}
		});
		
		fireTableDataChanged();
	}
}


/**
 * ファイル選択パネル
 * 
 * @author seraphy
 */
class ImportTypeSelectPanel extends ImportWizardCardPanel {
	
	private static final long serialVersionUID = 1L;

	public static final String PANEL_NAME = "importTypeSelectPanel";
	
	private ImportWizardDialog parent;

	private SamplePicturePanel samplePicturePanel;
	
	private JTextField txtCharacterId;
	
	private JTextField txtCharacterRev;

	private JTextField txtCharacterName;
	
	private JTextField txtAuthor;
	
	private JTextArea txtDescription;
	
	private JCheckBox chkPartsImages;
	
	private JCheckBox chkPresets;
	
	private JCheckBox chkSampleImage;
	
	private JCheckBox chkAddDescription;
	
	private String additionalDescription;
	

	/* 以下、選択結果 */
	
	public ImportTypeSelectPanel() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		GridBagLayout basicPanelLayout = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(basicPanelLayout);

		JPanel contentsSpecPanel = new JPanel();
		contentsSpecPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory
						.createTitledBorder(strings.getProperty("basic.contentsSpec"))));
		BoxLayout contentsSpecPanelLayout = new BoxLayout(contentsSpecPanel, BoxLayout.PAGE_AXIS);
		contentsSpecPanel.setLayout(contentsSpecPanelLayout);

		chkPartsImages = new JCheckBox(strings.getProperty("basic.chk.partsImages"));
		chkPresets = new JCheckBox(strings.getProperty("basic.chk.presets"));
		chkSampleImage = new JCheckBox(strings.getProperty("basic.chk.samplePicture"));

		contentsSpecPanel.add(chkPartsImages);
		contentsSpecPanel.add(chkPresets);
		contentsSpecPanel.add(chkSampleImage);

		//

		JPanel archiveInfoPanel = new JPanel();
		archiveInfoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createEmptyBorder(5, 5, 5, 5), BorderFactory
				.createTitledBorder(strings.getProperty("basic.archiveInfo"))));
		Dimension archiveInfoPanelMinSize = new Dimension(300, 200);
		archiveInfoPanel.setMinimumSize(archiveInfoPanelMinSize);
		archiveInfoPanel.setPreferredSize(archiveInfoPanelMinSize);
		GridBagLayout commentPanelLayout = new GridBagLayout();
		archiveInfoPanel.setLayout(commentPanelLayout);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		archiveInfoPanel.add(new JLabel(strings.getProperty("basic.profileId"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		txtCharacterId = new JTextField();
		txtCharacterId.setEditable(false); // 読み取り専用
		txtCharacterId.setEnabled(false);
		archiveInfoPanel.add(txtCharacterId, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		archiveInfoPanel.add(new JLabel(strings.getProperty("basic.profileRev"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		txtCharacterRev = new JTextField();
		txtCharacterRev.setEditable(false); // 読み取り専用
		txtCharacterRev.setEnabled(false);
		archiveInfoPanel.add(txtCharacterRev, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		archiveInfoPanel.add(new JLabel(strings.getProperty("basic.profileName"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		txtCharacterName = new JTextField();
		archiveInfoPanel.add(txtCharacterName, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		archiveInfoPanel.add(
				new JLabel(strings.getProperty("author"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		txtAuthor = new JTextField();
		archiveInfoPanel.add(txtAuthor, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		archiveInfoPanel.add(new JLabel(strings.getProperty("description"),
				JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		gbc.gridheight = 5;
		gbc.weighty = 1.;
		gbc.weightx = 1.;
		txtDescription = new JTextArea(); // 説明は更新可能にしておく。
		archiveInfoPanel.add(new JScrollPane(txtDescription), gbc);

		gbc.gridx = 0;
		gbc.gridy = 10;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.weighty = 0.;
		gbc.weightx = 0.;
		chkAddDescription = new JCheckBox(strings.getProperty("appendDescription"));
		archiveInfoPanel.add(chkAddDescription, gbc);

		// /

		samplePicturePanel = new SamplePicturePanel();
		JScrollPane samplePicturePanelSP = new JScrollPane(samplePicturePanel);
		samplePicturePanelSP.setBorder(null);
		JPanel samplePictureTitledPanel = new JPanel(new BorderLayout());
		samplePictureTitledPanel.add(samplePicturePanelSP, BorderLayout.CENTER);
		samplePictureTitledPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory
						.createTitledBorder(strings
								.getProperty("basic.sampleImage"))));

		// /
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		add(contentsSpecPanel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 1.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		add(archiveInfoPanel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		add(samplePictureTitledPanel, gbc);

		// アクションリスナ

		ActionListener modListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireChangeEvent();
			}
		};

		chkPartsImages.addActionListener(modListener);
		chkPresets.addActionListener(modListener);
		chkSampleImage.addActionListener(modListener);
		chkAddDescription.addActionListener(modListener);
	}

	@Override
	public void onActive(ImportWizardDialog parent, ImportWizardCardPanel previousPanel) {
		this.parent = parent;

		if (previousPanel == parent.importPartsSelectPanel) {
			return;
		}

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		// 呼び出しもと情報
		CharacterData current = parent.current;
		
		// キャラクター定義情報
		CharacterData cd = parent.importModel.getCharacterData();
		String readme;

		// 開いているか選択しているプロファイルが有効であれば更新可能
		final boolean updatable = (current != null && current.isValid());

		// 新規の場合でインポートもとが有効なキャラクターセットであれば作成可能
		final boolean creatable = (current == null && cd != null && cd.isValid());
		
		// 新規作成の場合はキャラクター定義名と作者名を更新可能とする
		txtCharacterName.setEnabled(current == null);
		txtCharacterName.setEditable(current == null);
		txtAuthor.setEditable(current == null);
		txtAuthor.setEnabled(current == null);
		
		// ID、REVが一致するか?
		boolean matchID = false;
		boolean matchREV = false;

		if (cd != null && cd.isValid()) {
			txtCharacterId.setText(cd.getId());
			txtCharacterRev.setText(cd.getRev());
			txtCharacterName.setText(cd.getName());
			
			if (current != null) {
				// 既存のプロファイルを選択していてインポート結果のキャラクター定義がある場合はID, REVを比較する.
				matchID = current.getId() == null ? cd.getId() == null : current.getId().equals(cd.getId());
				matchREV = current.getRev() == null ? cd.getRev() == null : current.getRev().equals(cd.getRev());
			} else {
				// 既存のプロファイルが存在しない場合は、ID,REVの比較は成功とみなす
				matchID = true;
				matchREV = true;
			}
			
			AppConfig appConfig = AppConfig.getInstance();
			Color invalidBgColor = appConfig.getInvalidBgColor();
			
			txtCharacterId.setBackground(matchID ? getBackground() : invalidBgColor);
			txtCharacterRev.setBackground(matchREV ? getBackground() : invalidBgColor);
			
			txtAuthor.setText(cd.getAuthor());
			readme = cd.getDescription(); 
			
		} else {
			// ID, REV等は存在しないので空にする
			txtCharacterId.setText("");
			txtCharacterRev.setText("");
			txtCharacterName.setText("");
			txtAuthor.setText("");
			
			// readmeで代用
			readme = parent.importModel.getReadme();
		}
		
		// 説明を追記する.
		boolean existsReadme = (readme != null && readme.trim().length() > 0);
		additionalDescription = existsReadme ? readme : "";
		txtDescription.setText(additionalDescription);
		chkAddDescription.setEnabled((updatable || creatable) && existsReadme);
		chkAddDescription.setSelected((updatable || creatable) && existsReadme);
		
		// プリセットまたはお気に入りが存在するか?
		boolean hasPresetOrFavorites = (cd == null) ? false : !cd.getPartsSets().isEmpty();
		chkPresets.setEnabled(hasPresetOrFavorites);
		chkPresets.setSelected(hasPresetOrFavorites);

		// パーツイメージ
		Collection<PartsImageContent> partsImageContentsMap = parent.importModel.getPartsImageContents();
		
		// パーツが存在するか?
		boolean hasParts = !partsImageContentsMap.isEmpty();
		chkPartsImages.setEnabled(hasParts);
		chkPartsImages.setSelected(hasParts);
		
		// サンプルピクチャ
		BufferedImage samplePicture = parent.importModel.getSamplePicture();
		if (samplePicture != null && (updatable || creatable)) {
			// サンプルピクチャが存在し、インポートか新規作成が可能であれば有効にする.
			samplePicturePanel.setSamplePicture(samplePicture);
			chkSampleImage.setEnabled(true);
			chkSampleImage.setSelected(current == null); // 新規作成の場合のみデフォルトでON

		} else {
			samplePicturePanel.setSamplePicture(samplePicture);
			chkSampleImage.setEnabled(false);
			chkSampleImage.setSelected(false);
		}

		// パーツまたはお気に入り・プリセットが存在する場合、
		// および、新規の場合はキャラクター定義が存在する場合はコンテンツ有り
		boolean hasContents = hasParts || hasPresetOrFavorites
			|| (current == null && cd != null && cd.isValid()); 

		if (!hasContents) {
			JOptionPane.showMessageDialog(this, strings.getProperty("noContents"));

		} else if (cd == null) {
			JOptionPane.showMessageDialog(this, strings.getProperty("notFormalArchive"));
		
		} else if (!matchID) {
			String fmt = strings.getProperty("unmatchedProfileId");
			String msg = MessageFormat.format(fmt,
					cd.getId() == null ? "" : cd.getId());
			JOptionPane.showMessageDialog(this, msg);
		
		} else if (!matchREV) {
			String fmt = strings.getProperty("unmatchedProfileRev");
			String msg = MessageFormat.format(fmt, cd.getRev() == null
					? ""
					: cd.getRev());
			JOptionPane.showMessageDialog(this, msg);
		}
	}
	
	public boolean isImportPreset() {
		return chkPresets.isSelected();
	}
	
	public boolean isImportPartsImages() {
		return chkPartsImages.isSelected();
	}
	
	public boolean isImportSampleImage() {
		return chkSampleImage.isSelected();
	}
	
	public boolean isAddDescription() {
		return chkAddDescription.isSelected();
	}
	
	/**
	 * 説明として追加するドキュメント.<Br>
	 * これはユーザーが編集可能であり、ユーザー編集後の値が取得される.<br>
	 * 
	 * @return 説明として追加するドキュメント
	 */
	public String getAdditionalDescription() {
		return txtDescription.getText();
	}
	
	/**
	 * キャラクター定義名を取得する.
	 * 
	 * @return キャラクター定義名
	 */
	public String getCharacterName() {
		return txtCharacterName.getText();
	}
	
	/**
	 * 作者名を取得する.
	 * 
	 * @return 作者名
	 */
	public String getAuthor() {
		return txtAuthor.getText();
	}
	
	@Override
	public boolean isReadyPrevious() {
		return true;
	}
	
	@Override
	public String doPrevious() {
		return ImportFileSelectPanel.PANEL_NAME;
	}
	
	@Override
	public boolean isReadyNext() {
		if (isImportPartsImages() || isImportPreset()) {
			// パーツイメージの選択もしくはパーツセットの選択を指定している場合は次へ進む
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isReadyFinish() {
		if (!isImportPartsImages() && !isImportPreset()) {
			if ((parent != null && parent.current == null)
					|| isImportSampleImage()) {
				// 新規プロファイル作成か、サンプルイメージの更新のみで
				// イメージもパーツセットもいらなければ、ただちに作成可能.
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String doNext() {
		return ImportPartsSelectPanel.PANEL_NAME;
	}
}


/**
 * パーツ選択パネル
 * 
 * @author seraphy
 */
class ImportPartsSelectPanel extends ImportWizardCardPanel {

	private static final long serialVersionUID = 1L;

	public static final String PANEL_NAME = "importPartsSelectPanel"; 
	
	private ImportWizardDialog parent;
	

	private ImportPartsTableModel partsTableModel;
	
	private JPanel profileSizePanel;
	
	private JTextField txtProfileHeight;
	
	private int profileWidth;
	
	private int profileHeight;

	private JTextField txtProfileWidth;

	private JTable partsTable;
	
	private Action actSelectAll;
	
	private Action actDeselectAll;

	private Action actSort;

	private Action actSortByTimestamp;

	
	public ImportPartsSelectPanel() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		setLayout(new BorderLayout());

		profileSizePanel = new JPanel();
		GridBagLayout profileSizePanelLayout = new GridBagLayout();
		profileSizePanel.setLayout(profileSizePanelLayout);
		profileSizePanel.setBorder(BorderFactory
				.createTitledBorder("プロファイルのサイズ"));
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		profileSizePanel.add(new JLabel("幅:", JLabel.RIGHT), gbc);

		txtProfileWidth = new JTextField();
		txtProfileWidth.setEditable(false);
		gbc.gridx = 1;
		gbc.gridy = 0;
		profileSizePanel.add(txtProfileWidth, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 0;
		profileSizePanel.add(new JLabel("高さ:", JLabel.RIGHT), gbc);

		txtProfileHeight = new JTextField();
		txtProfileHeight.setEditable(false);
		gbc.gridx = 3;
		gbc.gridy = 0;
		profileSizePanel.add(txtProfileHeight, gbc);
		
		gbc.gridx = 4;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		profileSizePanel.add(Box.createHorizontalGlue(), gbc);

		add(profileSizePanel, BorderLayout.NORTH);
		
		partsTableModel = new ImportPartsTableModel();

		partsTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				fireChangeEvent();
			}
		});

		AppConfig appConfig = AppConfig.getInstance();
		final Color disabledForeground = appConfig.getDisabledCellForgroundColor();

		partsTable = new JTable(partsTableModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				if (comp instanceof JCheckBox) {
					// BooleanのデフォルトのレンダラーはJCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}
				
				// 行モデル取得
				ImportPartsTableModel model = (ImportPartsTableModel) getModel();
				ImportPartsModel rowModel = model.getRow(row);
				
				Long lastModifiedAtCur = rowModel.getLastModifiedAtCurrentProfile();
				if (lastModifiedAtCur != null) {
					// 既存のパーツが存在すれば太字
					comp.setFont(getFont().deriveFont(Font.BOLD));
				} else {
					// 新規パーツであれば通常フォント
					comp.setFont(getFont());
				}

				// 列ごとの警告の判定
				boolean warnings = false;
				if (column == ImportPartsTableModel.COLUMN_LASTMODIFIED) {
					// 既存のほうが日付が新しければワーニング
					if (lastModifiedAtCur != null && 
							rowModel.getLastModified() < lastModifiedAtCur.longValue()) {
						warnings = true;
					}
				} else if (column == ImportPartsTableModel.COLUMN_ALPHA) {
					// アルファ情報がない画像は警告
					if (!rowModel.isAlphaColor()) {
						warnings = true;
					}
				} else if (column == ImportPartsTableModel.COLUMN_SIZE) {
					// プロファイルの画像サイズと一致しないか、不揃いな画像であれば警告
					if (rowModel.isUnmatchedSize()
							|| profileWidth != rowModel.getWidth()
							|| profileHeight != rowModel.getHeight()) {
						warnings = true;
					}
				}
				
				// 前景色、ディセーブル時は灰色
				Color foregroundColor = isCellSelected(row, column) ? getSelectionForeground() : getForeground();
				comp.setForeground(isEnabled() ? foregroundColor : disabledForeground);
				
				// 背景色、警告行は赤色に
				if (warnings) {
					AppConfig appConfig = AppConfig.getInstance();
					Color invalidBgColor = appConfig.getInvalidBgColor();
					comp.setBackground(invalidBgColor);
				} else {
					if (isCellSelected(row, column)) {
						comp.setBackground(getSelectionBackground());
					} else {
						comp.setBackground(getBackground());
					}
				}
				
				return comp;
			}
		};
		partsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		partsTable.setShowGrid(true);
		partsTable.setGridColor(appConfig.getGridColor());
		partsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		partsTableModel.adjustColumnModel(partsTable.getColumnModel());
		partsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		partsTable.setRowSelectionAllowed(true);

		Action actPartsSetCheck = new AbstractAction(strings.getProperty("parts.popup.check")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int[] selRows = partsTable.getSelectedRows();
				partsTableModel.setCheck(selRows, true);
			}
		};
		Action actPartsUnsetCheck = new AbstractAction(strings.getProperty("parts.popup.uncheck")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int[] selRows = partsTable.getSelectedRows();
				partsTableModel.setCheck(selRows, false);
			}
		};
		
		final JPopupMenu partsTablePopupMenu = new JPopupMenu();
		partsTablePopupMenu.add(actPartsSetCheck);
		partsTablePopupMenu.add(actPartsUnsetCheck);
		
		partsTable.setComponentPopupMenu(partsTablePopupMenu);
		
		JScrollPane partsTableSP = new JScrollPane(partsTable);
		partsTableSP.setBorder(null);
		JPanel partsTableTitledPanel = new JPanel(new BorderLayout());
		partsTableTitledPanel.add(partsTableSP, BorderLayout.CENTER);
		partsTableTitledPanel.setBorder(BorderFactory.createTitledBorder(strings
				.getProperty("parts.title")));

		add(partsTableTitledPanel, BorderLayout.CENTER);

		actSelectAll = new AbstractAction(strings
				.getProperty("parts.btn.selectAll")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onSelectAll();
			}
		};
		actDeselectAll = new AbstractAction(strings
				.getProperty("parts.btn.deselectAll")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onDeselectAll();
			}
		};
		actSort = new AbstractAction(strings.getProperty("parts.btn.sort")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onSort();
			}
		};
		actSortByTimestamp = new AbstractAction(strings
				.getProperty("parts.btn.sortByTimestamp")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onSortByTimestamp();
			}
		};

		JPanel btnPanel = new JPanel();
		GridBagLayout btnPanelLayout = new GridBagLayout();
		btnPanel.setLayout(btnPanelLayout);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.ipadx = 0;
		gbc.ipady = 0;
		JButton btnSelectAll = new JButton(actSelectAll);
		btnPanel.add(btnSelectAll, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		JButton btnDeselectAll = new JButton(actDeselectAll);
		btnPanel.add(btnDeselectAll, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		JButton btnSort = new JButton(actSort);
		btnPanel.add(btnSort, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		JButton btnSortByTimestamp = new JButton(actSortByTimestamp);
		btnPanel.add(btnSortByTimestamp, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);

		add(btnPanel, BorderLayout.SOUTH);
	}

	
	@Override
	public void onActive(ImportWizardDialog parent, ImportWizardCardPanel previousPanel) {
		this.parent = parent;
		if (previousPanel == parent.importPresetSelectPanel) {
			return;
		}
		
		// インポート対象のプロファイルサイズ
		CharacterData characterData;
		if (parent.current == null) {
			// 新規インポート
			characterData = parent.importModel.getCharacterData();
		} else {
			// 更新インポート
			characterData = parent.current;
		}
		int profileWidth = 0;
		int profileHeight = 0;
		if (characterData != null) {
			Dimension imageSize = characterData.getImageSize();
			if (imageSize != null) {
				profileWidth = imageSize.width;
				profileHeight = imageSize.height;
			}
		}
		txtProfileWidth.setText(Integer.toString(profileWidth));
		txtProfileHeight.setText(Integer.toString(profileHeight));
		profileSizePanel.revalidate();
		this.profileHeight = profileHeight;
		this.profileWidth = profileWidth;
		
		// パーツのインポート指定があれば編集可能に、そうでなければ表示のみ
		// (パーツセットのインポートの確認のため、パーツ一覧は表示できるようにしておく)
		boolean enabled = parent.importTypeSelectPanel.isImportPartsImages();
		
		partsTable.setEnabled(enabled);
		actDeselectAll.setEnabled(enabled);
		actSelectAll.setEnabled(enabled);
		actSort.setEnabled(enabled);
		actSortByTimestamp.setEnabled(enabled);
		
		CharacterData currentProfile = parent.current;
		Collection<PartsImageContent> partsImageContents = parent.importModel.getPartsImageContents();
		PartsManageData partsManageData = parent.importModel.getPartsManageData();
		partsTableModel.initModel(partsImageContents, partsManageData, currentProfile);
		
		// プリセットのモデルも更新する.
		Collection<PartsSet> partsSets = null;
		if (parent.importTypeSelectPanel.isImportPreset()) {
			CharacterData cd = parent.importModel.getCharacterData();
			if (cd != null && cd.isValid()) {
				partsSets = cd.getPartsSets().values();
			}
		}
		
		String defaultPartsSetId;
		CharacterData presetImportTarget;
		if (parent.current == null) {
			presetImportTarget = null;
			CharacterData cd = parent.importModel.getCharacterData();
			if (cd != null) {
				defaultPartsSetId = cd.getDefaultPartsSetId();
			} else {
				defaultPartsSetId = null;
			}
		} else {
			presetImportTarget = parent.current;
			defaultPartsSetId = null; // 既存の場合はデフォルトのパーツセットであるかは表示する必要ないのでnullにする.
		}
		
		parent.importPresetSelectPanel.initModel(partsSets, defaultPartsSetId, presetImportTarget);
	}

	@Override
	public boolean isReadyPrevious() {
		return true;
	}
	
	@Override
	public String doPrevious() {
		this.partsTableModel.clear();
		return ImportTypeSelectPanel.PANEL_NAME;
	}

	@Override
	public boolean isReadyNext() {
		if (this.parent != null) {
			if (this.parent.importTypeSelectPanel.isImportPreset()) {
				// パーツセットのインポート指定があれば次へ
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isReadyFinish() {
		if (this.parent != null) {
			if (this.parent.importTypeSelectPanel.isImportPartsImages()
					&& !this.parent.importTypeSelectPanel.isImportPreset()) {
				// パーツセットのインポート指定がなければ可
				return true;
			}
		}
		return false;
	}
	
	public String doNext() {
		return ImportPresetSelectPanel.PANEL_NAME;
	};
	
	
	protected void onSelectAll() {
		partsTableModel.selectAll();
	}

	protected void onDeselectAll() {
		partsTableModel.deselectAll();
	}

	protected void onSort() {
		partsTableModel.sort();
		if (partsTableModel.getRowCount() > 0) {
			Rectangle rct = partsTable.getCellRect(0, 0, true);
			partsTable.scrollRectToVisible(rct);
		}
	}

	protected void onSortByTimestamp() {
		partsTableModel.sortByTimestamp();
		if (partsTableModel.getRowCount() > 0) {
			Rectangle rct = partsTable.getCellRect(0, 0, true);
			partsTable.scrollRectToVisible(rct);
		}
	}

	/**
	 * 選択されたイメージコンテンツのコレクション.<br>
	 * 
	 * @return 選択されたイメージコンテンツのコレクション、なければ空
	 */
	public Collection<PartsImageContent> getSelectedPartsImageContents() {
		return partsTableModel.getSelectedPartsImageContents();
	}
	
	/**
	 * すでにプロファイルに登録済みのパーツ識別子、および、これからインポートする予定の選択されたパーツ識別子のコレクション.<br>
	 * 
	 * @return インポートされた、またはインポートするパーツ識別子のコレクション.なければ空.
	 */
	public Collection<PartsIdentifier> getImportedPartsIdentifiers() {
		HashSet<PartsIdentifier> partsIdentifiers = new HashSet<PartsIdentifier>();
		partsIdentifiers.addAll(partsTableModel.getCurrentProfilePartsIdentifers());
		partsIdentifiers.addAll(partsTableModel.getSelectedPartsIdentifiers());
		return partsIdentifiers;
	}
	
	public void selectByPartsIdentifiers(Collection<PartsIdentifier> partsIdentifiers) {
		partsTableModel.selectByPartsIdentifiers(partsIdentifiers);
	}
	
}


/**
 * 同じパーツ名をもつイメージのコレクション.<br>
 * パーツの各レイヤーの集合を想定する.<br>
 * 
 * @author seraphy
 */
class ImportPartsImageSet extends AbstractCollection<PartsImageContent> {

	/**
	 * パーツ名
	 */
	private String partsName;
	
	/**
	 * 各レイヤー
	 */
	private ArrayList<PartsImageContent> contentSet = new ArrayList<PartsImageContent>();
	
	private Long lastModified;
	
	private int width;
	
	private int height;
	
	private boolean unmatchedSize;
	
	private boolean alphaColor;
	
	private Collection<PartsCategory> partsCategories;
	
	private boolean checked;

	
	public ImportPartsImageSet(String partsName) {
		if (partsName == null || partsName.length() == 0) {
			throw new IllegalArgumentException();
		}
		this.partsName = partsName;
	}
	
	public String getPartsName() {
		return partsName;
	}
	
	@Override
	public int size() {
		return contentSet.size();
	}
	
	public Iterator<PartsImageContent> iterator() {
		return contentSet.iterator();
	}
	
	@Override
	public boolean add(PartsImageContent o) {
		if (o == null) {
			throw new IllegalArgumentException();
		}
		if (!partsName.equals(o.getPartsName())) {
			throw new IllegalArgumentException();
		}
		
		lastModified = null; // リセットする.
		
		return contentSet.add(o);
	}
	
	public int getWidth() {
		recheck();
		return width;
	}
	
	public int getHeight() {
		recheck();
		return height;
	}
	
	public boolean isUnmatchedSize() {
		recheck();
		return unmatchedSize;
	}
	
	public boolean isAlphaColor() {
		recheck();
		return alphaColor;
	}
	
	public long lastModified() {
		recheck();
		return lastModified.longValue();
	}
	
	public Collection<PartsCategory> getPartsCategories() {
		recheck();
		return this.partsCategories;
	}
	
	protected void recheck() {
		if (lastModified != null) {
			return;
		}

		long lastModified = 0;
		int maxWidth = 0;
		int maxHeight = 0;
		int minWidth = 0;
		int minHeight = 0;
		boolean alphaColor = !this.contentSet.isEmpty();
		HashSet<PartsCategory> partsCategories = new HashSet<PartsCategory>();

		for (PartsImageContent partsImageContent : this.contentSet) {
			PNGFileImageHeader header = partsImageContent.getPngFileImageHeader();

			maxWidth = Math.max(maxWidth, header.getWidth());
			maxHeight = Math.max(maxHeight, header.getHeight());
			minWidth = Math.max(minWidth, header.getWidth());
			minHeight = Math.max(minHeight, header.getHeight());
			
			if (header.getColorType() != 6 && !header.hasTransparencyInformation()) {
				// TrueColor + Alpha (6)か、アルファ情報があるもの以外はアルファなしとする.
				alphaColor = false;
			}
			
			for (CategoryLayerPair clPair : partsImageContent.getCategoryLayerPairs()) {
				partsCategories.add(clPair.getPartsCategory());
			}
			
			long tm = partsImageContent.lastModified();
			lastModified = Math.max(lastModified, tm);
		}
		
		this.lastModified = Long.valueOf(lastModified);
		this.width = maxWidth;
		this.height = maxHeight;
		this.unmatchedSize = (minWidth != maxWidth) || (minHeight != maxHeight);
		this.alphaColor = alphaColor;
		this.partsCategories = Collections.unmodifiableCollection(partsCategories);
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}
	
	public boolean isChecked() {
		return checked;
	}
}



class ImportPartsModel {

	private PartsIdentifier partsIdentifier;
	
	private PartsAuthorInfo authorInfo;
	
	private PartsManageData.PartsVersionInfo versionInfo;
	
	private PartsSpec partsSpecAtCurrent;
	
	private ImportPartsImageSet imageSet;
	
	private int numOfLink;
	
	private Long lastModifiedAtCurrentProfile;
	

	/**
	 * 行モデルを構築する
	 * 
	 * @param partsIdentifier
	 *            パーツ識別子
	 * @param authorInfo
	 *            作者情報(なければnull)
	 * @param versionInfo
	 *            バージョン情報(なければnull)
	 * @param imageSet
	 *            イメージファイルのセット
	 * @param numOfLink
	 *            カテゴリの参照カウント数(複数カテゴリに参照される場合は2以上となる)
	 */
	public ImportPartsModel(PartsIdentifier partsIdentifier,
			PartsAuthorInfo authorInfo,
			PartsManageData.PartsVersionInfo versionInfo,
			PartsSpec partsSpecAtCurrent,
			ImportPartsImageSet imageSet, int numOfLink) {
		if (partsIdentifier == null || imageSet == null) {
			throw new IllegalArgumentException();
		}

		this.partsIdentifier = partsIdentifier;
		this.authorInfo = authorInfo;
		this.versionInfo = versionInfo;
		this.partsSpecAtCurrent = partsSpecAtCurrent;
		this.imageSet = imageSet;
		this.numOfLink = numOfLink;
		
		if (partsSpecAtCurrent != null) {
			lastModifiedAtCurrentProfile = Long.valueOf(partsSpecAtCurrent
					.getPartsFiles().lastModified());
		} else {
			lastModifiedAtCurrentProfile = null;
		}
	}
	
	public int getNumOfLink() {
		return numOfLink;
	}
	
	public PartsIdentifier getPartsIdentifier() {
		return partsIdentifier;
	}
	
	public ImportPartsImageSet getImageSet() {
		return imageSet;
	}

	public String getPartsName() {
		return partsIdentifier.getLocalizedPartsName();
	}
	
	public String getAuthor() {
		if (authorInfo != null) {
			return authorInfo.getAuthor();
		}
		return null;
	}
	
	public String getAuthorAtCurrent() {
		if (partsSpecAtCurrent != null) {
			PartsAuthorInfo partsAuthorInfo = partsSpecAtCurrent.getAuthorInfo();
			if (partsAuthorInfo != null) {
				return partsAuthorInfo.getAuthor();
			}
		}
		return null;
	}
	
	public double getVersion() {
		if (versionInfo != null) {
			return versionInfo.getVersion();
		}
		return 0;
	}
	
	public double getVersionAtCurrent() {
		if (partsSpecAtCurrent != null) {
			return partsSpecAtCurrent.getVersion();
		}
		return 0;
	}
	
	public PartsCategory getPartsCategory() {
		return partsIdentifier.getPartsCategory();
	}
	
	public void setChecked(boolean checked) {
		imageSet.setChecked(checked);
	}
	
	public boolean isChecked() {
		return imageSet.isChecked();
	}
	
	public int getWidth() {
		return imageSet.getWidth();
	}
	
	public int getHeight() {
		return imageSet.getHeight();
	}
	
	public boolean isUnmatchedSize() {
		return imageSet.isUnmatchedSize();
	}
	
	public boolean isAlphaColor() {
		return imageSet.isAlphaColor();
	}
	
	public long getLastModified() {
		return imageSet.lastModified();
	}
	
	public Long getLastModifiedAtCurrentProfile() {
		return lastModifiedAtCurrentProfile;
	}
}


class ImportPartsTableModel extends AbstractTableModelWithComboBoxModel<ImportPartsModel> {

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMN_NAMES;
	
	private static final int[] COLUMN_WIDTHS;
	
	public static final int COLUMN_LASTMODIFIED = 5;
	
	public static final int COLUMN_ALPHA = 4;
	
	public static final int COLUMN_SIZE = 3;
	
	private Set<PartsIdentifier> currentProfilePartsIdentifiers;
	
	static {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);
		
		COLUMN_NAMES = new String[] {
				strings.getProperty("parts.column.check"),
				strings.getProperty("parts.column.partsname"),
				strings.getProperty("parts.column.category"),
				strings.getProperty("parts.column.imagesize"),
				strings.getProperty("parts.column.alpha"),
				strings.getProperty("parts.column.lastmodified"),
				strings.getProperty("parts.column.org-lastmodified"),
				strings.getProperty("parts.column.author"),
				strings.getProperty("parts.column.org-author"),
				strings.getProperty("parts.column.version"),
				strings.getProperty("parts.column.org-version"),
		};
		COLUMN_WIDTHS = new int[] {
				Integer.parseInt(strings.getProperty("parts.column.check.size")),
				Integer.parseInt(strings.getProperty("parts.column.partsname.size")),
				Integer.parseInt(strings.getProperty("parts.column.category.size")),
				Integer.parseInt(strings.getProperty("parts.column.imagesize.size")),
				Integer.parseInt(strings.getProperty("parts.column.alpha.size")),
				Integer.parseInt(strings.getProperty("parts.column.lastmodified.size")),
				Integer.parseInt(strings.getProperty("parts.column.org-lastmodified.size")),
				Integer.parseInt(strings.getProperty("parts.column.author.size")),
				Integer.parseInt(strings.getProperty("parts.column.org-author.size")),
				Integer.parseInt(strings.getProperty("parts.column.version.size")),
				Integer.parseInt(strings.getProperty("parts.column.org-version.size")),
		};
	}
	

	/**
	 * モデルを初期化する.<br>
	 * 
	 * @param partsImageContents
	 *            インポートもとアーカイブに含まれる、全パーツイメージコンテンツ
	 * @param currentProfile
	 *            インポート先のプロファイル、現在プロファイルが既に持っているパーツを取得するためのもの。
	 */
	public void initModel(Collection<PartsImageContent> partsImageContents, PartsManageData partsManageData, CharacterData currentProfile) {
		clear();
		if (partsImageContents == null || partsManageData == null) {
			return;
		}
		
		// 現在のプロファイルが所有する全パーツ一覧を構築する.
		// 現在のプロファイルがなければ空.
		HashSet<PartsIdentifier> currentProfilePartsIdentifiers = new HashSet<PartsIdentifier>();
		if (currentProfile != null) {
			for (PartsCategory partsCategory : currentProfile.getPartsCategories()) {
				currentProfilePartsIdentifiers.addAll(currentProfile.getPartsSpecMap(partsCategory).keySet());
			}
		}
		this.currentProfilePartsIdentifiers = Collections.unmodifiableSet(currentProfilePartsIdentifiers);

		// 同じパーツ名をもつ各レイヤーを集める
		HashMap<String, ImportPartsImageSet> partsImageSets = new HashMap<String, ImportPartsImageSet>();
		for (PartsImageContent content : partsImageContents) {
			String partsName = content.getPartsName();
			ImportPartsImageSet partsImageSet = partsImageSets.get(partsName);
			if (partsImageSet == null) {
				partsImageSet = new ImportPartsImageSet(partsName);
				partsImageSets.put(partsName, partsImageSet);
			}
			partsImageSet.add(content);
		}
		
		// 名前順に並び替える
		ArrayList<String> partsNames = new ArrayList<String>(partsImageSets.keySet());
		Collections.sort(partsNames);

		// 登録する
		for (String partsName : partsNames) {
			ImportPartsImageSet partsImageSet = partsImageSets.get(partsName);
			int numOfLink = partsImageSet.getPartsCategories().size();
			for (PartsCategory partsCategory : partsImageSet.getPartsCategories()) {
				
				// パーツ管理情報の索引キー
				PartsManageData.PartsKey partsKey = new PartsManageData.PartsKey(partsName, partsCategory.getCategoryId());
				// ローカライズされたパーツ名があれば取得する。なければオリジナルのまま
				String localizedPartsName = partsManageData.getLocalizedName(partsKey);
				if (localizedPartsName == null || localizedPartsName.length() == 0) {
					localizedPartsName = partsName;
				}
				// 作者情報・バージョン情報があれば取得する.
				PartsAuthorInfo partsAuthorInfo = partsManageData.getPartsAuthorInfo(partsKey);
				PartsManageData.PartsVersionInfo versionInfo = partsManageData.getVersion(partsKey);

				// パーツ識別子を構築する
				PartsIdentifier partsIdentifier = new PartsIdentifier(partsCategory, partsName, localizedPartsName);

				// 現在のプロファイル上のパーツ情報を取得する.(なければnull)
				PartsSpec partsSpec;
				if (currentProfile != null) {
					partsSpec = currentProfile.getPartsSpec(partsIdentifier);
				} else {
					partsSpec = null;
				}

				// 行モデルを構築する.
				ImportPartsModel rowModel = new ImportPartsModel(
						partsIdentifier, partsAuthorInfo, versionInfo,
						partsSpec, partsImageSet, numOfLink);
				
				addRow(rowModel);
			}
		}
		
		// 既存がないか、既存よりも新しい日付であれば自動的にチェックを設定する.
		// もしくはバージョンが上であれば自動的にチェックをつける.
		for (ImportPartsModel rowModel : elements) {
			
			// 現在のプロファイル上のファイル群の最終更新日
			Long lastModifiedAtCurrent = rowModel.getLastModifiedAtCurrentProfile();
			if (lastModifiedAtCurrent == null) {
				lastModifiedAtCurrent = Long.valueOf(0);
			}
			
			// インポートするファイル群の最終更新日
			ImportPartsImageSet partsImageSet = rowModel.getImageSet();

			// 新しければ自動的にチェックをつける.
			if (lastModifiedAtCurrent.longValue() < partsImageSet.lastModified()) {
				partsImageSet.setChecked(true);
			}
			
			// バージョンが新しければチェックをつける. (改変版や作者名改名もあるので、作者名が同一であるかは問わない.)
			double versionAtCurrent = rowModel.getVersionAtCurrent();
			double version = rowModel.getVersion();
			if (versionAtCurrent < version) {
				partsImageSet.setChecked(true);
			}
		}

		// 並び替え
		sort();
	}
	
	/**
	 * 選択されているパーツを構成するファイルのコレクションを返します.<br>
	 * 
	 * @return パーツイメージコンテンツのコレクション、選択がなければ空
	 */
	public Collection<PartsImageContent> getSelectedPartsImageContents() {
		IdentityHashMap<ImportPartsImageSet, ImportPartsImageSet> partsImageSets
				= new IdentityHashMap<ImportPartsImageSet, ImportPartsImageSet>();
		
		for (ImportPartsModel rowModel : elements) {
			ImportPartsImageSet partsImageSet = rowModel.getImageSet();
			if (partsImageSet.isChecked()) {
				partsImageSets.put(partsImageSet, partsImageSet);
			}
		}
		
		ArrayList<PartsImageContent> partsImageContents = new ArrayList<PartsImageContent>();
		for (ImportPartsImageSet partsImageSet : partsImageSets.values()) {
			partsImageContents.addAll(partsImageSet);
		}
		return partsImageContents;
	}

	/**
	 * 選択されているパーツ識別子のコレクションを返します.<br>
	 * 返されるコレクションには同一のパーツ識別子が複数存在しないことが保証されます.<br>
	 * 一つも選択がない場合は空が返されます.<br>
	 * 
	 * @return パーツ識別子のコレクション.<br>
	 */
	public Collection<PartsIdentifier> getSelectedPartsIdentifiers() {
		HashSet<PartsIdentifier> partsIdentifiers = new HashSet<PartsIdentifier>();
		for (ImportPartsModel rowModel : elements) {
			if (rowModel.isChecked()) {
				partsIdentifiers.add(rowModel.getPartsIdentifier());
			}
		}
		return partsIdentifiers;
	}
	
	/**
	 * 現在のプロファイルが所有している全パーツの識別子.<br>
	 * 現在のプロファイルがないか、まったく所有していなければ空.<br>
	 * 
	 * @return 現在のプロファイルが所有するパーツの識別子のコレクション.(重複しない一意であることが保証される.)
	 */
	public Collection<PartsIdentifier> getCurrentProfilePartsIdentifers() {
		return currentProfilePartsIdentifiers;
	}

	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		ImportPartsModel rowModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return rowModel.isChecked();
		case 1:
			return rowModel.getPartsName();
		case 2:
			return rowModel.getPartsCategory().getLocalizedCategoryName();
		case 3:
			return rowModel.getWidth() + "x" + rowModel.getHeight()
					+ (rowModel.isUnmatchedSize() ? "*" : "");
		case 4:
			return rowModel.isAlphaColor();
		case 5:
			long lastModified = rowModel.getLastModified();
			if (lastModified > 0) {
				return new Timestamp(lastModified).toString();
			}
			return "";
		case 6:
			Long lastModifiedAtCur = rowModel.getLastModifiedAtCurrentProfile();
			if (lastModifiedAtCur != null && lastModifiedAtCur.longValue() > 0) {
				return new Timestamp(lastModifiedAtCur.longValue()).toString();
			}
			return "";
		case 7:
			return rowModel.getAuthor();
		case 8:
			return rowModel.getAuthorAtCurrent();
		case 9:
			double version = rowModel.getVersion();
			if (version > 0) {
				return Double.toString(version);
			}
			return "";
		case 10:
			double versionAtCurrent = rowModel.getVersionAtCurrent();
			if (versionAtCurrent > 0) {
				return Double.toString(versionAtCurrent);
			}
			return "";
		}
		return "";
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ImportPartsModel rowModel = getRow(rowIndex);

		switch (columnIndex) {
		case 0:
			rowModel.setChecked(((Boolean) aValue).booleanValue());
			break;
		default:
			return;
		}

		if (rowModel.getNumOfLink() > 1) {
			fireTableDataChanged();
		} else {
			fireListUpdated(rowIndex, rowIndex);
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return Boolean.class;
		case 1:
			return String.class;
		case 2:
			return String.class;
		case 3:
			return String.class;
		case 4:
			return Boolean.class;
		case 5:
			return String.class;
		case 6:
			return String.class;
		case 7:
			return String.class;
		case 8:
			return String.class;
		case 9:
			return String.class;
		case 10:
			return String.class;
		}
		return String.class;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return true;
		}
		return false;
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		int mx = columnModel.getColumnCount();
		for (int idx = 0; idx < mx; idx++) {
			columnModel.getColumn(idx).setWidth(COLUMN_WIDTHS[idx]);
		}
	}
	
	public void selectAll() {
		boolean modified = false;
		for (ImportPartsModel rowModel : elements) {
			if (!rowModel.isChecked()) {
				rowModel.setChecked(true);
				modified = true;
			}
		}
		if (modified) {
			fireTableDataChanged();
		}
	}
	
	public void deselectAll() {
		boolean modified = false;
		for (ImportPartsModel rowModel : elements) {
			if (rowModel.isChecked()) {
				rowModel.setChecked(false);
				modified = true;
			}
		}
		if (modified) {
			fireTableDataChanged();
		}
	}
	
	public void sort() {
		Collections.sort(elements, new Comparator<ImportPartsModel> () {
			public int compare(ImportPartsModel o1, ImportPartsModel o2) {
				int ret = (o1.isChecked() ? 0 : 1) - (o2.isChecked() ? 0 : 1);
				if (ret == 0) {
					ret = o1.getPartsIdentifier().compareTo(o2.getPartsIdentifier());
				}
				return ret;
			}
		});
		
		fireTableDataChanged();
	}
	
	public void sortByTimestamp() {
		Collections.sort(elements, new Comparator<ImportPartsModel> () {
			public int compare(ImportPartsModel o1, ImportPartsModel o2) {
				long ret = (o1.isChecked() ? 0 : 1) - (o2.isChecked() ? 0 : 1);
				if (ret == 0) {
					Long tm1 = o1.getLastModifiedAtCurrentProfile();
					Long tm2 = o2.getLastModifiedAtCurrentProfile();
					
					long lastModified1 = Math.max(o1.getLastModified(), tm1 == null ? 0 : tm1.longValue());
					long lastModified2 = Math.max(o2.getLastModified(), tm2 == null ? 0 : tm2.longValue());

					ret = lastModified1 - lastModified2;
				}
				if (ret == 0) {
					ret = o1.getPartsIdentifier().compareTo(o2.getPartsIdentifier());
				}
				
				return ret == 0 ? 0 : ret > 0 ? 1 : -1;
			}
		});

		fireTableDataChanged();
	}

	/**
	 * 指定したパーツ識別子をチェック状態にする.
	 * 
	 * @param partsIdentifiers
	 *            パーツ識別子のコレクション、nullの場合は何もしない.
	 */
	public void selectByPartsIdentifiers(Collection<PartsIdentifier> partsIdentifiers) {
		boolean modified = false;
		if (partsIdentifiers != null) {
			for (PartsIdentifier partsIdentifier : partsIdentifiers) {
				for (ImportPartsModel rowModel : elements) {
					if (rowModel.getPartsIdentifier().equals(partsIdentifier)) {
						if (!rowModel.isChecked()) {
							rowModel.setChecked(true);
							modified = true;
						}
					}
				}
			}
		}
		if (modified) {
			fireTableDataChanged();
		}
	}
	
	public void setCheck(int[] selRows, boolean checked) {
		if (selRows == null || selRows.length == 0) {
			return;
		}
		Arrays.sort(selRows);
		for (int selRow : selRows) {
			ImportPartsModel rowModel = getRow(selRow);
			rowModel.setChecked(checked);
		}
		fireTableRowsUpdated(selRows[0], selRows[selRows.length - 1]);
	}
}



/**
 * プリセット選択パネル
 * 
 * @author seraphy
 */
class ImportPresetSelectPanel extends ImportWizardCardPanel {

	private static final long serialVersionUID = 1L;

	public static final String PANEL_NAME = "importPresetSelectPanel"; 
	
	private ImportPresetTableModel presetTableModel;
	
	private ImportWizardDialog parent;
	
	private JTable presetTable;

	private Action actSelectAll;
	
	private Action actDeselectAll;
	
	private Action actSelectUsedParts;

	public ImportPresetSelectPanel() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);

		setBorder(BorderFactory.createTitledBorder(strings.getProperty("preset.title")));

		setLayout(new BorderLayout());

		presetTableModel = new ImportPresetTableModel();

		presetTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					fireChangeEvent();
				}
			}
		});

		AppConfig appConfig = AppConfig.getInstance();
		final Color warningForegroundColor = appConfig.getExportPresetWarningsForegroundColor();
		final Color disabledForeground = appConfig.getDisabledCellForgroundColor();

		presetTable = new JTable(presetTableModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);

				if (comp instanceof JCheckBox) {
					// BooleanのデフォルトのレンダラーはJCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}

				ImportPresetModel presetModel = presetTableModel.getRow(row);
				
				// インポート先のプリセットを上書きする場合、もしくはデフォルトのパーツセットの場合は太字にする.
				if (presetModel.isOverwrite() || presetTableModel.isDefaultPartsSet(row)) {
					comp.setFont(getFont().deriveFont(Font.BOLD));
				} else {
					comp.setFont(getFont());
				}

				// インポートするプリセットのパーツが不足している場合、警告色にする.
				if (!isEnabled()) {
					comp.setForeground(disabledForeground);

				} else {
					if (presetModel.isCheched()
							&& presetModel.getMissingPartsIdentifiers().size() > 0) {
						comp.setForeground(warningForegroundColor);
					} else {
						comp.setForeground(getForeground());
					}
				}
				return comp;
			}
		};
		presetTable.setShowGrid(true);
		presetTable.setGridColor(appConfig.getGridColor());
		presetTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		actSelectUsedParts = new AbstractAction(strings.getProperty("preset.popup.selectUsedParts")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				exportUsedParts();
			}
		};

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(actSelectUsedParts);

		presetTable.setComponentPopupMenu(popupMenu);

		presetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		presetTableModel.adjustColumnModel(presetTable.getColumnModel());

		add(new JScrollPane(presetTable), BorderLayout.CENTER);

		actSelectAll = new AbstractAction(strings.getProperty("parts.btn.selectAll")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onSelectAll();
			}
		};
		actDeselectAll = new AbstractAction(strings.getProperty("parts.btn.deselectAll")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onDeselectAll();
			}
		};
		Action actSort = new AbstractAction(strings
				.getProperty("parts.btn.sort")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onSort();
			}
		};

		JPanel btnPanel = new JPanel();
		GridBagLayout btnPanelLayout = new GridBagLayout();
		btnPanel.setLayout(btnPanelLayout);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.ipadx = 0;
		gbc.ipady = 0;
		JButton btnSelectAll = new JButton(actSelectAll);
		btnPanel.add(btnSelectAll, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		JButton btnDeselectAll = new JButton(actDeselectAll);
		btnPanel.add(btnDeselectAll, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		JButton btnSort = new JButton(actSort);
		btnPanel.add(btnSort, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);

		add(btnPanel, BorderLayout.SOUTH);
	}

	@Override
	public void onActive(ImportWizardDialog parent,
			ImportWizardCardPanel previousPanel) {
		this.parent= parent;

		actSelectUsedParts.setEnabled(parent.importTypeSelectPanel.isImportPartsImages());
		checkMissingParts();
	}
	
	public void checkMissingParts() {
		Collection<PartsIdentifier> importedPartsIdentifiers = this.parent.importPartsSelectPanel.getImportedPartsIdentifiers();
		presetTableModel.checkMissingParts(importedPartsIdentifiers);
	}

	protected void onSelectAll() {
		presetTableModel.selectAll();
	}

	protected void onDeselectAll() {
		presetTableModel.deselectAll();
	}

	protected void onSort() {
		presetTableModel.sort();
		if (presetTableModel.getRowCount() > 0) {
			Rectangle rct = presetTable.getCellRect(0, 0, true);
			presetTable.scrollRectToVisible(rct);
		}
	}

	protected void exportUsedParts() {
		ArrayList<PartsIdentifier> requirePartsIdentifiers = new ArrayList<PartsIdentifier>();
		int[] selRows = presetTable.getSelectedRows();
		for (int selRow : selRows) {
			ImportPresetModel presetModel = presetTableModel.getRow(selRow);
			PartsSet partsSet = presetModel.getPartsSet();
			for (List<PartsIdentifier> partsIdentifiers : partsSet.values()) {
				for (PartsIdentifier partsIdentifier : partsIdentifiers) {
					requirePartsIdentifiers.add(partsIdentifier);
				}
			}
		}
		this.parent.importPartsSelectPanel.selectByPartsIdentifiers(requirePartsIdentifiers);
		checkMissingParts();
	}
	
	@Override
	public boolean isReadyPrevious() {
		return true;
	}
	
	@Override
	public boolean isReadyNext() {
		return false;
	}
	
	@Override
	public boolean isReadyFinish() {
		if (this.parent != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public String doPrevious() {
		
		return ImportPartsSelectPanel.PANEL_NAME;
	}
	
	@Override
	public String doNext() {
		return null;
	}
	
	public Collection<PartsSet> getSelectedPartsSets() {
		return presetTableModel.getSelectedPartsSets();
	}
	
	/**
	 * デフォルトのパーツセットIDとして使用されることが推奨されるパーツセットIDを取得する.<br>
	 * 明示的なデフォルトのパーツセットIDがなければ、もしくは、 明示的に指定されているパーツセットIDが選択されているパーツセットの中になければ、
	 * 選択されているパーツセットの最初のアイテムを返す.<br>
	 * 選択しているパーツセットが一つもなければnullを返す.<br>
	 * 
	 * @return デフォルトのパーツセット
	 */
	public String getPrefferedDefaultPartsSetId() {
		String defaultPartsSetId = presetTableModel.getDefaultPartsSetId();
		String firstPartsSetId = null;
		boolean existsDefaultPartsSetId = false;
		for (PartsSet partsSet : getSelectedPartsSets()) {
			if (firstPartsSetId == null) {
				firstPartsSetId = partsSet.getPartsSetId();
			}
			if (partsSet.getPartsSetId().equals(defaultPartsSetId)) {
				existsDefaultPartsSetId = true;
			}
		}
		if (!existsDefaultPartsSetId || defaultPartsSetId == null || defaultPartsSetId.length() == 0) {
			defaultPartsSetId = firstPartsSetId;
		}
		return defaultPartsSetId;
	}
	
	public void initModel(Collection<PartsSet> partsSets, String defaultPartsSetId, CharacterData presetImportTarget) {
		presetTableModel.initModel(partsSets, defaultPartsSetId, presetImportTarget);
	}
}

class ImportPresetModel {

	private boolean cheched;
	
	private PartsSet partsSet;
	
	private boolean overwrite;
	
	private Collection<PartsIdentifier> missingPartsIdentifiers = Collections.emptySet();
	
	public ImportPresetModel(PartsSet partsSet, boolean overwrite, boolean checked) {
		if (partsSet == null) {
			throw new IllegalArgumentException();
		}
		this.partsSet = partsSet;
		this.cheched = checked;
		this.overwrite = overwrite;
	}
	
	public boolean isCheched() {
		return cheched;
	}
	
	public void setCheched(boolean cheched) {
		this.cheched = cheched;
	}
	
	public PartsSet getPartsSet() {
		return partsSet;
	}
	
	public String getPartsSetName() {
		return partsSet.getLocalizedName();
	}
	
	public void setPartsSetName(String name) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		partsSet.setLocalizedName(name);
	}
	
	public Collection<PartsIdentifier> getMissingPartsIdentifiers() {
		return missingPartsIdentifiers;
	}
	
	public boolean hasMissingParts() {
		return true;
	}
	
	public boolean isOverwrite() {
		return overwrite;
	}
	
	public boolean checkMissingParts(Collection<PartsIdentifier> importedPartsIdentifiers) {
		HashSet<PartsIdentifier> missingPartsIdentifiers = new HashSet<PartsIdentifier>();
		for (List<PartsIdentifier> partsIdentifiers : partsSet.values()) {
			for (PartsIdentifier partsIdentifier : partsIdentifiers) {
				boolean exists = false;
				if (importedPartsIdentifiers != null && importedPartsIdentifiers.contains(partsIdentifier)) {
					exists = true;
				}
				if (!exists) {
					missingPartsIdentifiers.add(partsIdentifier);
				}
			}
		}
		
		boolean modified = (!missingPartsIdentifiers.equals(this.missingPartsIdentifiers));
		if (modified) {
			this.missingPartsIdentifiers = missingPartsIdentifiers;
		}
		return modified;
	}
}

class ImportPresetTableModel extends AbstractTableModelWithComboBoxModel<ImportPresetModel> {

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMN_NAMES; // = {"選択", "プリセット名",
												// "不足するパーツ"};
	
	private static final int[] COLUMN_WIDTHS; // = {50, 100, 200};
	
	static {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ImportWizardDialog.STRINGS_RESOURCE);
		
		COLUMN_NAMES = new String[] {
				strings.getProperty("preset.column.check"),
				strings.getProperty("preset.column.name"),
				strings.getProperty("preset.column.missings"),
		};
		
		COLUMN_WIDTHS = new int[] {
				Integer.parseInt(strings.getProperty("preset.column.check.size")),
				Integer.parseInt(strings.getProperty("preset.column.name.size")),
				Integer.parseInt(strings.getProperty("preset.column.missings.size")),
		};
	}
	
	private String defaultPartsSetId;
	
	public String getDefaultPartsSetId() {
		return defaultPartsSetId;
	}
	
	public void setDefaultPartsSetId(String defaultPartsSetId) {
		this.defaultPartsSetId = defaultPartsSetId;
	}
	
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return Boolean.class;
		case 1:
			return String.class;
		case 2:
			return String.class;
		}
		return String.class;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0 || columnIndex == 1) {
			return true;
		}
		return false;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		ImportPresetModel rowModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return rowModel.isCheched();
		case 1:
			return rowModel.getPartsSetName();
		case 2:
			return getMissingPartsIdentifiersString(rowModel);
		}
		return "";
	}
	
	private String getMissingPartsIdentifiersString(ImportPresetModel rowModel) {
		StringBuilder buf = new StringBuilder();
		for (PartsIdentifier partsIdentifier : rowModel.getMissingPartsIdentifiers()) {
			if (buf.length() > 0) {
				buf.append(", ");
			}
			buf.append(partsIdentifier.getLocalizedPartsName());
		}
		return buf.toString();
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ImportPresetModel rowModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			rowModel.setCheched(((Boolean) aValue).booleanValue());
			break;
		case 1:
			String name = (String) aValue;
			name = (name != null) ? name.trim() : "";
			if (name.length() > 0) {
				rowModel.setPartsSetName(name);
			}
		default:
			return;
		}
		fireTableRowsUpdated(rowIndex, rowIndex);
	}

	/**
	 * 指定した行のパーツセットがデフォルトパーツセットであるか?
	 * 
	 * @param rowIndex
	 *            行インデックス
	 * @return デフォルトパーツセットであればtrue、そうでなければfalse
	 */
	public boolean isDefaultPartsSet(int rowIndex) {
		ImportPresetModel rowModel = getRow(rowIndex);
		return rowModel.getPartsSet().getPartsSetId().equals(defaultPartsSetId);
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		int mx = columnModel.getColumnCount();
		for (int idx = 0; idx < mx; idx++) {
			columnModel.getColumn(idx).setWidth(COLUMN_WIDTHS[idx]);
		}
	}
	
	/**
	 * パーツセットリストを構築する.<br>
	 * 
	 * @param partsSets
	 *            登録するパーツセット
	 * @param defaultPartsSetId
	 *            デフォルトのパーツセットID、なければnull
	 * @param presetImportTarget
	 *            インポート先、新規の場合はnull (上書き判定のため)
	 */
	public void initModel(Collection<PartsSet> partsSets, String defaultPartsSetId, CharacterData presetImportTarget) {
		clear();
		if (partsSets == null) {
			return;
		}

		// インポート先の既存のパーツセット
		Map<String, PartsSet> currentProfilesPartsSet;
		if (presetImportTarget != null) {
			currentProfilesPartsSet = presetImportTarget.getPartsSets();
		} else {
			// 新規の場合は既存パーツセットは空.
			currentProfilesPartsSet = Collections.emptyMap();
		}
		
		// インポートもとのパーツセットをテープルモデルに登録する.
		for (PartsSet partsSet : partsSets) {
			String partsSetId = partsSet.getPartsSetId();
			if (partsSetId == null || partsSetId.length() == 0) {
				continue;
			}
			PartsSet compatiblePartsSet;
			if (presetImportTarget != null) {
				// 既存のキャラクター定義へのインポート時は、パーツセットのカテゴリを合わせる.
				// 一つもカテゴリが合わない場合は空のパーツセットになる.
				compatiblePartsSet = partsSet.createCompatible(presetImportTarget);
			} else {
				compatiblePartsSet = partsSet; // 新規の場合はフィッティングの必要なし.
			}
			if (!compatiblePartsSet.isEmpty()) {
				// 空のパーツセットは登録対象にしない.
				boolean overwrite = currentProfilesPartsSet.containsKey(partsSetId);
				boolean checked = (presetImportTarget == null); // 新規の場合は既定で選択状態とする.
				ImportPresetModel rowModel = new ImportPresetModel(partsSet, overwrite, checked);
				addRow(rowModel);
			}
		}
		
		// デフォルトのパーツセットIDを設定、存在しない場合はnull
		this.defaultPartsSetId = defaultPartsSetId;
		
		sort();
	}
	
	public Collection<PartsSet> getSelectedPartsSets() {
		ArrayList<PartsSet> partsSets = new ArrayList<PartsSet>();
		for (ImportPresetModel rowModel : elements) {
			if (rowModel.isCheched()) {
				partsSets.add(rowModel.getPartsSet());
			}
		}
		return partsSets;
	}

	public void selectAll() {
		boolean modified = false;
		for (ImportPresetModel rowModel : elements) {
			if (!rowModel.isCheched()) {
				rowModel.setCheched(true);
				modified = true;
			}
		}
		if (modified) {
			fireTableDataChanged();
		}
	}
	
	public void deselectAll() {
		boolean modified = false;
		for (ImportPresetModel rowModel : elements) {
			if (rowModel.isCheched()) {
				rowModel.setCheched(false);
				modified = true;
			}
		}
		if (modified) {
			fireTableDataChanged();
		}
	}
	
	public void sort() {
		Collections.sort(elements, new Comparator<ImportPresetModel>() {
			public int compare(ImportPresetModel o1, ImportPresetModel o2) {
				int ret = (o1.isCheched() ? 0 : 1) - (o2.isCheched() ? 0 : 1);
				if (ret == 0) {
					ret = o1.getPartsSetName().compareTo(o2.getPartsSetName());
				}
				return ret;
			}
		});
		fireTableDataChanged();
	}
	
	public void checkMissingParts(Collection<PartsIdentifier> importedPartsIdentifiers) {
		boolean changed = false;
		
		for (ImportPresetModel rowModel : elements) {
			if (rowModel.checkMissingParts(importedPartsIdentifiers)) {
				changed = true;
			}
		}
		
		if (changed) {
			fireTableDataChanged();
		}
	}
}

