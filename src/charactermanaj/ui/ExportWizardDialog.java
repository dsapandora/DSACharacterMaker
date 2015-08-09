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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import charactermanaj.Main;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.PartsSpecResolver;
import charactermanaj.model.io.CharacterDataFileReaderWriterFactory;
import charactermanaj.model.io.CharacterDataWriter;
import charactermanaj.model.io.ExportInfoKeys;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.ui.progress.ProgressHandle;
import charactermanaj.ui.progress.Worker;
import charactermanaj.ui.progress.WorkerException;
import charactermanaj.ui.progress.WorkerWithProgessDialog;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;

public class ExportWizardDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	protected static final String STRINGS_RESOURCE = "languages/exportwizdialog";
	
	protected static ArchiveFileDialog archiveFileDialog = new ArchiveFileDialog();
	
	private JPanel activePanel;
	
	private AbstractAction actNext;
	
	private AbstractAction actPrev;

	private AbstractAction actFinish;
	
	private ExportInformationPanel basicPanel;
	
	private ExportPartsSelectPanel partsSelectPanel;
	
	private ExportPresetSelectPanel presetSelectPanel;

	private CharacterData source;
	
	public static File getLastUsedDir() {
		return archiveFileDialog.getLastUSedDir();
	}
	
	public static void setLastUsedDir(File lastUsedDir) {
		archiveFileDialog.setLastUSedDir(lastUsedDir);
	}
	
	public ExportWizardDialog(JFrame parent, CharacterData characterData, BufferedImage samplePicture) {
		super(parent, true);
		initComponent(parent, characterData, samplePicture);
	}
	
	public ExportWizardDialog(JDialog parent, CharacterData characterData, BufferedImage samplePicture) {
		super(parent, true);
		initComponent(parent, characterData, samplePicture);
	}

	private void initComponent(Component parent, CharacterData characterData, BufferedImage samplePicture) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		this.source = characterData;

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
		setTitle(strings.getProperty("title"));
		
		// メインパネル
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		final JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEtchedBorder());
		final CardLayout mainPanelLayout = new CardLayout(5, 5);
		mainPanel.setLayout(mainPanelLayout);
		contentPane.add(mainPanel, BorderLayout.CENTER);
		
		ComponentListener componentListener = new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				onComponentShown((JPanel) e.getComponent());
			}
		};

		
		// アクション
		
		this.actNext = new AbstractAction(strings.getProperty("btn.next")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				mainPanelLayout.next(mainPanel);
			}
		};
		this.actPrev = new AbstractAction(strings.getProperty("btn.prev")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				mainPanelLayout.previous(mainPanel);
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

		ChangeListener actChangeValue = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateBtnPanelState();
			}
		};
		
		ChangeListener actPanelEnabler = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updatePanelStatus();
			}
		};

		// panel1 : basic
		this.basicPanel = new ExportInformationPanel(characterData, samplePicture);
		this.basicPanel.addComponentListener(componentListener);
		this.basicPanel.addChangeListener(actChangeValue);
		this.basicPanel.addChangeListener(actPanelEnabler);
		mainPanel.add(this.basicPanel, "basicPanel");
		
		// panel2 : panelSelectPanel
		this.partsSelectPanel = new ExportPartsSelectPanel(characterData);
		this.partsSelectPanel.addComponentListener(componentListener);
		this.partsSelectPanel.addChangeListener(actChangeValue);
		mainPanel.add(this.partsSelectPanel, "partsSelectPanel");
		
		// panel3 : presetSelectPanel
		this.presetSelectPanel = new ExportPresetSelectPanel(
				this.partsSelectPanel,
				this.basicPanel,
				characterData.getPartsSets().values(),
				characterData.getDefaultPartsSetId());
		this.presetSelectPanel.addComponentListener(componentListener);
		this.presetSelectPanel.addChangeListener(actChangeValue);
		mainPanel.add(this.presetSelectPanel, "presetSelectPanel");
		
		
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
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeExportWizDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeExportWizDialog");
		am.put("closeExportWizDialog", actCancel);

		// 表示
		
		setSize(500, 500);
		setLocationRelativeTo(parent);
		
		mainPanelLayout.first(mainPanel);
		updateBtnPanelState();
		updatePanelStatus();
	}
	
	protected void onComponentShown(JPanel panel) {
		activePanel = panel;
		updateBtnPanelState();
	}
	
	protected void updatePanelStatus() {
		partsSelectPanel.setEnabled(basicPanel.isExportPartsImages());
		presetSelectPanel.setEnabled(basicPanel.isExportPresets());
	}
	
	protected void updateBtnPanelState() {
		actPrev.setEnabled(activePanel != null && activePanel != basicPanel);
		actNext.setEnabled(activePanel != null && activePanel != presetSelectPanel);
		actFinish.setEnabled(isComplete());
	}
	
	protected void checkMissingParts(Collection<PartsSet> partaSets) {
		if (partaSets == null) {
			partaSets = presetSelectPanel.getSelectedPresets();
		}
		partsSelectPanel.checkMissingPartsList(partaSets);
	}
	
	protected boolean isComplete() {
		
		if (basicPanel.isExportPartsImages()) {
			if (partsSelectPanel.getSelectedCount() == 0) {
				// パーツイメージのエクスポートを指定した場合、エクスポートするパーツの選択は必須
				return false;
			}
		}
		if (basicPanel.isExportPresets()) {
			if (presetSelectPanel.getSelectedCount() == 0) {
				// プリセットのエクスポートを指定した場合、エクスポートするプリセットの選択は必須
				return false;
			}
		}
		
		return true;
	}
	
	protected void onClose() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);

		if (JOptionPane.showConfirmDialog(this,
				strings.getProperty("confirm.close"),
				strings.getProperty("confirm"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
			return;
		}
		
		dispose();
	}
	
	protected void onFinish() {
		if (!isComplete()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		try {
			final File outFile = archiveFileDialog.showSaveDialog(this);
			if (outFile == null) {
				return;
			}

			// 出力
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				Worker<Object> worker = new Worker<Object>() {
					public Object doWork(ProgressHandle progressHandle) throws IOException {
						doExport(outFile);
						return null;
					}
				};
				
				WorkerWithProgessDialog<Object> dlg
					= new WorkerWithProgessDialog<Object>(this, worker);
				dlg.startAndWait();

			} finally {
				setCursor(Cursor.getDefaultCursor());
			}

			// 完了メッセージ
			Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
					.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);
			JOptionPane.showMessageDialog(this, strings.getProperty("complete"));
			
			// 完了後、ウィンドウを閉じる.
			dispose();

		} catch (WorkerException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex.getCause());
			
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void doExport(File outFile) throws IOException {
		CharacterDataFileReaderWriterFactory writerFactory = CharacterDataFileReaderWriterFactory.getInstance();
		CharacterDataWriter exportWriter = writerFactory.createWriter(outFile);
		try {
			// 基本情報のみをコピーし、これをエクスポートするキャラクター定義のベースとする。
			// (プリセットとパーツイメージはリセットされている状態。)
			CharacterData cd = source.duplicateBasicInfo();
			cd.clearPartsSets(false);
			
			boolean exportPresets = basicPanel.isExportPresets();
			boolean exportSamplePicture = basicPanel.isExportSamplePicture();
			boolean exportCharacterData = true;
			boolean exportPartsImages = basicPanel.isExportPartsImages();

			// 基本情報を設定する.
			cd.setAuthor(basicPanel.getAuthor());
			cd.setDescription(basicPanel.getDescription());
			
			// エクスポート情報を出力する.
			Properties exportProp = new Properties();
			exportProp.setProperty(ExportInfoKeys.EXPORT_PRESETS, Boolean.toString(exportPresets));
			exportProp.setProperty(ExportInfoKeys.EXPORT_SAMPLE_PICTURE, Boolean.toString(exportSamplePicture));
			exportProp.setProperty(ExportInfoKeys.EXPORT_CHARACTER_DATA, Boolean.toString(exportCharacterData));
			exportProp.setProperty(ExportInfoKeys.EXPORT_PARTS_IMAGES, Boolean.toString(exportPartsImages));
			exportProp.setProperty(ExportInfoKeys.EXPORT_TIMESTAMP, Long.toString(System.currentTimeMillis()));

			exportWriter.writeExportProp(exportProp);
			
			// プリセットをエクスポートする場合、プリセット情報を登録する.
			if (exportPresets) {
				HashSet<String> registered = new HashSet<String>();
				for (PartsSet partsSet : presetSelectPanel.getSelectedPresets()) {
					registered.add(partsSet.getPartsSetId());
					cd.addPartsSet(partsSet);
				}
				// プリセットとして登録済みのもののみ既定に設定可能
				String defaultPresetId = presetSelectPanel.getDefaultPresetId();
				if (registered.contains(defaultPresetId)) {
					cd.setDefaultPartsSetId(defaultPresetId);
				}
			}
			
			// キャラクターデータを出力する.
			exportWriter.writeCharacterData(cd);
			
			// readme.txtを出力する.
			String readmeContents = cd.getDescription();
			if (readmeContents != null && readmeContents.trim().length() > 0) {
				AppConfig appConfig = AppConfig.getInstance();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				pw.println("exported by CharacterManaJ (version "
						+ appConfig.getSpecificationVersion() + " "
						+ appConfig.getImplementationVersion() + ")");
				pw.println();
				pw.println(readmeContents);
				pw.close();
				exportWriter.writeTextUTF16LE("readme.txt", sw.toString());
			}

			// サンプルピクチャをエクスポートする
			if (exportSamplePicture) {
				BufferedImage pic = null;
				pic = basicPanel.getSamplePicture();
				if (pic != null) {
					exportWriter.writeSamplePicture(pic);
				}
			}

			if (exportPartsImages) {
				Map<PartsIdentifier, PartsSpec> partsSpecMap = partsSelectPanel.getSelectedParts();
				
				// パーツ管理情報を出力する
				exportWriter.writePartsManageData(partsSpecMap);
				
				// パーツイメージを出力する
				exportWriter.writePartsImages(partsSpecMap);
			}

		} finally {
			exportWriter.close();
		}
	}
}

