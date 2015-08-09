package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Properties;

import javax.swing.AbstractAction;
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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import charactermanaj.Main;
import charactermanaj.graphics.io.UkagakaImageConverter;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 伺か用PNG/PNA出力設定ダイアログ
 * @author seraphy
 */
public class UkagakaConvertDialog extends JDialog {

	private static final long serialVersionUID = 4189631881766588004L;

	/**
	 * リソース
	 */
	protected static final String STRINGS_RESOURCE = "languages/ukagakaConvertDialog";

	/**
	 * キャプション
	 */
	private TextField caption = new TextField();
	
	/**
	 * キャンセル
	 */
	private AbstractAction actCancel;
	
	/**
	 * 保存(デフォルトアクション)
	 */
	private AbstractAction actSave;
	

	/**
	 * プレビュー(PNG)
	 */
	private SamplePicturePanel opaqueImagePanel = new SamplePicturePanel();
	
	/**
	 * プレビュー(PNA)
	 */
	private SamplePicturePanel alphaImagePanel = new SamplePicturePanel();

	/**
	 * 透過色キー表示ボックス
	 */
	private ColorBox colorBox = new ColorBox();
	
	/**
	 * エクスポート対象の元イメージ.
	 */
	private BufferedImage originalImage;
	
	/**
	 * 透過色をマニュアルとするか?
	 */
	private boolean manualTransparentColorKey;
	
	/**
	 * 透過キー自動モード
	 */
	private JRadioButton radioAuto;

	/**
	 * 透過キー手動選択モード
	 */
	private JRadioButton radioManual;
	
	/**
	 * 上書きモードチェックボックス
	 */
	private JCheckBox chkOverwriteOption;
	
	/**
	 * 終了コード
	 */
	private Object result;
	
	/**
	 * 保存ボタンアクションリスナ.<br>
	 */
	private ActionListener saveActionListener;
	
	/**
	 * 上書きオプションの表示フラグ
	 */
	private boolean showOverwriteOption;
	

	/**
	 * 伺か用PNG/PNA出力設定ダイアログを構築する.
	 * @param parent 親フレーム
	 */
	public UkagakaConvertDialog(JFrame parent) {
		this(parent, false);
	}
	
	/**
	 * 伺か用PNG/PNA出力設定ダイアログを構築する.
	 * @param parent 親フレーム
	 * @param overwriteOption 上書きオプションの表示 
	 */
	public UkagakaConvertDialog(JFrame parent, boolean overwriteOption) {
		this(parent, null, overwriteOption);
	}

