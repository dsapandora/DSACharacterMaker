package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import charactermanaj.Main;
import charactermanaj.clipboardSupport.ClipboardUtil;
import charactermanaj.graphics.io.FileImageResource;
import charactermanaj.graphics.io.ImageCachedLoader;
import charactermanaj.graphics.io.LoadedImage;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.CharacterDataChangeObserver;
import charactermanaj.model.io.CharacterDataDefaultProvider;
import charactermanaj.model.io.CharacterDataPersistent;
import charactermanaj.model.io.PartsImageDirectoryWatchAgent;
import charactermanaj.model.io.PartsImageDirectoryWatchAgentFactory;
import charactermanaj.ui.util.FileDropTarget;
import charactermanaj.ui.util.SingleRootFileSystemView;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.UIHelper;


/**
 * プロファイルを選択するためのダイアログ、およびデフォルトプロファイルを開く
 * 
 * @author seraphy
 */
public class ProfileSelectorDialog extends JDialog {

	private static final long serialVersionUID = -6883202891172975022L;

	private static final Logger logger = Logger.getLogger(ProfileSelectorDialog.class.getName());


	protected static final String STRINGS_RESOURCE = "languages/profileselectordialog";

	/**
	 * サンプルイメージをロードするためのローダー
	 */
	private ImageCachedLoader imageLoader = new ImageCachedLoader();


	/**
	 * サンプルイメージファイルが保存可能であるか?<br>
	 * 有効なキャラクターデータが選択されており、サンプルイメージの更新が許可されていればtrue.<br>
	 */
	private boolean canWriteSamplePicture;

	/**
	 * サンプルイメージを表示するパネル
	 */
	private SamplePicturePanel sampleImgPanel;

	private Action actOK;

	private Action actCancel;

	private Action actProfileNew;

	private Action actProfileCopy;

	private Action actProfileEdit;

	private Action actProfileRemove;

	private Action actProfileBrowse;

	private Action actProfileImport;

	private Action actProfileExport;

	private Action actProfileTemplate;

	/**
	 * プロファイル一覧を表示するリストコンポーネント
	 */
	private JTable characterList;

	/**
	 * プロファイル一覧のリストモデル
	 */
	private ProfileSelectorTableModel characterListModel;

	/**
	 * プロファイルの説明用テキストエリア
	 */
	private JTextArea descriptionArea;



	/**
	 * ダイアログをOKで閉じた場合に選択していたキャラクターデータを示す.<br>
	 * nullの場合はキャンセルを意味する.
	 */
	private CharacterData selectedCharacterData;