interface ExportResolverBase {
	
	void addChangeListener(ChangeListener l);
	
	void removeChangeListener(ChangeListener l);
	
}



/**
 * 基本情報
 * @author seraphy
 */
interface ExportInformationResolver extends ExportResolverBase {
	
	BufferedImage getSamplePicture();
	
	boolean isExportSamplePicture();
	
	boolean isExportPartsImages();
	
	boolean isExportPresets();
	
	String getAuthor();
	
	String getDescription();
}

abstract class AbstractImportPanel extends JPanel implements ExportResolverBase {
	
	private static final long serialVersionUID = 1L;

	protected LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();

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
	
	protected void fireChangeEvent() {
		ChangeEvent e = new ChangeEvent(this);
		for (ChangeListener listener : listeners) {
			listener.stateChanged(e);
		}
	}
}

/**
 * 基本情報パネル
 * @author seraphy
 */
class ExportInformationPanel extends AbstractImportPanel implements ExportInformationResolver {

	private static final long serialVersionUID = 1L;

	private BufferedImage samplePicture;
	
	private SamplePicturePanel sampleImagePanel;
	
	private JTextField txtAuthor;
	
	private JTextArea txtDescription;
	
	private JCheckBox chkPartsImages;
	
	private JCheckBox chkPresets;
	