	/**
	 * 伺か用PNG/PNA出力設定ダイアログを構築する.
	 * @param parent 親フレーム
	 * @param saveActionListener 保存ボタンアクション
	 * @param overwriteOption 上書きオプションの表示 
	 */
	public UkagakaConvertDialog(JFrame parent, ActionListener saveActionListener, boolean overwriteOption) {
		super(parent, true);
		this.saveActionListener = saveActionListener;
		this.showOverwriteOption = overwriteOption;

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
			@Override
			public void windowOpened(WindowEvent e) {
				result = null;
			}
		});
		
		try {
			initComponent();

		} catch (RuntimeException ex) {
			dispose();
			throw ex;
		}
	}

	/**
	 * コンポーネントの初期化
	 */
	private void initComponent() {
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(STRINGS_RESOURCE);

		setTitle(strings.getProperty("title"));

		Toolkit tk = Toolkit.getDefaultToolkit();

		actCancel = new AbstractAction(strings.getProperty("btn.cancel")) {
			private static final long serialVersionUID = -1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};
		actSave = new AbstractAction(strings.getProperty("btn.save")) {
			private static final long serialVersionUID = -1L;
			public void actionPerformed(ActionEvent e) {
				onSave();
			}
		};
		
		JButton btnCancel = new JButton(actCancel);
		JButton btnSave = new JButton(actSave);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout(3, 3));

		contentPane.add(caption, BorderLayout.NORTH);
		caption.setEditable(false);
		caption.setVisible(false);
		
		JScrollPane opaqueSp = new JScrollPane(opaqueImagePanel);
		JScrollPane alphaSp = new JScrollPane(alphaImagePanel);

		JPanel previewSpPane = new JPanel();
		BoxLayout boxlayout = new BoxLayout(previewSpPane, BoxLayout.LINE_AXIS);
		previewSpPane.setLayout(boxlayout);
		
		previewSpPane.add(opaqueSp);
		previewSpPane.add(Box.createHorizontalStrut(3));
		previewSpPane.add(alphaSp);
		
		previewSpPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(strings.getProperty("preview")),
				BorderFactory.createEmptyBorder(3, 3, 3, 3)
				));

		JPanel centerPane = new JPanel(new BorderLayout());
		centerPane.add(previewSpPane, BorderLayout.CENTER);

		JPanel transparentColorPanel = new JPanel();
		GridBagLayout tc_gbl = new GridBagLayout();
		transparentColorPanel.setLayout(tc_gbl);
		transparentColorPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(strings.getProperty("caption.chooseTransparentColorKey")),
				BorderFactory.createEmptyBorder(3, 3, 3, 3)
				));
		centerPane.add(transparentColorPanel, BorderLayout.SOUTH);
		
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		radioAuto = new JRadioButton(strings.getProperty("radio.auto"));
		radioAuto.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				onChange(!radioAuto.isSelected());
			}
		});
		radioManual = new JRadioButton(strings.getProperty("radio.manual"));
		radioAuto.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				onChange(!radioAuto.isSelected());
			}
		});
		
		ButtonGroup btngroup = new ButtonGroup();
		btngroup.add(radioAuto);
		btngroup.add(radioManual);
		
		radioAuto.setSelected(!manualTransparentColorKey);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		transparentColorPanel.add(radioAuto, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		transparentColorPanel.add(radioManual, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		transparentColorPanel.add(colorBox, gbc);

		
		contentPane.add(centerPane, BorderLayout.CENTER);
		
		
		JPanel btnPanel = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		btnPanel.setLayout(gbl);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		
		chkOverwriteOption = new JCheckBox(strings.getProperty("chk.overwriteOriginalFile"));
		
		if (showOverwriteOption) {
			btnPanel.add(chkOverwriteOption, gbc);

		} else {
			btnPanel.add(Box.createHorizontalGlue(), gbc);
		}
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 2 : 1;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		btnPanel.add(btnSave, gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 1 : 2;
		gbc.gridy = 0;
		btnPanel.add(btnCancel, gbc);
		
		btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 45));
		
		contentPane.add(btnPanel, BorderLayout.SOUTH);
	
		JRootPane rootPane = getRootPane();
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeExportWizDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeExportWizDialog");
		 
		am.put("closeExportWizDialog", actCancel);
		
		rootPane.setDefaultButton(btnSave);
		
		rootPane.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

		setSize(450, 450);
		setLocationRelativeTo(getParent());
		
		// colorBoxの色変更イベントのハンドル
		colorBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onChooseTransparentColorKey();
			}
		});
	}
	
	/**
	 * 出力するイメージを設定する.
	 * @param img イメージ(TYPE_INT_ARGBのみ)
	 * @param colorKey 透過色に指定するカラーキー(候補)、nullの場合はデフォルト
	 */
	public void setExportImage(BufferedImage img, Color colorKey) {
		if (img == null) {
			throw new IllegalArgumentException();
		}
		if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new IllegalArgumentException("TYPE_INT_ARGB以外は指定できません。");
		}
		
		if (colorKey == null) {
			colorKey = Color.GREEN;
		}

		this.originalImage = img;
		colorBox.setColorKey(colorKey);

		rebuildImage();
	}
	
	public BufferedImage getOpaqueImage() {
		return opaqueImagePanel.getSamplePictrue();
	}
	
	public BufferedImage getAlphaImage() {
		return alphaImagePanel.getSamplePictrue();
	}
	
	public void setAutoTransparentColor(boolean mode) {
		if (mode) {
			radioAuto.setSelected(true);
			radioManual.setSelected(false);

		} else {
			radioManual.setSelected(true);
			radioAuto.setSelected(false);
		}
	}
	
	public Color getTransparentColorKey() {
		return colorBox.getColorKey();
	}
	
	public void setTransparentColorKey(Color colorKey) {
		colorBox.setColorKey(colorKey);
	}
	
	public boolean isAutoTransparentColor() {
		return radioAuto.isSelected();
	}
	
	public boolean isOverwriteOriginalFile() {
		return chkOverwriteOption.isSelected();
	}
	
	public void setOverwriteOriginalFile(boolean overwriteOriginalFile) {
		chkOverwriteOption.setSelected(overwriteOriginalFile);
	}
	
	protected void onClose() {
		result = null;
		dispose();
	}
	
	protected void onSave() {
		if (saveActionListener != null) {
			ActionEvent e = new ActionEvent(this, 0, "save");
			saveActionListener.actionPerformed(e);
		}
	}
	
	public void setSaveActionListener(ActionListener saveActionListener) {
		this.saveActionListener = saveActionListener;
	}
	
	public ActionListener getSaveActionListener() {
		return saveActionListener;
	}
	
	public Object getResult() {
		return result;
	}
	
	public void setResult(Object result) {
		this.result = result; 
	}
	
	public void setCaption(String text) {
		if (text == null || text.length() == 0) {
			caption.setText("");
			caption.setVisible(false);

		} else {
			caption.setText(text);
			caption.setVisible(true);
		}
	}
	
	public String getCaption() {
		return caption.getText();
	}
	
	/**
	 * 透過色のマニュアル選択.<br>
	 */
	protected void onChooseTransparentColorKey() {
		// モードを手動に切り替え
		setAutoTransparentColor(false);
		// プレビューを再構築
		rebuildImage();
	}

	/**
	 * 伺か用PNGの透過色キーの自動・マニュアルの切り替えイベント.<br>
	 * @param modeManual
	 */
	protected void onChange(boolean modeManual) {
		if (manualTransparentColorKey != modeManual) {
			manualTransparentColorKey = modeManual;
			rebuildImage();
		}
	}
	
	/**
	 * 伺か用のPNG/PNA画像を生成してプレビューに設定します.<br>
	 */
	protected void rebuildImage() {
		if (originalImage == null) {
			return;
		}
		UkagakaImageConverter conv = UkagakaImageConverter.getInstance();

		BufferedImage pna = conv.createUkagakaPNA(originalImage);
		
		Color transparentColorKey = null;
		if (manualTransparentColorKey) {
			transparentColorKey = colorBox.getColorKey();
		} else {
			transparentColorKey = conv.detectTransparentColorKey(originalImage);
		}
		
		BufferedImage png = conv.createUkagakaPNG(originalImage, transparentColorKey);
		
		opaqueImagePanel.setSamplePicture(png);
		alphaImagePanel.setSamplePicture(pna);
	}
}