	/**
	 * プロファイルの選択ダイアログを構築する.
	 * 
	 * @param parent
	 *            親フレーム、もしくはnull
	 * @param characterDatas
	 *            プロファイルのリスト
	 */
	public ProfileSelectorDialog(JFrame parent, List<CharacterData> characterDatas) {
		super(parent, true);
		if (characterDatas == null) {
			throw new IllegalArgumentException();
		}

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClosing();
			}
		});

		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		setTitle(strings.getProperty("title"));

		JPanel pnlProfiles = new JPanel(new BorderLayout());

		characterListModel = new ProfileSelectorTableModel();
		characterListModel.setModel(characterDatas);

		characterList = new JTable(characterListModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {

				CharacterData cd = characterListModel.getRow(row);

				Component comp = super.prepareRenderer(renderer, row, column);
				if (ProfileListManager.isUsingCharacterData(cd)) {
					// 使用中のものは太字で表示
					Font f = comp.getFont();
					comp.setFont(f.deriveFont(Font.BOLD));
				}

				if (!cd.canWrite()) {
					// 書き込み不可のものはイタリックで表示
					Font f = comp.getFont();
					comp.setFont(f.deriveFont(Font.ITALIC));
				}
				return comp;
			}
		};
		characterList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		characterListModel.adjustColumnModel(characterList.getColumnModel());

		characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		characterList.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					updateUIState();
				}
			}
		});
		characterList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// 正確に2回
					onOK();
				}
			}
		});

		JScrollPane characterListSP = new JScrollPane(characterList);
		characterListSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		characterListSP
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		pnlProfiles.add(characterListSP, BorderLayout.CENTER);

		actOK = new AbstractAction(strings.getProperty("btn.select")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		};

		actCancel = new AbstractAction(strings.getProperty("btn.cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		};

		actProfileNew = new AbstractAction(strings.getProperty("profile.new")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileNew(true);
			}
		};

		actProfileCopy = new AbstractAction(strings.getProperty("profile.copy")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileNew(false);
			}
		};

		actProfileEdit = new AbstractAction(strings.getProperty("profile.edit")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileEdit();
			}
		};

		actProfileRemove = new AbstractAction(
				strings.getProperty("profile.remove")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileRemove();
			}
		};

		actProfileBrowse = new AbstractAction(
				strings.getProperty("profile.browse")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileBrowse();
			}
		};

		actProfileImport = new AbstractAction(
				strings.getProperty("profile.import")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileImport();
			}
		};

		actProfileExport = new AbstractAction(
				strings.getProperty("profile.export")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileExport();
			}
		};

		actProfileTemplate = new AbstractAction(
				strings.getProperty("profile.template")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onProfileTemplate();
			}
		};

		final JPopupMenu popupTblMenu = new JPopupMenu();
		popupTblMenu.add(new JMenuItem(actOK));
		popupTblMenu.add(new JSeparator());
		popupTblMenu.add(new JMenuItem(actProfileCopy));
		popupTblMenu.add(new JMenuItem(actProfileEdit));
		popupTblMenu.add(new JMenuItem(actProfileRemove));
		popupTblMenu.add(new JSeparator());
		popupTblMenu.add(new JMenuItem(actProfileBrowse));
		popupTblMenu.add(new JMenuItem(actProfileImport));
		popupTblMenu.add(new JMenuItem(actProfileExport));
		popupTblMenu.add(new JSeparator());
		popupTblMenu.add(new JMenuItem(actProfileTemplate));

		characterList.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				evaluatePopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				evaluatePopup(e);
			}
			private void evaluatePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popupTblMenu.show(characterList, e.getX(), e.getY());
				}
			}
		});

		JButton btnProfileNew = new JButton(actProfileNew);
		JButton btnProfileCopy = new JButton(actProfileCopy);
		JButton btnProfileEdit = new JButton(actProfileEdit);
		JButton btnProfileRemove = new JButton(actProfileRemove);
		JButton btnProfileBrowse = new JButton(actProfileBrowse);
		JButton btnProfileImport = new JButton(actProfileImport);
		JButton btnProfileExport = new JButton(actProfileExport);
		JButton btnProfileTemplate = new JButton(actProfileTemplate);

		JPanel pnlProfileEditButtons = new JPanel();
		pnlProfileEditButtons.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(0, 3, 0, 3);
		pnlProfileEditButtons.add(btnProfileNew, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		pnlProfileEditButtons.add(btnProfileCopy, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		pnlProfileEditButtons.add(btnProfileEdit, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		pnlProfileEditButtons.add(btnProfileRemove, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weighty = 1.;
		pnlProfileEditButtons.add(Box.createGlue(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.weighty = 0.;
		pnlProfileEditButtons.add(btnProfileBrowse, gbc);

		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.weighty = 0.;
		pnlProfileEditButtons.add(btnProfileImport, gbc);

		gbc.gridx = 0;
		gbc.gridy = 7;
		pnlProfileEditButtons.add(btnProfileExport, gbc);

		gbc.gridx = 0;
		gbc.gridy = 8;
		pnlProfileEditButtons.add(btnProfileTemplate, gbc);

		JPanel pnlProfilesGroup = new JPanel(new BorderLayout());
		pnlProfilesGroup.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createEmptyBorder(3, 3, 3, 3), BorderFactory
				.createTitledBorder(strings.getProperty("profiles"))));
		pnlProfilesGroup.add(pnlProfiles, BorderLayout.CENTER);
		pnlProfilesGroup.add(pnlProfileEditButtons, BorderLayout.EAST);

		JPanel infoPanel = new JPanel(new GridLayout(1, 2));
		JPanel descriptionPanel = new JPanel(new BorderLayout());
		descriptionPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3), BorderFactory
						.createTitledBorder(strings.getProperty("description"))));

		descriptionArea = new JTextArea();
		descriptionArea.setEditable(false);
		descriptionPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

		// サンプルピクャパネル
		sampleImgPanel = new SamplePicturePanel();

		// サンプルピクチャファイルのドラッグアンドドロップ
		new DropTarget(sampleImgPanel, new FileDropTarget() {
			@Override
			protected void onDropFiles(final List<File> dropFiles) {
				if (dropFiles == null || dropFiles.isEmpty()) {
					return;
				}
				// インポートダイアログを開く.
				// ドロップソースの処理がブロッキングしないように、
				// ドロップハンドラの処理を終了して呼び出す.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						onDrop(dropFiles);
					}
				});
			}
			@Override
			protected void onException(Exception ex) {
				ErrorMessageHelper.showErrorDialog(ProfileSelectorDialog.this, ex);
			}
		});

		// サンプルピクチャのコンテキストメニュー
		final JPopupMenu popupMenu = new JPopupMenu();
		final JMenuItem popupMenuCut = popupMenu.add(new AbstractAction(strings.getProperty("samplepicture.cut")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSamplePictureCut();
			}
		});
		final JMenuItem popupMenuPaste = popupMenu.add(new AbstractAction(strings.getProperty("samplepicture.paste")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSamplePicturePaste();
			}
		});
		sampleImgPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				evaluatePopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				evaluatePopup(e);
			}
			private void evaluatePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popupMenuCut.setEnabled(sampleImgPanel.getSamplePictrue() != null && canWriteSamplePicture);
					popupMenuPaste.setEnabled(canWriteSamplePicture && ClipboardUtil.hasImage());
					popupMenu.show(sampleImgPanel, e.getX(), e.getY());
				}
			}
		});

		JScrollPane sampleImgPanelSp = new JScrollPane(sampleImgPanel);
		sampleImgPanelSp.setBorder(null);
		JPanel sampleImgTitledPanel = new JPanel(new BorderLayout());
		sampleImgTitledPanel.add(sampleImgPanelSp, BorderLayout.CENTER);
		sampleImgTitledPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3), BorderFactory
						.createTitledBorder(strings.getProperty("sample-image"))));

		infoPanel.add(descriptionPanel);
		infoPanel.add(sampleImgTitledPanel);

		JSplitPane centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		centerPane.setResizeWeight(1.f); // ウィンドウサイズ変更時に上を可変とする.
		centerPane.setDividerLocation(Integer.parseInt(strings
				.getProperty("dividerLocation")));

		centerPane.add(pnlProfilesGroup);
		centerPane.add(infoPanel);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(centerPane, BorderLayout.CENTER);

		// OK/CANCEL ボタンパネル

		JPanel btnPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(btnPanel, BoxLayout.LINE_AXIS);
		btnPanel.setLayout(boxLayout);
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 42));

		JButton btnOK = new JButton(actOK);
		JButton btnCancel = new JButton(actCancel);
		if (Main.isLinuxOrMacOSX()) {
			btnPanel.add(btnCancel);
			btnPanel.add(btnOK);
		} else {
			btnPanel.add(btnOK);
			btnPanel.add(btnCancel);
		}

		JPanel btnPanel2 = new JPanel(new BorderLayout());
		btnPanel2.add(btnPanel, BorderLayout.EAST);

		contentPane.add(btnPanel2, BorderLayout.SOUTH);

		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(btnOK);

		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeProfileSelectorDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeProfileSelectorDialog");
		am.put("closeProfileSelectorDialog", actCancel);

		int width = Integer.parseInt(strings.getProperty("windowWidth"));
		int height = Integer.parseInt(strings.getProperty("windowHeight"));
		setSize(width, height);
		setLocationRelativeTo(parent);

		characterList.requestFocus();
		updateUIState();
	}

	public CharacterData getSelectedCharacterData() {
		return selectedCharacterData;
	}

	/**
	 * キャラクターデータの選択変更に伴い、ボタンやサンプルピクチャなどを切り替える.
	 */
	protected void updateUIState() {
		CharacterData characterData = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			characterData = characterListModel.getRow(selRow);
		}

		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		boolean selected = (characterData != null);
		boolean enableEdit = (characterData != null)
				&& characterData.canWrite();

		actOK.setEnabled(selected);

		actProfileNew.setEnabled(true);
		actProfileCopy.setEnabled(selected);
		actProfileEdit.setEnabled(selected);
		actProfileRemove.setEnabled(selected && enableEdit);
		actProfileImport.setEnabled(true);
		actProfileExport.setEnabled(selected);
		actProfileBrowse.setEnabled(selected);
		actProfileTemplate.setEnabled(selected);

		if (enableEdit) {
			actProfileEdit.putValue(Action.NAME,
					strings.getProperty("profile.edit"));
		} else {
			actProfileEdit.putValue(Action.NAME,
					strings.getProperty("profile.edit.readonly"));
		}

		// 有効なキャラクターデータであり、且つ、書き込み可能であり、且つ、使用中でなければ削除可能
		boolean removable = characterData != null && characterData.isValid()
				&& !ProfileListManager.isUsingCharacterData(characterData)
				&& characterData.canWrite();

		actProfileRemove.setEnabled(removable);

		boolean canWriteSamplePicture = false;
		BufferedImage sampleImage = null;

		if (characterData != null && characterData.isValid()) {
			// description
			StringWriter sw = new StringWriter();
			PrintWriter descriptionBuf = new PrintWriter(sw);
			URI docBase = characterData.getDocBase();
			String author = characterData.getAuthor();
			String description = characterData.getDescription();
			if (docBase != null) {
				descriptionBuf.println("configuration: " + docBase);
			}
			if (author != null && author.length() > 0) {
				descriptionBuf.println("author: " + author);
			}
			Dimension imageSize = characterData.getImageSize();
			if (imageSize != null) {
				descriptionBuf.println("size: "
						+ imageSize.width + "x"	+ imageSize.height);
			}
			if (description != null) {
				description = description.replace("\r\n", "\n");
				description = description.replace("\r", "\n");
				description = description.replace("\n", System.getProperty("line.separator"));
				descriptionBuf.println(description);
			}
			descriptionArea.setText(sw.toString());
			descriptionArea.setSelectionStart(0);
			descriptionArea.setSelectionEnd(0);

			// sample picture
			try {
				CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
				sampleImage = persist.loadSamplePicture(characterData, imageLoader);
				canWriteSamplePicture = persist.canSaveSamplePicture(characterData);

			} catch (Exception ex) {
				// サンプルピクチャの読み込みに失敗したら、サンプルピクチャを表示しないだけで処理は継続する.
				logger.log(Level.WARNING, "sample picture loading failed. " + characterData , ex);
				sampleImage = null;
			}
		}

		this.canWriteSamplePicture = canWriteSamplePicture;

		String dropHere = strings.getProperty("dropHere");
		String noPicture = strings.getProperty("nopicture");
		sampleImgPanel.setSamplePicture(sampleImage);
		sampleImgPanel.setAlternateText(canWriteSamplePicture ? dropHere : noPicture);
	}


	/**
	 * サンプルピクチャのファイルを削除し、表示されている画像をクリップボードに保存する
	 */
	protected void onSamplePictureCut() {
		int selRow = characterList.getSelectedRow();
		if (selRow < 0) {
			return;
		}
		CharacterData characterData = characterListModel.getRow(selRow);

		BufferedImage img = sampleImgPanel.getSamplePictrue();

		Toolkit tk = Toolkit.getDefaultToolkit();
		if (characterData == null || !characterData.isValid() || !canWriteSamplePicture || img == null) {
			tk.beep();
			return;
		}

		try {
			// クリップボードにイメージをセット
			Color bgColor = AppConfig.getInstance().getSampleImageBgColor();
			ClipboardUtil.setImage(img, bgColor);

			// サンプルピクチャを削除
			CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
			persist.saveSamplePicture(characterData, null);

			// プレビューを更新
			sampleImgPanel.setSamplePicture(null);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * サンプルピクチャをクリップボードから貼り付け、それをファイルに保存する
	 */
	protected void onSamplePicturePaste() {
		CharacterData characterData = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			characterData = characterListModel.getRow(selRow);
		}

		Toolkit tk = Toolkit.getDefaultToolkit();
		if (characterData == null || !characterData.isValid() || !canWriteSamplePicture) {
			tk.beep();
			return;
		}

		try {
			BufferedImage img = ClipboardUtil.getImage();
			if (img != null) {
				// 画像が読み込まれた場合、それを保存する.
				CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
				persist.saveSamplePicture(characterData, img);

				sampleImgPanel.setSamplePicture(img);

			} else {
				// サンプルピクチャは更新されなかった。
				tk.beep();
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * サンプルピクチャパネルにドロップされた画像ファイルをサンプルピクチャとしてコピーします.<br>
	 * 
	 * @param dtde
	 *            ドロップイベント
	 */
	protected void onDrop(Collection<File> dropFiles) {
		CharacterData characterData = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			characterData = characterListModel.getRow(selRow);;
		}

		Toolkit tk = Toolkit.getDefaultToolkit();
		if (dropFiles == null || dropFiles.isEmpty()
				|| !canWriteSamplePicture || characterData == null
				|| !characterData.isValid() || !canWriteSamplePicture) {
			tk.beep();
			return;
		}

		try {
			// 最初のファィルのみ取得する.
			File dropFile = dropFiles.iterator().next();

			// ドロップファイルがあれば、イメージとして読み込む
			BufferedImage img = null;
			if (dropFile != null && dropFile.isFile() && dropFile.canRead()) {
				try {
					LoadedImage loadedImage = imageLoader.load(new FileImageResource(dropFile));
					img = loadedImage.getImage();

				} catch (IOException ex) {
					// イメージのロードができない = 形式が不正であるなどの場合は
					// 読み込みせず、継続する.
					img = null;
				}
			}
			if (img != null) {
				// 画像が読み込まれた場合、それを保存する.
				CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
				persist.saveSamplePicture(characterData, img);

				sampleImgPanel.setSamplePicture(img);

			} else {
				// サンプルピクチャは更新されなかった。
				tk.beep();
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	@Override
	public void dispose() {
	    imageLoader.close();
	    super.dispose();
	}

	/**
	 * 閉じる場合
	 */
	protected void onClosing() {
		dispose();
	}

	/**
	 * OKボタン押下
	 */
	protected void onOK() {
		selectedCharacterData = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			selectedCharacterData = characterListModel.getRow(selRow);
		}
		if (selectedCharacterData == null) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		dispose();
	}

	/**
	 * キャンセルボタン押下
	 */
	protected void onCancel() {
		selectedCharacterData = null;
		onClosing();
	}

	/**
	 * プロファイルの作成
	 * 
	 * @param makeDefault
	 *            デフォルトのプロファイルで作成する場合
	 */
	protected void onProfileNew(boolean makeDefault) {
		CharacterData cd = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			cd = characterListModel.getRow(selRow);
		}

		CharacterDataPersistent persist = CharacterDataPersistent.getInstance();

		if (makeDefault || cd == null) {
			try {
				final Properties strings = LocalizedResourcePropertyLoader
						.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

				// キャラクターデータ選択用のコンボボックスの準備
				JComboBox comboTemplates = new JComboBox();
				comboTemplates.setEditable(false);

				final JLabel lbl = new JLabel();
				comboTemplates.setRenderer(new ListCellRenderer() {
					public Component getListCellRendererComponent(JList list,
							Object value, int index, boolean isSelected,
							boolean cellHasFocus) {
						@SuppressWarnings("unchecked")
						Map.Entry<String, String> entry = (Map.Entry<String, String>) value;
						if (entry != null) {
							lbl.setText(entry.getValue());
						}
						return lbl;
					}
				});

				// キャラクターデータのテンプレートを一覧登録する.
				CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
				for (final Map.Entry<String, String> entry : defProv
						.getCharacterDataTemplates().entrySet()) {
					comboTemplates.addItem(entry);
				}

				// コンボボックスの幅を広げる.
				// (短いとInputBoxのタイトルが隠れるため)
				Dimension preferredSize = comboTemplates.getPreferredSize();
				int comboWidth = Integer.parseInt(strings
						.getProperty("profileNew.chooseTemplate.combo.width"));
				preferredSize.width = Math.max(preferredSize.width, comboWidth);
				comboTemplates.setPreferredSize(preferredSize);

				int ret = JOptionPane.showConfirmDialog(this, comboTemplates,
						strings.getProperty("profileNew.chooseTemplate.title"),
						JOptionPane.OK_CANCEL_OPTION);
				if (ret != JOptionPane.OK_OPTION) {
					// キャンセルされた場合
					return;
				}

				@SuppressWarnings("unchecked")
				Map.Entry<String, String> selection = (Map.Entry<String, String>) comboTemplates
						.getSelectedItem();
				if (selection == null) {
					// 未選択の場合
					return;
				}

				// テンプレートを読み込む
				String characterXmlName = selection.getKey();
				cd = defProv.loadPredefinedCharacterData(characterXmlName);

			} catch (Exception ex) {
				ErrorMessageHelper.showErrorDialog(this, ex);
				return;
			}
		}

		// 基本情報をコピーします。
		CharacterData newCd = cd.duplicateBasicInfo();
		// DocBaseはnullにする。これにより新規作成と判断される.
		newCd.setDocBase(null);

		// 新規なのでパーツセット情報はリセットする
		newCd.clearPartsSets(false);

		ProfileEditDialog editDlg = new ProfileEditDialog(this, newCd);
		editDlg.setVisible(true);

		newCd = editDlg.getResult();
		if (newCd == null) {
			// キャンセルされた場合
			return;
		}

		// 新規プロファイルを保存する.
		try {
			persist.createProfile(newCd);
			persist.saveFavorites(newCd);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}

		// 作成されたプロファイルを一覧に追加する.
		characterListModel.add(newCd);
	}

	/**
	 * プロファィルの編集
	 */
	protected void onProfileEdit() {
		CharacterData cd = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			cd = characterListModel.getRow(selRow);
		}
		if (cd == null || !cd.isValid()) {
			return;
		}

		try {
			// プロファイル編集ダイアログを開き、その結果を取得する.
			CharacterData newCd = ProfileListManager.editProfile(this, cd);
			if (newCd == null) {
				// キャンセルされた場合
				return;
			}

			// 現在開いているメインフレームに対してキャラクター定義が変更されたことを通知する.
			CharacterDataChangeObserver.getDefault().notifyCharacterDataChange(
					this, newCd, true, true);

			// プロファイル一覧画面も更新する.
			characterListModel.set(selRow, newCd);
			characterList.repaint();

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}
	}

	/**
	 * プロファイルの削除
	 */
	protected void onProfileRemove() {
		CharacterData cd = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			cd = characterListModel.getRow(selRow);
		}
		if (cd == null || !cd.isValid() || ProfileListManager.isUsingCharacterData(cd) || !cd.canWrite()) {
			// 無効なキャラクター定義であるか、使用中であるか、書き込み不可であれば削除は実行できない.
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		String msgTempl = strings.getProperty("profile.remove.confirm");
		MessageFormat fmt = new MessageFormat(msgTempl);
		String msg = fmt.format(new Object[]{cd.getName()});

		JPanel msgPanel = new JPanel(new BorderLayout(5, 5));
		msgPanel.add(new JLabel(msg), BorderLayout.CENTER);
		JCheckBox chkRemoveForce = new JCheckBox(strings.getProperty("profile.remove.force"));
		msgPanel.add(chkRemoveForce, BorderLayout.SOUTH);

		JOptionPane optionPane = new JOptionPane(msgPanel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION) {
			private static final long serialVersionUID = 1L;
			@Override
			public void selectInitialValue() {
				String noBtnCaption = UIManager.getString("OptionPane.noButtonText");
				for (JButton btn : UIHelper.getInstance().getDescendantOfClass(JButton.class, this)) {
					if (btn.getText().equals(noBtnCaption)) {
						// 「いいえ」ボタンにフォーカスを設定
						btn.requestFocus();
					}
				}
			}
		};
		JDialog dlg = optionPane.createDialog(this, strings.getProperty("confirm.remove"));
		dlg.setVisible(true);
		Object ret = optionPane.getValue();
		if (ret == null || ((Number) ret).intValue() != JOptionPane.YES_OPTION) {
			return;
		}

		if (!cd.canWrite() || cd.getDocBase() == null) {
			JOptionPane.showMessageDialog(this, strings.getProperty("profile.remove.cannot"));
			return;
		}

		boolean forceRemove = chkRemoveForce.isSelected();

		try {
			CharacterDataPersistent persiste = CharacterDataPersistent.getInstance();
			persiste.remove(cd, forceRemove);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}

		// モデルから該当キャラクターを削除して再描画
		characterListModel.remove(selRow);
		characterList.repaint();
		updateUIState();
	}

	/**
	 * 場所を開く
	 */
	protected void onProfileBrowse() {
		CharacterData cd = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			cd = characterListModel.getRow(selRow);
		}
		if (cd == null || !cd.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		try {
			URI docBase = cd.getDocBase();
			if (!DesktopUtilities.browseBaseDir(docBase)) {
				JOptionPane.showMessageDialog(this, docBase);
			}
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * インポート
	 */
	protected void onProfileImport() {
		try {
			CharacterData selCd = null;
			int selRow = characterList.getSelectedRow();
			if (selRow >= 0) {
				selCd = characterListModel.getRow(selRow);
			}

			// 選択したプロファイルを更新するか、新規にプロファイルを作成するか選択できるようにする
			if (selCd != null) {
				final Properties strings = LocalizedResourcePropertyLoader
					.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

				JPanel radioPanel = new JPanel(new BorderLayout());
				JRadioButton btnUpdate = new JRadioButton(strings.getProperty("importToUpdateProfile"));
				JRadioButton btnNew = new JRadioButton(strings.getProperty("importToCreateProfile"));
				ButtonGroup radios = new ButtonGroup();
				radios.add(btnUpdate);
				radios.add(btnNew);
				btnUpdate.setSelected(true);
				radioPanel.add(btnUpdate, BorderLayout.NORTH);
				radioPanel.add(btnNew, BorderLayout.SOUTH);

				int ret = JOptionPane.showConfirmDialog(this, radioPanel,
						strings.getProperty("confirmUpdateProfile"),
						JOptionPane.OK_CANCEL_OPTION);
				if (ret != JOptionPane.OK_OPTION) {
					return;
				}

				if (btnNew.isSelected()) {
					// 選択されていないことにする.
					selCd = null;
				}
			}

			// キャラクターデータをロードし直す.
			CharacterData cd;
			if (selCd != null) {
				cd = selCd.duplicateBasicInfo();
				try {
					ProfileListManager.loadCharacterData(cd);
					ProfileListManager.loadFavorites(cd);

				} catch (IOException ex) {
					ErrorMessageHelper.showErrorDialog(this, ex);
					// 継続する.
				}
			} else {
				cd = null;
			}

			// ディレクトリ監視エージェントの停止
			PartsImageDirectoryWatchAgentFactory agentFactory = PartsImageDirectoryWatchAgentFactory.getFactory();
			PartsImageDirectoryWatchAgent agent = agentFactory.getAgent(cd);
			agent.suspend();
			try {
				// インポートウィザードの実行
				ImportWizardDialog importWizDialog = new ImportWizardDialog(this, cd);
				importWizDialog.setVisible(true);

				CharacterData newCd = importWizDialog.getImportedCharacterData();
				if (importWizDialog.getExitCode() == ImportWizardDialog.EXIT_PROFILE_CREATED) {

					// 作成されたプロファイルを一覧に追加する.
					characterListModel.add(newCd);

				} else if (importWizDialog.getExitCode() == ImportWizardDialog.EXIT_PROFILE_UPDATED) {

					// 更新されたプロファイルを通知する
					CharacterDataChangeObserver.getDefault()
							.notifyCharacterDataChange(this, newCd, true, true);
				}

			} finally {
				agent.resume();
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	/**
	 * エクスポート
	 */
	protected void onProfileExport() {
		CharacterData cd = null;
		int selRow = characterList.getSelectedRow();
		if (selRow >= 0) {
			cd = characterListModel.getRow(selRow);
		}
		if (cd == null || !cd.isValid()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		try {
			// コピーした情報に対してパーツデータをロードする.
			final CharacterData newCd = cd.duplicateBasicInfo();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				ProfileListManager.loadCharacterData(newCd);

			} finally {
				setCursor(Cursor.getDefaultCursor());
			}

			// エクスポートウィザードを表示
			BufferedImage sampleImage = sampleImgPanel.getSamplePictrue();
			ExportWizardDialog exportWizDialog = new ExportWizardDialog(this, newCd, sampleImage);
			exportWizDialog.setVisible(true);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}
	}

	/**
	 * 選択したプロファイルをテンプレートとして登録する.
	 */
	protected void onProfileTemplate() {
		try {
			CharacterData cd = null;
			int selRow = characterList.getSelectedRow();
			if (selRow >= 0) {
				cd = characterListModel.getRow(selRow);
			}
			if (cd == null || !cd.isValid()) {
				Toolkit tk = Toolkit.getDefaultToolkit();
				tk.beep();
				return;
			}

			String defualtName = cd.getId() + "_" + cd.getRev() + ".xml";
			// Windowsでのファイル名として使用禁止の文字を置換する.
			for (char c : "<>|:;*?/\\\"".toCharArray()) {
				defualtName = defualtName.replace(c, '_');
			}

			// カスタマイズ用テンプレートファイルの格納場所を取得する.
			final CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
			final File templDir = defProv.getTemplateDir(true);

			// 指定されたディレクトリ以外に表示・移動できないファイルシステムビューを使用したファイルチューザ
			JFileChooser fileChooser = new JFileChooser(
					new SingleRootFileSystemView(templDir)) {
				private static final long serialVersionUID = 1L;

				@Override
				public void approveSelection() {
					File outFile = getSelectedFile();
					if (outFile == null) {
						return;
					}
					String name = outFile.getName();
					if (!defProv.canFileSave(name) || !name.endsWith(".xml")) {
						// 書き込み不可ファイル、もしくはxml以外なので許可しない.
						Toolkit tk = Toolkit.getDefaultToolkit();
						tk.beep();
						return;
					}

					// ファイルが存在すれば上書き確認する.
					if (outFile.exists()) {
						Properties strings = LocalizedResourcePropertyLoader
								.getCachedInstance().getLocalizedProperties(
										STRINGS_RESOURCE);

						if (JOptionPane.showConfirmDialog(this,
								strings.getProperty("confirmOverwrite"),
								strings.getProperty("confirm"),
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
							return;
						}
					}

					super.approveSelection();
				}
			};

			// 保存先ファイル名
			fileChooser.setSelectedFile(new File(templDir, defualtName));
			int ret = fileChooser.showSaveDialog(this);
			if (ret != JFileChooser.APPROVE_OPTION) {
				return;
			}

			// テンプレート名
			String localizedName = cd.getName();
			final Properties strings = LocalizedResourcePropertyLoader
					.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);
			localizedName = JOptionPane.showInputDialog(this,
					strings.getProperty("inputTemplateName"), localizedName);
			if (localizedName == null) {
				return;
			}

			File outFile = fileChooser.getSelectedFile();
			defProv.saveTemplate(outFile.getName(), cd, localizedName);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}
	}
}

/**
 * プロファイル一覧リストのモデル
 */
class ProfileSelectorTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private enum Columns {
		NAME("profile.column.name") {
			@Override
			public String getValue(CharacterData cd) {
				return cd.getName();
			}
		},
		ID("profile.column.id") {
			@Override
			public String getValue(CharacterData cd) {
				return cd.getId();
			}
		},
		REVISION("profile.column.revision") {
			@Override
			public String getValue(CharacterData cd) {
				return cd.getRev();
			}
		},
		CANVAS_SIZE("profile.column.canvasSize") {
			@Override
			public String getValue(CharacterData cd) {
				Dimension siz = cd.getImageSize();
				if (siz != null) {
					return siz.width + "x" + siz.height;
				}
				return "";
			}
		},
		DESCRIPTION("profile.column.description") {
			@Override
			public String getValue(CharacterData cd) {
				return cd.getDescription();
			}
		},
		AUTHOR("profile.column.author") {
			@Override
			public String getValue(CharacterData cd) {
				return cd.getAuthor();
			}
		},
		LOCATION("profile.column.location") {
			@Override
			public String getValue(CharacterData cd) {
				return cd.getDocBase().toString();
			}
		};

		private final String columnName;

		private String displayName;

		private int width;

		private Columns(String columnName) {
			this.columnName = columnName;
		}

		public String getDisplayName() {
			loadProperty();
			return displayName;
		}

		public int getWidth() {
			loadProperty();
			return width;
		}

		private void loadProperty() {
			if (displayName != null) {
				return;
			}
			final Properties strings = LocalizedResourcePropertyLoader
					.getCachedInstance().getLocalizedProperties(
							ProfileSelectorDialog.STRINGS_RESOURCE);
			displayName = strings.getProperty(columnName);
			width = Integer
					.parseInt(strings.getProperty(columnName + ".width"));
		}

		public abstract String getValue(CharacterData cd);
	}

	private List<CharacterData> rows = Collections
			.emptyList();

	public void setModel(List<CharacterData> rows) {
		if (rows == null) {
			throw new IllegalArgumentException();
		}
		this.rows = new ArrayList<CharacterData>(rows);
		fireTableDataChanged();
	}

	public void add(CharacterData cd) {
		if (cd == null) {
			throw new IllegalArgumentException();
		}
		this.rows.add(cd);
		int lastRow = this.rows.size() - 1;
		fireTableRowsInserted(lastRow, lastRow);
	}

	public void set(int selRow, CharacterData cd) {
		this.rows.set(selRow, cd);
		fireTableRowsDeleted(selRow, selRow);
	}

	public void remove(int selRow) {
		this.rows.remove(selRow);
		fireTableRowsDeleted(selRow, selRow);
	}

	public List<CharacterData> getModel() {
		return rows;
	}

	public CharacterData getRow(int rowIndex) {
		CharacterData cd = rows.get(rowIndex);
		return cd;
	}

	public int getColumnCount() {
		return Columns.values().length;
	}

	public int getRowCount() {
		return rows.size();
	}

	public void adjustColumnModel(TableColumnModel columnModel) {
		Columns[] columns = Columns.values();
		for (int idx = 0; idx < columns.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(
					columns[idx].getWidth());
		}
	}

	@Override
	public String getColumnName(int column) {
		return Columns.values()[column].getDisplayName();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		CharacterData cd = getRow(rowIndex);
		Columns column = Columns.values()[columnIndex];
		return column.getValue(cd);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		// なにもしない.
	}
}