	private JCheckBox chkSampleImage;
	

	
	protected ExportInformationPanel(final CharacterData characterData, final BufferedImage samplePicture) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		setName("basicPanel");
		this.samplePicture = samplePicture;

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);

		GridBagLayout basicPanelLayout = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(basicPanelLayout);
		
		JPanel contentsSpecPanel = new JPanel();
		contentsSpecPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(strings.getProperty("basic.contentsSpec"))));
		BoxLayout contentsSpecPanelLayout = new BoxLayout(contentsSpecPanel, BoxLayout.PAGE_AXIS);
		contentsSpecPanel.setLayout(contentsSpecPanelLayout);
		
		JCheckBox chkCharacterDef = new JCheckBox(strings.getProperty("characterdef"));
		chkPartsImages = new JCheckBox(strings.getProperty("basic.chk.partsImages")); 
		chkPresets = new JCheckBox(strings.getProperty("basic.chk.presets"));
		chkSampleImage = new JCheckBox(strings.getProperty("basic.chk.samplePicture"));

		chkCharacterDef.setEnabled(false); // キャラクター定義は固定
		chkCharacterDef.setSelected(true);

		contentsSpecPanel.add(chkCharacterDef);
		contentsSpecPanel.add(chkPartsImages);
		contentsSpecPanel.add(chkPresets);
		contentsSpecPanel.add(chkSampleImage);

		///
		
		JPanel commentPanel = new JPanel(); 
		Dimension archiveInfoPanelMinSize = new Dimension(300, 200);
		commentPanel.setMinimumSize(archiveInfoPanelMinSize);
		commentPanel.setPreferredSize(archiveInfoPanelMinSize);
		commentPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(strings.getProperty("basic.comment"))));
		GridBagLayout commentPanelLayout = new GridBagLayout();
		commentPanel.setLayout(commentPanelLayout);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		commentPanel.add(new JLabel(strings.getProperty("author"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		txtAuthor = new JTextField();
		commentPanel.add(txtAuthor, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		commentPanel.add(new JLabel(strings.getProperty("description"), JLabel.RIGHT), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		gbc.weighty = 1.;
		gbc.weightx = 1.;
		txtDescription = new JTextArea();
		commentPanel.add(new JScrollPane(txtDescription), gbc);

		///
		
		sampleImagePanel = new SamplePicturePanel(samplePicture);
		sampleImagePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(strings.getProperty("basic.sampleImage"))));
		
		
		///
		
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
		add(commentPanel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		add(sampleImagePanel, gbc);
		
		loadBasicInfo(characterData);

		// アクションリスナ
		
		ActionListener modListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateSamplePicture();
				fireChangeEvent();
			}
		};
		chkPartsImages.addActionListener(modListener);
		chkPresets.addActionListener(modListener);
		chkSampleImage.addActionListener(modListener);
	}
	
	protected void updateSamplePicture() {
		sampleImagePanel.setVisiblePicture(chkSampleImage.isSelected());
	}
	
	protected void loadBasicInfo(CharacterData characterData) {
		if (samplePicture == null) {
			// サンプルイメージがなければディセーブル
			chkSampleImage.setEnabled(false);
			chkSampleImage.setSelected(false);
			sampleImagePanel.setVisiblePicture(false);
		} else {
			chkSampleImage.setSelected(true);
			sampleImagePanel.setVisiblePicture(true);
		}
		chkPartsImages.setSelected(true);
		chkPresets.setSelected(true);

		String author = characterData.getAuthor();
		String description = characterData.getDescription();
		txtAuthor.setText(author == null ? "" : author);
		txtDescription.setText(description == null ? "" : description);
	}

	public BufferedImage getSamplePicture() {
		return samplePicture;
	}
	
	public boolean isExportSamplePicture() {
		return chkSampleImage.isSelected();
	}
	
	public boolean isExportPartsImages() {
		return chkPartsImages.isSelected();
	}
	
	public boolean isExportPresets() {
		return chkPresets.isSelected();
	}
	
	public String getAuthor() {
		return txtAuthor.getText();
	}
	
	public String getDescription() {
		return txtDescription.getText();
	}
}

