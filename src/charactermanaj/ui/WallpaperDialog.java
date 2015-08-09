package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import charactermanaj.Main;
import charactermanaj.graphics.io.ImagePreviewFileChooser;
import charactermanaj.ui.model.PredefinedWallpaper;
import charactermanaj.ui.model.WallpaperInfo;
import charactermanaj.ui.model.WallpaperInfo.WallpaperResourceType;
import charactermanaj.util.LocalizedMessageComboBoxRender;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 壁紙選択ダイアログ
 * @author seraphy
 */
public class WallpaperDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	/**
	 * リソース
	 */
	protected static final String STRINGS_RESOURCE = "languages/wallpaperdialog";

	/**
	 * 定義済み壁紙のキャッシュ
	 */
	private static ArrayList<PredefinedWallpaper> predefinedWallpapers
		= new ArrayList<PredefinedWallpaper>();

	/**
	 * 壁紙情報
	 */
	private WallpaperInfo wallpaperInfo = new WallpaperInfo();
	
	
	/**
	 * 選択なしラジオ
	 */
	private JRadioButton radioNone;
	
	/**
	 * ファイルから選択ラジオ
	 */
	private JRadioButton radioFile;
	
	/**
	 * 定義済みから選択ラジオ
	 */
	private JRadioButton radioPredefined;
	
	/**
	 * 定義済み壁紙リスト
	 */
	private JList listPredefinedWallpapers;
	
	/**
	 * 背景画像の透過率
	 */
	private JSpinner spinnerAlpha;
	
	/**
	 * 選択ファイルフィールド
	 */
	private JTextField txtFile;
	
	/**
	 * ファイルを選択アクション
	 */
	private AbstractAction actChooseFile;
	
	/**
	 * 背景色選択コンポーネント
	 */
	private ColorBox colorBox;

	
	/**
	 * コンストラクタ
	 * @param parent 親フレーム
	 */
	public WallpaperDialog(JFrame parent) {
		super(parent, true);
		try {
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent e) {
					onCancel();
				};
			});
			
			initPredefinedWallpapers();
			initComponent();
			
		} catch (RuntimeException ex) {
			dispose();
			throw ex;
		}
	}
	
	/**
	 * 定義済み壁紙リストを初期化する.<br>
	 * 既に初期化済みであれば何もしない.<br>
	 */
	private static synchronized void initPredefinedWallpapers() {
		if ( !predefinedWallpapers.isEmpty()) {
			return;
		}
		predefinedWallpapers.addAll(PredefinedWallpaper.getPredefinedWallpapers());
	}
	
	/**
	 * このダイアログのコンポーネントを初期化します.<br>
	 */
	private void initComponent() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(WallpaperDialog.STRINGS_RESOURCE);

		setTitle(strings.getProperty("title"));
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout(3, 3));
		getRootPane().setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		JPanel wallpaperPanel = createWallpaperChoosePanel(strings);
		JPanel bgcolorPanel = createBgColorPanel(strings);
		JPanel btnPanel = createButtonPanel(strings);
		
		contentPane.add(bgcolorPanel, BorderLayout.NORTH);
		contentPane.add(wallpaperPanel, BorderLayout.CENTER);
		contentPane.add(btnPanel, BorderLayout.SOUTH);
		
		setSize(400, 350);
		setLocationRelativeTo(getParent());
	}
	
	private JPanel createButtonPanel(Properties strings) {
		JPanel btnPanel = new JPanel();

		AbstractAction actOK = new AbstractAction(strings.getProperty("btn.ok")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		};
		AbstractAction actCancel = new AbstractAction(strings.getProperty("btn.cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		};
		
		JButton btnOK = new JButton(actOK);
		JButton btnCancel = new JButton(actCancel);

		BoxLayout bl = new BoxLayout(btnPanel, BoxLayout.LINE_AXIS);
		btnPanel.setLayout(bl);
		btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 45));
		
		btnPanel.add(Box.createHorizontalGlue());
		if (Main.isLinuxOrMacOSX()) {
			btnPanel.add(btnCancel);
			btnPanel.add(btnOK);

		} else {
			btnPanel.add(btnOK);
			btnPanel.add(btnCancel);
		}

		// Enter/Returnキーを既定ボタンにする.
		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(btnOK);
		
		// CTRL-Wでウィンドウを非表示にする. (ESCでは非表示にしない)
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeWallpaperDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeWallpaperDialog");
		am.put("closeWallpaperDialog", actCancel);

		return btnPanel;
	}
	
	private JPanel createBgColorPanel(Properties strings) {
		JPanel bgcolorPanel = new JPanel();

		bgcolorPanel.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(strings.getProperty("group.bgcolor")),
						BorderFactory.createEmptyBorder(3, 3, 3, 3)));

		GridBagLayout gbl = new GridBagLayout();
		bgcolorPanel.setLayout(gbl);
		
		colorBox = new ColorBox();
		colorBox.getColorDisplayPanel().setPreferredSize(new Dimension(48, 20));

		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.weighty = 0.;
		gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		
		bgcolorPanel.add(colorBox, gbc);

		return bgcolorPanel;
	}

	private JPanel createWallpaperChoosePanel(Properties strings) {
		JPanel wallpaperPanel = new JPanel();
		
		wallpaperPanel.setBorder(BorderFactory.createTitledBorder(strings.getProperty("group.wallpaper")));

		GridBagLayout gbl = new GridBagLayout();
		wallpaperPanel.setLayout(gbl);
		
		radioNone = new JRadioButton(strings.getProperty("radio.none"));
		radioFile = new JRadioButton(strings.getProperty("radio.file"));
		radioPredefined = new JRadioButton(strings.getProperty("radio.predefined"));
		
		ButtonGroup btnGroup = new ButtonGroup();
		btnGroup.add(radioNone);
		btnGroup.add(radioFile);
		btnGroup.add(radioPredefined);
		
		radioNone.setSelected(true);
		
		txtFile = new JTextField();
		
		actChooseFile = new AbstractAction(strings.getProperty("btn.chooseFile")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onChooseFile();
			}
		};
		JButton btnChooseFile = new JButton(actChooseFile);

		listPredefinedWallpapers = new JList(predefinedWallpapers
				.toArray(new PredefinedWallpaper[predefinedWallpapers.size()]));
		listPredefinedWallpapers.setCellRenderer(new LocalizedMessageComboBoxRender(strings));
		
		SpinnerNumberModel alphaSpModel = new SpinnerNumberModel(100, 0, 100, 1);
		spinnerAlpha = new JSpinner(alphaSpModel);
		
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.weighty = 0.;
		gbc.gridheight = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.weightx = 0.;

		wallpaperPanel.add(radioNone, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weightx = 0.;

		wallpaperPanel.add(radioFile, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		
		wallpaperPanel.add(Box.createHorizontalStrut(20), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		
		wallpaperPanel.add(txtFile, gbc);

		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		
		wallpaperPanel.add(btnChooseFile, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.weightx = 0.;

		wallpaperPanel.add(radioPredefined, gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		
		wallpaperPanel.add(new JScrollPane(listPredefinedWallpapers), gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 3;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.insets = new Insets(5, 0, 0, 0);
		
		wallpaperPanel.add(new JLabel(
				strings.getProperty("label.wallpaperImageAlpha")), gbc);
		
		
		JPanel alphaPanel = new JPanel(new BorderLayout(3, 3));
		alphaPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		alphaPanel.add(spinnerAlpha, BorderLayout.CENTER);
		alphaPanel.add(new JLabel("%"), BorderLayout.EAST);
		
		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.gridwidth = 2;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.ipady = 0;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;

		wallpaperPanel.add(alphaPanel, gbc);
		
		// リスナ
		listPredefinedWallpapers.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				radioPredefined.setSelected(true);
			}
		});

		return wallpaperPanel;
	}

	protected void onChooseFile() {
		File selectedFile = wallpaperInfo.getFile();
		
		final JFileChooser fileChooser = new ImagePreviewFileChooser();
		fileChooser.setSelectedFile(selectedFile);

		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		selectedFile = fileChooser.getSelectedFile();
		txtFile.setText(selectedFile == null ? "" : selectedFile.getPath());
		radioFile.setSelected(selectedFile != null);
	}
	
	/**
	 * 壁紙情報インスタンスの内容でコンポーネントを設定する.
	 */
	protected void applyByWallpaperInfo() {
		
		// ファイル
		File imageFile = wallpaperInfo.getFile();
		txtFile.setText(imageFile == null ? "" : imageFile.getPath());
		
		// リソース
		PredefinedWallpaper selectedPredefinedWp = null;
		String resource = wallpaperInfo.getResource();
		for (PredefinedWallpaper predefinedWp : predefinedWallpapers) {
			if (predefinedWp.getResource().equals(resource)) {
				selectedPredefinedWp = predefinedWp;
			}
		}
		listPredefinedWallpapers.setSelectedValue(selectedPredefinedWp, true);
		
		// 透過率
		int alphaInt = (int)(wallpaperInfo.getAlpha() * 100);
		spinnerAlpha.setValue(alphaInt);
		
		// 背景色
		Color bgColor = wallpaperInfo.getBackgroundColor();
		colorBox.setColorKey(bgColor);
		
		// タイプ
		WallpaperResourceType typ = wallpaperInfo.getType();
		if (typ == WallpaperResourceType.FILE) {
			radioFile.setSelected(true);
		
		} else if (typ == WallpaperResourceType.PREDEFINED) {
			radioPredefined.setSelected(true);

		} else {
			radioNone.setSelected(true);
		}
	}

	/**
	 * ダイアログのコンポーネントの状態から壁紙情報を構築して返す.<br>
	 * 生成した壁紙情報が妥当であるかは検証しない.<br>
	 * @return 新しい壁紙情報インスタンス
	 */
	public WallpaperInfo createWallpaperInfo() {
		WallpaperInfo wallpaperInfo = this.wallpaperInfo.clone();

		// 選択したタイプ
		WallpaperResourceType typ;
		if (radioFile.isSelected()) {
			// 背景画像ファイル選択
			typ = WallpaperResourceType.FILE;

		} else if (radioPredefined.isSelected()) {
			// リソース選択
			typ = WallpaperResourceType.PREDEFINED;
		
		} else {
			// それ以外は選択なし
			typ = WallpaperResourceType.NONE;
		}

		// 画像ファイルの現在の選択
		String strSelectedFile = txtFile.getText();
		File selectedFile = null;
		if (strSelectedFile != null) {
			strSelectedFile = strSelectedFile.trim();
			if (strSelectedFile.length() > 0) {
				selectedFile = new File(strSelectedFile);
			}
		}
		wallpaperInfo.setFile(selectedFile);
		
		// 定義済みリソースの現在の選択
		PredefinedWallpaper predefinedWp =
			(PredefinedWallpaper) listPredefinedWallpapers.getSelectedValue();
		wallpaperInfo.setResource(predefinedWp == null ?
				null : predefinedWp.getResource());
		
		// タイプの設定
		wallpaperInfo.setType(typ);
		
		// 背景画像の透過率
		int alphaInt = (Integer) spinnerAlpha.getValue();
		float alpha = (float) alphaInt / 100.f;
		wallpaperInfo.setAlpha(alpha);
		
		// 背景色
		Color bgColor = colorBox.getColorKey();
		wallpaperInfo.setBackgroundColor(bgColor);
		
		return wallpaperInfo;
	}
	
	protected void onOK() {
		WallpaperInfo wallpaperInfo = createWallpaperInfo();
		
		if (!checkValidate(wallpaperInfo)) {
			return;
		}

		this.wallpaperInfo = wallpaperInfo;
		dispose();
	}
	
	protected void onCancel() {
		wallpaperInfo = null;
		dispose();
	}
	
	protected boolean checkValidate(WallpaperInfo wallpaperInfo) {
		String messageid = null;
		WallpaperResourceType typ = wallpaperInfo.getType();
		if (typ == WallpaperResourceType.FILE) {
			File selectedFile = wallpaperInfo.getFile();
			if (selectedFile == null) {
				messageid = "error.require.imageFile";

			} else if (!selectedFile.exists() || !selectedFile.isFile()
					|| !selectedFile.canRead()) {
				messageid = "error.invalid.imageFile";
			}
		} else if (typ == WallpaperResourceType.PREDEFINED) {
			String resource = wallpaperInfo.getResource();
			if (resource == null || resource.trim().length() == 0) {
				messageid = "error.require.resource";
			}
		}
		
		if (messageid != null) {
			Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(WallpaperDialog.STRINGS_RESOURCE);

			String message = strings.getProperty(messageid);
			JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
		}
		
		return messageid == null;
	}
	
	public void setWallpaperInfo(WallpaperInfo wallpaperInfo) {
		if (wallpaperInfo == null) {
			wallpaperInfo = new WallpaperInfo();
		}
		this.wallpaperInfo = wallpaperInfo;
	}
	
	public WallpaperInfo getWallpaperInfo() {
		return wallpaperInfo;
	}
	
	/**
	 * ダイアログを表示し、その結果を返す.<br>
	 * キャンセルされた場合はnullが返される.<br>
	 * @return 結果、もしくはnull
	 */
	public WallpaperInfo showDialog() {
		applyByWallpaperInfo();
		setVisible(true);
		return wallpaperInfo;
	}
}