/**
 * エクスポート対象パーツ
 * @author seraphy
 */
interface ExportPartsResolver extends ExportResolverBase {

	int getSelectedCount();
	
	void selectByPartsSet(Collection<PartsSet> partsSet);
	
	Map<PartsIdentifier, PartsSpec> getSelectedParts();
	
	Map<PartsSet, List<PartsIdentifier>> checkMissingPartsList(Collection<PartsSet> partsSets);

}

/**
 * エクスポート対象パーツ選択パネル
 * @author seraphy
 */
class ExportPartsSelectPanel extends AbstractImportPanel implements ExportPartsResolver {

	private static final long serialVersionUID = 1L;

	private ExportPartsTableModel partsTableModel;
	
	private JTable partsTable;
	
	private Action actSelectAll;
	
	private Action actDeselectAll;

	private Action actSort;

	private Action actSortByTimestamp;

	protected ExportPartsSelectPanel(PartsSpecResolver partsSpecResolver) {
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException();
		}

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);

		setName("choosePartsPanel");
		setBorder(BorderFactory.createTitledBorder(strings.getProperty("parts.title")));

		setLayout(new BorderLayout());
		
		partsTableModel = new ExportPartsTableModel();
		
		partsTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				fireChangeEvent();
			}
		});

		loadPartsInfo(partsSpecResolver);
		
		AppConfig appConfig = AppConfig.getInstance();
		
		final Color disabledForeground = appConfig.getDisabledCellForgroundColor();
		
		partsTable = new JTable(partsTableModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				if (comp instanceof JCheckBox) {
					// BooleanのデフォルトのレンダラーはKCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}
				comp.setForeground(isEnabled() ?
						(isCellSelected(row,column) ? getSelectionForeground() : getForeground())
						: disabledForeground);
				return comp;
			}
		};
		partsTable.setShowGrid(true);
		partsTable.setGridColor(appConfig.getGridColor());
		partsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

		add(new JScrollPane(partsTable), BorderLayout.CENTER);

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
		actSort = new AbstractAction(strings.getProperty("parts.btn.sort")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSort();
			}
		};
		actSortByTimestamp = new AbstractAction(strings.getProperty("parts.btn.sortByTimestamp")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSortByTimestamp();
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
		JButton btnSortByTimestamp = new JButton(actSortByTimestamp);
		btnPanel.add(btnSortByTimestamp, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);

		add(btnPanel, BorderLayout.SOUTH);
	}
	
	protected void loadPartsInfo(PartsSpecResolver partsSpecResolver) {
		partsTableModel.clear();
		for (PartsCategory partsCategory : partsSpecResolver.getPartsCategories()) {
			Map<PartsIdentifier, PartsSpec> partsSpecMap = partsSpecResolver.getPartsSpecMap(partsCategory);
			for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsSpecMap.entrySet()) {
				PartsIdentifier partsIdentifier = entry.getKey();
				PartsSpec partsSpec = entry.getValue();
				ExportPartsSelectModel model = new ExportPartsSelectModel(partsIdentifier, partsSpec, false);
				partsTableModel.addRow(model);
			}
		}
		partsTableModel.sort();
	}
	
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
	
	public Map<PartsIdentifier, PartsSpec> getSelectedParts() {
		return partsTableModel.getSelectedParts();
	}
	
	public Map<PartsSet, List<PartsIdentifier>> checkMissingPartsList(Collection<PartsSet> partsSets) {
		return partsTableModel.checkMissingPartsList(partsSets);
	}
	
	public void selectByPartsSet(Collection<PartsSet> partsSets) {
		partsTableModel.selectByPartsSet(partsSets);
	}
	
	public int getSelectedCount() {
		return partsTableModel.getSelectedCount();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		partsTable.setEnabled(enabled);
		partsTableModel.setEnabled(enabled);
		actSelectAll.setEnabled(enabled);
		actDeselectAll.setEnabled(enabled);
		super.setEnabled(enabled);
	}
}


interface ExportPresetResolve extends ExportResolverBase {
	
	int getSelectedCount();
	
	List<PartsSet> getSelectedPresets();
}

class ExportPresetSelectPanel extends AbstractImportPanel implements ExportPresetResolve {

	private static final long serialVersionUID = 1L;

	private ExportPartsResolver exportPartsResolver;
	
	private ExportPresetTableModel presetTableModel;
	
	private JTable presetTable;
	
	private Action actSelectAll;
	
	private Action actDeselectAll;


	protected ExportPresetSelectPanel(
			final ExportPartsResolver exportPartsResolver,
			final ExportInformationResolver exportInfoResolver,
			Collection<PartsSet> partsSets, String defaultPresetId) {

		this.exportPartsResolver = exportPartsResolver;
		
		setName("presetSelectPanel");

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);

		setName("choosePartsPanel");
		setBorder(BorderFactory.createTitledBorder(strings.getProperty("preset.title")));

		setLayout(new BorderLayout());
		
		presetTableModel = new ExportPresetTableModel();

		presetTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					fireChangeEvent();
				}
			}
		});
		exportPartsResolver.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				checkMissingParts();
			}
		});

		loadPresetInfo(partsSets, defaultPresetId);
		
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
					// BooleanのデフォルトのレンダラーはKCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}
				
				ExportPresetModel presetModel = presetTableModel.getRow(row);
				if (presetModel.isPresetParts()) {
					comp.setFont(getFont().deriveFont(Font.BOLD));
				} else {
					comp.setFont(getFont());
				}

				if (!isEnabled()) {
					comp.setForeground(disabledForeground);
				
				} else {
					if (presetModel.isSelected() && presetModel.getMissingPartsIdentifiers().size() > 0) {
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
		
		final Action actSelectUsedParts = new AbstractAction(
				strings.getProperty("preset.popup.selectUsedParts")) {
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
		Action actSort = new AbstractAction(strings.getProperty("parts.btn.sort")) {
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
	
	protected void loadPresetInfo(Collection<PartsSet> partsSets, String defaultPresetId) {
		presetTableModel.clear();
		for (PartsSet orgPartsSet : partsSets) {
			PartsSet partsSet = orgPartsSet.clone();
			ExportPresetModel model = new ExportPresetModel(partsSet, partsSet.isPresetParts());
			presetTableModel.addRow(model);
		}
		presetTableModel.setDefaultPresetId(defaultPresetId);
		presetTableModel.sort();
		checkMissingParts();
	}
	
	public void checkMissingParts() {
		ArrayList<PartsSet> changedPartsSets = new ArrayList<PartsSet>();
		HashMap<PartsSet, ExportPresetModel> partsSetModelMap = new HashMap<PartsSet, ExportPresetModel>();
		int mx = presetTableModel.getRowCount();
		for (int idx = 0; idx < mx; idx++) {
			ExportPresetModel presetModel = presetTableModel.getRow(idx);
			PartsSet partsSet = presetModel.getPartsSet();
			partsSetModelMap.put(partsSet, presetModel);
			changedPartsSets.add(partsSet);
		}
		Map<PartsSet, List<PartsIdentifier>> missingPartsIdentifiersMap = exportPartsResolver
				.checkMissingPartsList(changedPartsSets);
		for (Map.Entry<PartsSet, List<PartsIdentifier>> entry : missingPartsIdentifiersMap.entrySet()) {
			PartsSet partsSet = entry.getKey();
			List<PartsIdentifier> missingPartsIdentifiers = entry.getValue();
			ExportPresetModel presetModel = partsSetModelMap.get(partsSet);
			presetModel.setMissingPartsIdentifiers(missingPartsIdentifiers);
		}
		if (!missingPartsIdentifiersMap.isEmpty()) {
			presetTableModel.fireTableDataChanged();
		}
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
	
	public List<PartsSet> getSelectedPresets() {
		return presetTableModel.getSelectedPresets();
	}
	
	protected void exportUsedParts() {
		ArrayList<PartsSet> partsSets = new ArrayList<PartsSet>();
		int[] selRows = presetTable.getSelectedRows();
		for (int selRow : selRows) {
			ExportPresetModel presetModel = presetTableModel.getRow(selRow);
			partsSets.add(presetModel.getPartsSet());
		}
		
		exportPartsResolver.selectByPartsSet(partsSets);
	}
	
	public int getSelectedCount() {
		return presetTableModel.getSelectedCount();
	}
	
	public String getDefaultPresetId() {
		return presetTableModel.getDefaultPresetId();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		this.presetTable.setEnabled(enabled);
		this.presetTableModel.setEnabled(enabled);
		this.actSelectAll.setEnabled(enabled);
		this.actDeselectAll.setEnabled(enabled);
		super.setEnabled(enabled);
	}
}



class ExportPartsTableModel extends AbstractTableModelWithComboBoxModel<ExportPartsSelectModel> {

	private static final long serialVersionUID = 1L;

	private static final String[] columnNames;
	
	private static final int[] columnWidths;
	
	private boolean enabled = true;

	static {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);

		columnNames = new String[] {
				strings.getProperty("parts.column.selected"),
				strings.getProperty("parts.column.category"),
				strings.getProperty("parts.column.name"),
				strings.getProperty("parts.column.timestamp"),
				strings.getProperty("parts.column.author"),
				strings.getProperty("parts.column.version"),
		};
		
		columnWidths = new int[] {
				Integer.parseInt(strings.getProperty("parts.column.selected.width")),
				Integer.parseInt(strings.getProperty("parts.column.category.width")),
				Integer.parseInt(strings.getProperty("parts.column.name.width")),
				Integer.parseInt(strings.getProperty("parts.column.timestamp.width")),
				Integer.parseInt(strings.getProperty("parts.column.author.width")),
				Integer.parseInt(strings.getProperty("parts.column.version.width")),
		};
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < columnWidths.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(columnWidths[idx]);
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		ExportPartsSelectModel partsSelectModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return Boolean.valueOf(partsSelectModel.isChecked() && enabled);
		case 1:
			return partsSelectModel.getPartsCategory().getLocalizedCategoryName();
		case 2:
			return partsSelectModel.getPartsName();
		case 3:
			Timestamp tm = partsSelectModel.getTimestamp();
			if (tm != null) {
				return tm.toString();
			}
			return "";
		case 4:
			return partsSelectModel.getAuthor();
		case 5:
			return partsSelectModel.getVersion();
		default:
		}
		return "";
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ExportPartsSelectModel partsSelectModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			partsSelectModel.setChecked(((Boolean) aValue).booleanValue());
			break;
		default:
			return;
		}
		fireTableRowsUpdated(rowIndex, rowIndex);
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return isEnabled();
		}
		return false;
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
		default:
		}
		return String.class;
	}
	
	public void sort() {
		Collections.sort(elements);
		fireTableDataChanged();
	}
	
	public void sortByTimestamp() {
		Collections.sort(elements, new Comparator<ExportPartsSelectModel>() {
			public int compare(ExportPartsSelectModel o1, ExportPartsSelectModel o2) {
				int ret = 0;
				Timestamp t1 = o1.getTimestamp();
				Timestamp t2 = o2.getTimestamp();
				if (t1 == null || t2 == null) {
					if (t1 == null && t2 == null) {
						ret = 0;
					} else if (t1 == null) {
						ret = 1;
					} else {
						ret = -1;
					}
				} else {
					ret = t2.compareTo(t1); // 逆順(日付の新しいもの順)
				}
				if (ret == 0) {
					ret = o1.compareTo(o2);
				}
				return ret;
			}
		});
		fireTableDataChanged();
	}
	
	public void selectAll() {
		for (ExportPartsSelectModel model : elements) {
			model.setChecked(true);
		}
		fireTableDataChanged();
	}
	
	public void deselectAll() {
		for (ExportPartsSelectModel model : elements) {
			model.setChecked(false);
		}
		fireTableDataChanged();
	}
	
	/**
	 * 選択されているパーツイメージのマップを返す.<br>
	 * @return 選択されているパーツイメージのマップ
	 */
	public Map<PartsIdentifier, PartsSpec> getSelectedParts() {
		HashMap<PartsIdentifier, PartsSpec> selectedPartsMap = new HashMap<PartsIdentifier, PartsSpec>();
		for (ExportPartsSelectModel partsSelectModel : elements) {
			if (partsSelectModel.isChecked() && isEnabled()) {
				selectedPartsMap.put(partsSelectModel.getPartsIdentifier(), partsSelectModel.getPartsSpec());
			}
		}
		return selectedPartsMap;
	}
	
	/**
	 * パーツセットのコレクションを指定し、パーツセットの各パーツがすべてエクスポート対象になっているものだけを返す.<br>
	 * @param partsSets パーツセットのリスト
	 * @return 不足しているパーツセットと、その不足しているパーツリストを返す.
	 */
	public Map<PartsSet, List<PartsIdentifier>> checkMissingPartsList(Collection<PartsSet> partsSets) {
		if (partsSets == null) {
			throw new IllegalArgumentException();
		}
		Map<PartsIdentifier, PartsSpec> selectedPartsMap = getSelectedParts();
		HashMap<PartsSet, List<PartsIdentifier>> missingPartsMap = new HashMap<PartsSet, List<PartsIdentifier>>(); 

		for (PartsSet partsSet : partsSets) {
			ArrayList<PartsIdentifier> missingPartss = new ArrayList<PartsIdentifier>();
			for (List<PartsIdentifier> partsIdentifiers : partsSet.values()) {
				for (PartsIdentifier requirePartsIdentifier : partsIdentifiers) {
					if (!selectedPartsMap.containsKey(requirePartsIdentifier)) {
						missingPartss.add(requirePartsIdentifier);
					}
				}
			}
			Collections.sort(missingPartss);
			missingPartsMap.put(partsSet, missingPartss);
		}
		
		return missingPartsMap;
	}
	
	/**
	 * パーツセットで使用されているパーツを選択状態にする.
	 * @param partsSet パーツセットのコレクション
	 */
	public void selectByPartsSet(Collection<PartsSet> partsSets) {
		if (partsSets == null) {
			throw new IllegalArgumentException();
		}
		HashSet<PartsIdentifier> requirePartsIdentifiers = new HashSet<PartsIdentifier>();
		for (PartsSet partsSet : partsSets) {
			for (List<PartsIdentifier> partsIdentifiers : partsSet.values()) {
				for (PartsIdentifier partsIdentifier : partsIdentifiers) {
					requirePartsIdentifiers.add(partsIdentifier);
				}
			}
		}
		for (ExportPartsSelectModel partsSelectModel : elements) {
			if (requirePartsIdentifiers.contains(partsSelectModel.getPartsIdentifier())) {
				partsSelectModel.setChecked(true);
			}
		}
		fireTableDataChanged();
	}
	
	/**
	 * 選択されているパーツ数を返す.
	 * @return パーツ数
	 */
	public int getSelectedCount() {
		int count = 0;
		for (ExportPartsSelectModel partsSelectModel : elements) {
			if (partsSelectModel.isChecked() && isEnabled()) {
				count++;
			}
		}
		return count;
	}
	
	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			fireTableDataChanged();
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setCheck(int[] selRows, boolean checked) {
		if (selRows == null || selRows.length == 0) {
			return;
		}
		Arrays.sort(selRows);
		for (int selRow : selRows) {
			ExportPartsSelectModel rowModel = getRow(selRow);
			rowModel.setChecked(checked);
		}
		fireTableRowsUpdated(selRows[0], selRows[selRows.length - 1]);
	}
}


class ExportPartsSelectModel implements Comparable<ExportPartsSelectModel> {
	
	private boolean checked;
	
	private PartsIdentifier partsIdentifier;
	
	private PartsSpec partsSpec;
	
	private Timestamp timestamp;

	public ExportPartsSelectModel(PartsIdentifier partsIdentifier, PartsSpec partsSpec, boolean selected) {
		if (partsIdentifier == null || partsSpec == null) {
			throw new IllegalArgumentException();
		}
		this.partsIdentifier = partsIdentifier;
		this.partsSpec = partsSpec;
		this.checked = selected;
		
		long maxLastModified = partsSpec.getPartsFiles().lastModified();
		if (maxLastModified > 0) {
			timestamp = new Timestamp(maxLastModified);
		} else {
			timestamp = null;
		}
	}
	
	@Override
	public int hashCode() {
		return partsIdentifier.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ExportPartsSelectModel) {
			ExportPartsSelectModel o = (ExportPartsSelectModel) obj;
			return partsIdentifier.equals(o.partsIdentifier);
		}
		return false;
	}
	
	public int compareTo(ExportPartsSelectModel o) {
		int ret = (checked ? 0 : 1) - (o.checked ? 0 : 1); // 逆順
		if (ret == 0) {
			ret = partsIdentifier.compareTo(o.partsIdentifier);
		}
		return ret;
	}
	
	public PartsIdentifier getPartsIdentifier() {
		return this.partsIdentifier;
	}
	
	public PartsSpec getPartsSpec() {
		return this.partsSpec;
	}
	
	public boolean isChecked() {
		return checked;
	}
	
	public void setChecked(boolean checked) {
		this.checked = checked;
	}
	
	public PartsCategory getPartsCategory() {
		return this.partsIdentifier.getPartsCategory();
	}
	
	public String getPartsName() {
		return this.partsIdentifier.getLocalizedPartsName();
	}
	
	public Timestamp getTimestamp() {
		return timestamp == null ? null : (Timestamp) timestamp.clone();
	}
	
	public String getAuthor() {
		return partsSpec.getAuthor();
	}
	
	public String getVersion() {
		double version = partsSpec.getVersion();
		if (version <= 0) {
			return "";
		}
		return Double.toString(version);
	}
}


class ExportPresetTableModel extends AbstractTableModelWithComboBoxModel<ExportPresetModel> {

	private static final long serialVersionUID = 1L;

	private static final String[] columnNames;
	
	private static final int[] columnWidths;
	
	private boolean enabled = true;

	static {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(ExportWizardDialog.STRINGS_RESOURCE);

		columnNames = new String[] {
				strings.getProperty("preset.column.selected"),
				strings.getProperty("preset.column.default"),
				strings.getProperty("preset.column.name"),
				strings.getProperty("preset.column.missingparts"),
		};
		
		columnWidths = new int[] {
				Integer.parseInt(strings.getProperty("preset.column.selected.width")),
				Integer.parseInt(strings.getProperty("preset.column.default.width")),
				Integer.parseInt(strings.getProperty("preset.column.name.width")),
				Integer.parseInt(strings.getProperty("preset.column.missingparts.width")),
		};
	}
	
	private String defaultPresetId;
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < columnWidths.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(columnWidths[idx]);
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		ExportPresetModel presetModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return Boolean.valueOf(presetModel.isSelected() && isEnabled());
		case 1:
			return Boolean.valueOf(presetModel.getPartsSet().getPartsSetId().equals(defaultPresetId) && isEnabled());
		case 2:
			return presetModel.getPartsSetName();
		case 3:
			StringBuilder buf = new StringBuilder();
			for (PartsIdentifier partsIdentifier : presetModel.getMissingPartsIdentifiers()) {
				if (buf.length() > 0) {
					buf.append(", ");
				}
				buf.append(partsIdentifier.getLocalizedPartsName());
			}
			return buf.toString();
		default:
		}
		return "";
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ExportPresetModel presetModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			if (((Boolean) aValue).booleanValue()) {
				presetModel.setSelected(true);
			} else {
				presetModel.setSelected(false);
				if (presetModel.getPartsSet().getPartsSetId().equals(defaultPresetId)) {
					// 選択解除したものが既定のパーツセットであった場合、既定も解除する.
					defaultPresetId = null;
					fireTableRowsUpdated(rowIndex, rowIndex);
					return;
				}
			}
			break;
		case 1:
			if (((Boolean) aValue).booleanValue()) {
				defaultPresetId = presetModel.getPartsSet().getPartsSetId();
				presetModel.setSelected(true); // 既定のパーツセットにした場合は自動的にエクスポート対象にもする。
				fireTableDataChanged();
				return;
			}
		default:
			return;
		}
		fireTableRowsUpdated(rowIndex, rowIndex);
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return Boolean.class;
		case 1:
			return Boolean.class;
		case 2:
			return String.class;
		case 3:
			return String.class;
		default:
		}
		return String.class;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0 || columnIndex == 1) {
			return isEnabled();
		}
		return false;
	}
	
	public void sort() {
		Collections.sort(elements);
		fireTableDataChanged();
	}
	
	public void selectAll() {
		for (ExportPresetModel model : elements) {
			model.setSelected(true);
		}
		fireTableDataChanged();
	}
	
	public void deselectAll() {
		for (ExportPresetModel model : elements) {
			model.setSelected(false);
		}
		fireTableDataChanged();
	}
	
	/**
	 * 選択されているパーツセットのリストを返す.<br>
	 * なにもなければ空.<br>
	 * @return 選択されているパーツセットのリスト
	 */
	public List<PartsSet> getSelectedPresets() {
		ArrayList<PartsSet> partsSets = new ArrayList<PartsSet>();
		for (ExportPresetModel presetModel : elements) {
			if (presetModel.isSelected() && isEnabled()) {
				PartsSet partsSet = presetModel.getPartsSet().clone();
				partsSet.setPresetParts(true);
				partsSets.add(partsSet);
			}
		}
		return partsSets;
	}
	
	public int getSelectedCount() {
		int count = 0;
		for (ExportPresetModel presetModel : elements) {
			if (presetModel.isSelected() && isEnabled()) {
				count++;
			}
		}
		return count;
	}
	
	public String getDefaultPresetId() {
		return defaultPresetId;
	}
	
	/**
	 * デフォルトのプリセットを設定する.<br>
	 * @param defaultPresetId
	 */
	public void setDefaultPresetId(String defaultPresetId) {
		this.defaultPresetId = defaultPresetId;
	}
	
	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			fireTableDataChanged();
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}
}

class ExportPresetModel implements Comparable<ExportPresetModel> {

	private boolean selected;
	
	private PartsSet partsSet;
	
	private List<PartsIdentifier> missingPartsIdentifiers;
	
	public ExportPresetModel(PartsSet partsSet, boolean selected) {
		if (partsSet == null) {
			throw new IllegalArgumentException();
		}
		this.partsSet = partsSet;
		this.selected = selected;
	}
	
	@Override
	public int hashCode() {
		return partsSet.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ExportPresetModel) {
			ExportPresetModel o = (ExportPresetModel) obj;
			return partsSet.equals(o.partsSet);
		}
		return false;
	}
	
	public int compareTo(ExportPresetModel o) {
		int ret = (selected ? 0 : 1) - (o.selected ? 0 : 1);
		if (ret == 0) {
			ret = getPartsSetName().compareTo(o.getPartsSetName());
		}
		return ret;
	}
	
	public String getPartsSetName() {
		String name = partsSet.getLocalizedName();
		return name == null ? "" : name;
	}
	
	public boolean isPresetParts() {
		return partsSet.isPresetParts();
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public PartsSet getPartsSet() {
		return partsSet;
	}
	
	public void setMissingPartsIdentifiers(
			List<PartsIdentifier> missingPartsIdentifiers) {
		this.missingPartsIdentifiers = Collections.unmodifiableList(missingPartsIdentifiers);
	}
	
	public List<PartsIdentifier> getMissingPartsIdentifiers() {
		if (missingPartsIdentifiers == null) {
			return Collections.emptyList();
		}
		return missingPartsIdentifiers;
	}
	
}
