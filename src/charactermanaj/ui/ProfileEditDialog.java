package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import charactermanaj.Main;
import charactermanaj.graphics.colormodel.ColorModels;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.RecommendationURL;
import charactermanaj.model.io.CharacterDataDefaultProvider;
import charactermanaj.model.io.CharacterDataDefaultProvider.DefaultCharacterDataVersion;
import charactermanaj.model.io.CharacterDataPersistent;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;


public class ProfileEditDialog extends JDialog {

	private static final long serialVersionUID = 8559918820826437849L;

	/**
	 * ローカライズ文字列
	 */
	protected static final String STRINGS_RESOURCE = "languages/profileditdialog";

	protected static class JTextFieldEx extends JTextField {

		private static final long serialVersionUID = -8608404290439184405L;
		
		private boolean error;
		
		@Override
		public Color getBackground() {
			if (error) {
				AppConfig appConfig = AppConfig.getInstance();
				return appConfig.getInvalidBgColor();
			}
			return super.getBackground();
		}
		
		public void setError(boolean error) {
			if (this.error != error) {
				this.error = error;
				repaint();
			}
		}
		
		public boolean isError() {
			return error;
		}
	}
	

	/**
	 * オリジナルのデータ.
	 */
	private CharacterData original;

	
	/**
	 * キャラクターデータID
	 */
	private JTextFieldEx txtCharacterID;

	/**
	 * キャラクターデータ Rev
	 */
	private JTextFieldEx txtCharacterRev;

	/**
	 * キャラクターデータ DocBase(読み込み専用)
	 */
	private JTextField txtCharacterDocBase;

	
	/**
	 * キャラクター名
	 */
	private JTextFieldEx txtCharacterName;
	
	/**
	 * イメージ幅
	 */
	private JSpinner txtImageWidth;
	
	/**
	 * イメージ高さ
	 */
	private JSpinner txtImageHeight;
	
	/**
	 * 作者
	 */
	private JTextField txtAuthor;
	
	/**
	 * 説明
	 */
	private JTextArea txtDescription;
	
	/**
	 * ディレクトリの監視
	 */
	private JCheckBox chkWatchDir;


	/**
	 * カラーグループのモデル
	 */
	private ColorGroupsTableModel colorGroupsTableModel;

	/**
	 * カテゴリのモデル
	 */
	private CategoriesTableModel categoriesTableModel;
	
	/**
	 * レイヤーのモデル
	 */
	private LayersTableModel layersTableModel;
	
	/**
	 * パーツセットのモデル
	 */
	private PartssetsTableModel partssetsTableModel;
	
	/**
	 * お勧めリンクのモデル
	 */
	private RecommendationTableModel recommendationsTableModel;
	
	
	/**
	 * 画面の内容から生成された新しいキャラクターデータ、もしくはnull
	 */
	private CharacterData result;
	

	/**
	 * OKボタン
	 */
	private JButton btnOK;
	

	/**
	 * キャラクターデータの編集画面を構築する.<br>
	 * 
	 * @param parent
	 *            親、もしくはnull
	 * @param original
	 *            オリジナルのキャラクターデータ(変更されない)
	 */
	public ProfileEditDialog(JFrame parent, CharacterData original) {
		super(parent, true);
		initDialog(parent, original);
	}
	
	/**
	 * キャラクターデータの編集画面を構築する.<br>
	 * 
	 * @param parent
	 *            親、もしくはnull
	 * @param original
	 *            オリジナルのキャラクターデータ(変更されない)
	 */
	public ProfileEditDialog(JDialog parent, CharacterData original) {
		super(parent, true);
		initDialog(parent, original);
	}

	/**
	 * 編集ダイアログを初期化する.
	 * 
	 * @param origianl
	 *            編集もとキャラクター定義
	 */
	private void initDialog(Component parent, CharacterData original) {
		// 元情報
		if (original == null) {
			throw new IllegalArgumentException();
		}
		this.original = original;
		
		// ウィンドウイベントのハンドル
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});

		// 設定
		AppConfig appConfig = AppConfig.getInstance();
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		// タイトル
		String title;
		if (original.isValid()) {
			title = strings.getProperty("title.edit");
		} else {
			title = strings.getProperty("title.new");
		}
		setTitle(title);
		
		// OK/CANCEL PANEL
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 42));
		GridBagLayout buttonsPanelLayout = new GridBagLayout();
		buttonsPanel.setLayout(buttonsPanelLayout);
		GridBagConstraints gbc = new GridBagConstraints();
	
		String okCaption;
		if (original.isValid()) {
			okCaption = strings.getProperty("button.ok.edit");
		} else {
			okCaption = strings.getProperty("button.ok.new");
		}
		
		btnOK = new JButton(new AbstractAction(okCaption) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		});
		btnOK.setEnabled(false); // 初期状態はディセーブル、updateUIStateで更新する.
		
		Action actOpenDir = new AbstractAction(strings.getProperty("button.openDir")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onOpenDir();
			}
		};
		actOpenDir.setEnabled(original.isValid());
		JButton btnOpenDir = new JButton(actOpenDir);

		Action actCancel = new AbstractAction(strings.getProperty("button.cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};
		JButton btnCancel = new JButton(actCancel);
	
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		buttonsPanel.add(btnOpenDir, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		buttonsPanel.add(Box.createGlue(), gbc);

		gbc.gridx = Main.isLinuxOrMacOSX() ? 3 : 2;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		buttonsPanel.add(btnOK, gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 2 : 3;
		gbc.gridy = 0;
		buttonsPanel.add(btnCancel, gbc);
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(buttonsPanel, BorderLayout.SOUTH);

		// InputMap/ActionMap
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(btnOK);

		InputMap im = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeWindow");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeWindow");
		rootPane.getActionMap().put("closeWindow", actCancel);
		
		// Main
		JPanel mainPanel = new JPanel();
		GridBagLayout mainPanelLayout = new GridBagLayout();
		mainPanel.setLayout(mainPanelLayout);
		
		this.txtCharacterID = new JTextFieldEx();
		this.txtCharacterRev = new JTextFieldEx();
		this.txtCharacterDocBase = new JTextField();

		this.txtCharacterID.setEditable(true);
		this.txtCharacterRev.setEditable(true);

		this.txtCharacterDocBase.setEditable(false);

		this.txtCharacterName = new JTextFieldEx();
		this.txtImageWidth = new JSpinner(new SpinnerNumberModel(1, 1,
				Integer.MAX_VALUE, 1)); // 現実に可能であるかを問わず制限を設けない
		this.txtImageHeight = new JSpinner(new SpinnerNumberModel(1, 1,
				Integer.MAX_VALUE, 1)); // 現実に可能であるかを問わず制限を設けない
		this.txtAuthor = new JTextField();
		this.txtDescription = new JTextArea();
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(1, 3, 1, 5);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("docbase.caption"), SwingConstants.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtCharacterDocBase, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(1, 3, 1, 5);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("id.caption"), SwingConstants.RIGHT), gbc);
		
		txtCharacterID.setToolTipText(strings.getProperty("id.caption.help"));
		txtCharacterRev.setToolTipText(strings.getProperty("rev.caption.help"));

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtCharacterID, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(1, 3, 1, 5);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("rev.caption"), SwingConstants.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtCharacterRev, gbc);
		
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("name.caption"), SwingConstants.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtCharacterName, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("image-width.caption"), SwingConstants.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtImageWidth, gbc);

		gbc.gridx = 2;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("image-height.caption"), JLabel.RIGHT), gbc);

		gbc.gridx = 3;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtImageHeight, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("author.caption"), SwingConstants.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(txtAuthor, gbc);

		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(strings.getProperty("description.caption"), SwingConstants.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.gridwidth = 3;
		gbc.gridheight = 2;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JScrollPane(txtDescription), gbc);

		// model
		
		this.colorGroupsTableModel = new ColorGroupsTableModel();
		this.categoriesTableModel = new CategoriesTableModel();
		this.layersTableModel = new LayersTableModel();
		this.partssetsTableModel = new PartssetsTableModel();
		this.recommendationsTableModel = new RecommendationTableModel();
		
		this.colorGroupsTableModel.setEditable(true);
		this.categoriesTableModel.setEditable(true);
		this.layersTableModel.setEditable(true);
		this.partssetsTableModel.setEditable(true);
		this.recommendationsTableModel.setEditable(true);
		
		// colorGroup
		JPanel colorGroupPanel = new JPanel(new BorderLayout());
		final JTable colorGroupTable = new JTable(colorGroupsTableModel);
		colorGroupTable.setShowGrid(true);
		colorGroupTable.setGridColor(appConfig.getGridColor());
		colorGroupTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		colorGroupTable.setRowHeight(colorGroupTable.getRowHeight() + 4);
		colorGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		colorGroupPanel.add(new JScrollPane(colorGroupTable), BorderLayout.CENTER);
		
		JPanel colorGroupBtnPanel = new JPanel();
		GridBagLayout colorGroupBtnPanelLayout = new GridBagLayout();
		colorGroupBtnPanel.setLayout(colorGroupBtnPanelLayout);

		AbstractAction actColorGroupAdd = new AbstractAction(strings.getProperty("colorgroup.add.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				colorGroupsTableModel.addNewColorGroup();
			}
		};
		AbstractAction actColorGroupDel = new AbstractAction(strings.getProperty("colorgroup.delete.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int selRow = colorGroupTable.getSelectedRow();
				if (selRow >= 0) {
					ColorGroupsTableRow colorGroup = colorGroupsTableModel.getRow(selRow);
					if (layersTableModel.isUsed(colorGroup)) {
						JOptionPane.showMessageDialog(ProfileEditDialog.this, strings.getProperty("warning.used-colorgroup"));
					} else {
						colorGroupsTableModel.removeRow(selRow);
					}
				}
			}
		};
		AbstractAction actColorGroupMoveUp = new AbstractAction(strings.getProperty("colorgroup.moveup.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = colorGroupTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = colorGroupsTableModel.moveUp(rowIndex);
					colorGroupTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		AbstractAction actColorGroupMoveDown = new AbstractAction(strings.getProperty("colorgroup.movedown.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = colorGroupTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = colorGroupsTableModel.moveDown(rowIndex);
					colorGroupTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		colorGroupBtnPanel.add(new JButton(actColorGroupAdd), gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		colorGroupBtnPanel.add(new JButton(actColorGroupDel), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		colorGroupBtnPanel.add(new JButton(actColorGroupMoveUp), gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		colorGroupBtnPanel.add(new JButton(actColorGroupMoveDown), gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weighty = 1.;
		colorGroupBtnPanel.add(Box.createGlue(), gbc);

		colorGroupPanel.add(colorGroupBtnPanel, BorderLayout.EAST);
		
		final Color disabledForeground = appConfig.getDisabledCellForgroundColor();

		// categories
		JPanel categoriesPanel = new JPanel(new BorderLayout());
		final JTable categoriesTable = new JTable(categoriesTableModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				if (comp instanceof JCheckBox) {
					// BooleanのデフォルトのレンダラーはJCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}

				if (isCellSelected(row, column)) {
					comp.setForeground(getSelectionForeground());
					comp.setBackground(getSelectionBackground());

				} else {
					// 前景色、ディセーブル時は灰色
					Color foregroundColor = getForeground();
					comp.setForeground(isEnabled() ? foregroundColor : disabledForeground);
					comp.setBackground(getBackground());
				}

				return comp;
			}
		};
		categoriesTable.setShowGrid(true);
		categoriesTable.setGridColor(appConfig.getGridColor());
		categoriesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		categoriesTable.setRowHeight(categoriesTable.getRowHeight() + 4);
		categoriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		categoriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		categoriesPanel.add(new JScrollPane(categoriesTable), BorderLayout.CENTER);
		
		categoriesTableModel.adjustColumnModel(categoriesTable.getColumnModel());
		
		JPanel categoriesBtnPanel = new JPanel();
		GridBagLayout categoryBtnPanelLayout = new GridBagLayout();
		categoriesBtnPanel.setLayout(categoryBtnPanelLayout);
		
		AbstractAction actCategoryAdd = new AbstractAction(strings.getProperty("categories.add.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				categoriesTableModel.addCategory();
			}
		};
		AbstractAction actCategoryDel = new AbstractAction(strings.getProperty("categories.delete.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int selRow = categoriesTable.getSelectedRow();
				if (selRow >= 0) {
					CategoriesTableRow partsCategory = categoriesTableModel.getRow(selRow);
					if (layersTableModel.isUsed(partsCategory)) {
						JOptionPane.showMessageDialog(ProfileEditDialog.this, strings.getProperty("warning.used-category"));
					} else {
						categoriesTableModel.removeRow(selRow);
					}
				}
			}
		};
		AbstractAction actCategoryMoveUp = new AbstractAction(strings.getProperty("categories.moveup.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = categoriesTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = categoriesTableModel.moveUp(rowIndex);
					categoriesTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		AbstractAction actCategoryMoveDown = new AbstractAction(strings.getProperty("categories.movedown.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = categoriesTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = categoriesTableModel.moveDown(rowIndex);
					categoriesTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		categoriesBtnPanel.add(new JButton(actCategoryAdd), gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		categoriesBtnPanel.add(new JButton(actCategoryDel), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		categoriesBtnPanel.add(new JButton(actCategoryMoveUp), gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		categoriesBtnPanel.add(new JButton(actCategoryMoveDown), gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weighty = 1.;
		categoriesBtnPanel.add(Box.createGlue(), gbc);

		categoriesPanel.add(categoriesBtnPanel, BorderLayout.EAST);

		
		// layers
		JPanel layersPanel = new JPanel(new BorderLayout());
		final Color invalidBgColor = appConfig.getInvalidBgColor();
		final JTable layersTable = new JTable(layersTableModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				
				if (comp instanceof JCheckBox) {
					// BooleanのデフォルトのレンダラーはJCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}
				
				LayersTableModel model = (LayersTableModel) getModel();
				LayersTableRow layer = model.getRow(row);
				comp.setForeground(getForeground());
				comp.setBackground(layer.isValid() ? getBackground() : invalidBgColor);

				// 前景色、ディセーブル時は灰色
				Color foregroundColor = getForeground();
				comp.setForeground(isEnabled() ? foregroundColor : disabledForeground);
				
				return comp;
			}
		};
		layersTableModel.adjustColumnModel(layersTable.getColumnModel());

		JComboBox colorGroupCombo = new JComboBox(
				new FirstItemInjectionComboBoxModelWrapper<ColorGroupsTableRow>(colorGroupsTableModel, ColorGroupsTableRow.NA));
		JComboBox categoriesCombo = new JComboBox(categoriesTableModel);
		JComboBox colorModelsCombo = new JComboBox(ColorModels.values());
		
		layersTable.setShowGrid(true);
		layersTable.setGridColor(appConfig.getGridColor());
		layersTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		layersTable.setRowHeight(layersTable.getRowHeight() + 4);
		layersTable.setDefaultEditor(ColorGroupsTableRow.class, new DefaultCellEditor(colorGroupCombo));
		layersTable.setDefaultEditor(CategoriesTableRow.class, new DefaultCellEditor(categoriesCombo));
		layersTable.setDefaultEditor(ColorModels.class, new DefaultCellEditor(colorModelsCombo));

		layersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		layersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		layersPanel.add(new JScrollPane(layersTable), BorderLayout.CENTER);
		
		JPanel layersBtnPanel = new JPanel();
		GridBagLayout layersBtnPanelLayout = new GridBagLayout();
		layersBtnPanel.setLayout(layersBtnPanelLayout);
		
		AbstractAction actLayerAdd = new AbstractAction(strings.getProperty("layers.add.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				layersTableModel.addNewLayer();
			}
		};
		AbstractAction actLayerDel = new AbstractAction(strings.getProperty("layers.delete.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int selRow = layersTable.getSelectedRow();
				if (selRow >= 0) {
					layersTableModel.removeRow(selRow);
				}
			}
		};
		AbstractAction actLayerSort = new AbstractAction(strings.getProperty("layers.sort.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				layersTableModel.sort();
			}
		};
		AbstractAction actLayerMoveUp = new AbstractAction(strings.getProperty("layers.moveup.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = layersTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = layersTableModel.moveUp(rowIndex);
					layersTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		AbstractAction actLayerMoveDown = new AbstractAction(strings.getProperty("layers.movedown.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = layersTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = layersTableModel.moveDown(rowIndex);
					layersTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		layersBtnPanel.add(new JButton(actLayerAdd), gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		layersBtnPanel.add(new JButton(actLayerDel), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		layersBtnPanel.add(new JButton(actLayerMoveUp), gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		layersBtnPanel.add(new JButton(actLayerMoveDown), gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		layersBtnPanel.add(new JButton(actLayerSort), gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.weighty = 1.;
		layersBtnPanel.add(Box.createGlue(), gbc);

		layersPanel.add(layersBtnPanel, BorderLayout.EAST);
		
		chkWatchDir = new JCheckBox(strings.getProperty("layers.watchdir"));
		layersPanel.add(chkWatchDir, BorderLayout.SOUTH);
		
		// Presets
		JPanel partssetsPanel = new JPanel(new BorderLayout());
		JTable partssetsTable = new JTable(partssetsTableModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				if (comp instanceof JCheckBox) {
					// BooleanのデフォルトのレンダラーはJCheckBoxを継承したJTable$BooleanRenderer
					comp.setEnabled(isCellEditable(row, column) && isEnabled());
				}

				if (isCellSelected(row, column)) {
					comp.setForeground(getSelectionForeground());
					comp.setBackground(getSelectionBackground());

				} else {
					// 前景色、ディセーブル時は灰色
					Color foregroundColor = getForeground();
					comp.setForeground(isEnabled() ? foregroundColor : disabledForeground);
					comp.setBackground(getBackground());
				}

				return comp;
			}
		};
		partssetsTableModel.adjustColumnModel(partssetsTable.getColumnModel());

		partssetsTable.setRowHeight(layersTable.getRowHeight() + 4);

		partssetsTable.setShowGrid(true);
		partssetsTable.setGridColor(appConfig.getGridColor());
		partssetsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		partssetsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		partssetsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		partssetsPanel.add(new JScrollPane(partssetsTable), BorderLayout.CENTER);

		// Recommendations
		JPanel recommendationsPanel = new JPanel(new BorderLayout());
		final JTable recommendationsTable = new JTable(recommendationsTableModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);

				if (isCellSelected(row, column)) {
					comp.setForeground(getSelectionForeground());
					comp.setBackground(getSelectionBackground());

				} else {
					// 前景色、ディセーブル時は灰色
					Color foregroundColor = getForeground();
					comp.setForeground(isEnabled() ? foregroundColor : disabledForeground);
					comp.setBackground(getBackground());
				}
				return comp;
			}
		};
		recommendationsTableModel.adjustColumnModel(recommendationsTable.getColumnModel());

		recommendationsTable.setRowHeight(layersTable.getRowHeight() + 4);

		recommendationsTable.setShowGrid(true);
		recommendationsTable.setGridColor(appConfig.getGridColor());
		recommendationsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		recommendationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		recommendationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		recommendationsPanel.add(new JScrollPane(recommendationsTable), BorderLayout.CENTER);

		JPanel recommendationsBtnPanel = new JPanel();
		GridBagLayout recommendationsBtnPanelLayout = new GridBagLayout();
		recommendationsBtnPanel.setLayout(recommendationsBtnPanelLayout);
		
		AbstractAction actRecommendationAdd = new AbstractAction(strings.getProperty("recommendations.add.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				recommendationsTableModel.addNew();
			}
		};
		AbstractAction actRecommendationDel = new AbstractAction(strings.getProperty("recommendations.delete.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int selRow = recommendationsTable.getSelectedRow();
				if (selRow >= 0) {
					recommendationsTableModel.removeRow(selRow);
				}
			}
		};
		AbstractAction actRecommendationMoveUp = new AbstractAction(strings.getProperty("recommendations.moveup.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = recommendationsTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = recommendationsTableModel.moveUp(rowIndex);
					recommendationsTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		AbstractAction actRecommendationMoveDown = new AbstractAction(strings.getProperty("recommendations.movedown.caption")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int rowIndex = recommendationsTable.getSelectedRow();
				if (rowIndex >= 0) {
					int newSel = recommendationsTableModel.moveDown(rowIndex);
					recommendationsTable.getSelectionModel().setSelectionInterval(newSel, newSel);
				}
			}
		};
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		recommendationsBtnPanel.add(new JButton(actRecommendationAdd), gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		recommendationsBtnPanel.add(new JButton(actRecommendationDel), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		recommendationsBtnPanel.add(new JButton(actRecommendationMoveUp), gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		recommendationsBtnPanel.add(new JButton(actRecommendationMoveDown), gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weighty = 1.;
		recommendationsBtnPanel.add(Box.createGlue(), gbc);

		recommendationsPanel.add(recommendationsBtnPanel, BorderLayout.EAST);
		
		
		// データのロード
		loadCharacterData(original);
		
		// レイヤーのカテゴリ使用状態を監視するリスナ関連
		final HashMap<CategoriesTableRow, List<LayersTableRow>> usedLayerMap = new HashMap<CategoriesTableRow, List<LayersTableRow>>();
		final Runnable resetUsedLayers = new Runnable() {
			public void run() {
				usedLayerMap.clear();
			}
		};
		layersTableModel.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent e) {
				resetUsedLayers.run();
			}
			public void intervalAdded(ListDataEvent e) {
				resetUsedLayers.run();
			}
			public void intervalRemoved(ListDataEvent e) {
				resetUsedLayers.run();
			}
		});
		categoriesTableModel.setUsedCategoryDetector(new CategoriesTableModel.UsedCategoryDetector() {
			public List<LayersTableRow> getLayers(CategoriesTableRow partsCategory) {
				if (usedLayerMap.isEmpty()) {
					int mx = layersTableModel.getRowCount();
					for (int idx = 0; idx < mx; idx++) {
						LayersTableRow layer = layersTableModel.getRow(idx);
						CategoriesTableRow pc = layer.getPartsCategory();
						List<LayersTableRow> usedLayers = usedLayerMap.get(pc);
						if (usedLayers == null) {
							usedLayers = new ArrayList<LayersTableRow>();
							usedLayerMap.put(pc, usedLayers);
						}
						usedLayers.add(layer);
					}
				}
				return usedLayerMap.get(partsCategory);
			}
		});
		
		// 生成可能であるかチェックするためのリスナ
		layersTableModel.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent e) {
				updateUIState();
				layersTable.repaint(); // エラー有無表示を最新の状態で再判定・再描画するため
			}
			public void intervalAdded(ListDataEvent e) {
				updateUIState();
			}
			public void intervalRemoved(ListDataEvent e) {
				updateUIState();
			}
		});
		
		// キャラクターID/REV/NAMEが変更されたことを通知され、OKボタンを判定するためのリスナ
		DocumentListener textChangeListener = new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				updateUIState();
			}
			public void insertUpdate(DocumentEvent e) {
				updateUIState();
			}
			public void changedUpdate(DocumentEvent e) {
				updateUIState();
			}
		};
		txtCharacterID.getDocument().addDocumentListener(textChangeListener);
		txtCharacterRev.getDocument().addDocumentListener(textChangeListener);
		txtCharacterName.getDocument().addDocumentListener(textChangeListener);
		
		// TABS
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add(strings.getProperty("panel.basicinfomation"), mainPanel);
		tabbedPane.add(strings.getProperty("panel.colorgroup"), colorGroupPanel);
		tabbedPane.add(strings.getProperty("panel.categories"), categoriesPanel);
		tabbedPane.add(strings.getProperty("panel.layers"), layersPanel);
		tabbedPane.add(strings.getProperty("panel.partssets"), partssetsPanel);
		tabbedPane.add(strings.getProperty("panel.recommendations"), recommendationsPanel);
		
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
		setSize(500, 500);
		setLocationRelativeTo(parent);

		updateUIState();
	}
	
	/**
	 * CharacterDataから画面へ転記する.
	 * 
	 * @param original
	 *            オリジナル情報
	 */
	protected void loadCharacterData(CharacterData original) {
		if (original == null) {
			throw new IllegalArgumentException();
		}

		colorGroupsTableModel.clear();
		categoriesTableModel.clear();
		layersTableModel.clear();
		partssetsTableModel.clear();
		recommendationsTableModel.clear();

		// 基本情報
		txtCharacterID.setText(original.getId());
		txtCharacterRev.setText(original.getRev());
		
		txtCharacterDocBase.setText(original.getDocBase() == null ? "" : original.getDocBase().toString());

		txtCharacterName.setText(original.getName());
		txtAuthor.setText(original.getAuthor() != null ? original.getAuthor() : "");
		txtDescription.setText(original.getDescription() != null ? original.getDescription() : "");
		Dimension siz = original.getImageSize();
		txtImageWidth.setValue(siz != null ? siz.width : 300);
		txtImageHeight.setValue(siz != null ? siz.height : 400);

		// カラーグループ
		HashMap<ColorGroup, ColorGroupsTableRow> colorGroupMap = new HashMap<ColorGroup, ColorGroupsTableRow>();
		for (ColorGroup colorGroup : original.getColorGroups()) {
			if (colorGroup.isEnabled()) {
				ColorGroupsTableRow mutableColorGroup = ColorGroupsTableRow.valueOf(colorGroup);
				colorGroupsTableModel.addRow(mutableColorGroup);
				colorGroupMap.put(colorGroup, mutableColorGroup);
			}
		}
		
		// カテゴリとレイヤー
		for (PartsCategory partsCategory : original.getPartsCategories()) {
			categoriesTableModel.addRow(new CategoriesTableRow(partsCategory));
			for (Layer layer : partsCategory.getLayers()) {
				LayersTableRow editableLayer = new LayersTableRow();
				ColorGroupsTableRow mutableColorGroup = colorGroupMap.get(layer.getColorGroup());
				if (mutableColorGroup == null) {
					mutableColorGroup = ColorGroupsTableRow.NA;
				}
				editableLayer.setColorGroup(mutableColorGroup);
				editableLayer.setPartsCategory(new CategoriesTableRow(partsCategory));
				editableLayer.setDir(layer.getDir());
				editableLayer.setColorModel(ColorModels.safeValueOf(layer.getColorModelName()));
				editableLayer.setOrder(layer.getOrder());
				editableLayer.setLayerId(layer.getId());
				editableLayer.setLayerName(layer.getLocalizedName());
				layersTableModel.addRow(editableLayer);
			}
		}
		
		// ディレクトリ監視有無
		chkWatchDir.setSelected(original.isWatchDirectory());
		
		// パーツセット
		ArrayList<PartsSet> partsSets = new ArrayList<PartsSet>();
		partsSets.addAll(original.getPartsSets().values());
		Collections.sort(partsSets, PartsSet.DEFAULT_COMPARATOR);
		for (PartsSet partsSet : partsSets) {
			partssetsTableModel.addRow(new PresetsTableRow(partsSet));
		}
		partssetsTableModel.setDefaultPartsSetId(original.getDefaultPartsSetId());

		// お勧めリンク
		List<RecommendationURL> recommendationURLList = original.getRecommendationURLList();
		if (recommendationURLList == null) {
			// キャラクターデータのお勧めリンクがnull(古い形式)の場合は、デフォルトのお勧めリンクで代替する.
			CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
			CharacterData defaultCd = defProv
					.createDefaultCharacterData(DefaultCharacterDataVersion.V3);
			recommendationURLList = defaultCd.getRecommendationURLList();
		}
		if (recommendationURLList != null) {
			for (RecommendationURL recommendationURL : recommendationURLList) {
				recommendationsTableModel.addRow(new RecommendationTableRow(recommendationURL));
			}
		}
	}
	

	protected void onOpenDir() {
		try {
			URI docBase = original.getDocBase();
			if (!DesktopUtilities.browseBaseDir(docBase)) {
				JOptionPane.showMessageDialog(this, docBase);
			}
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	/**
	 * 画面を閉じる場合
	 */
	protected void onClose() {
		result = null;
		
		boolean writable = !original.isValid() || original.canWrite(); // 新規または更新可能
		if (writable) {
			final Properties strings = LocalizedResourcePropertyLoader
					.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);
			if (JOptionPane.showConfirmDialog(this, strings.get("confirm.close"), strings.getProperty("confirm"),
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
				// YESでなければ継続しない.
				return;
			}
		}
		dispose();
	}
	
	/**
	 * 画面の状態を更新する
	 */
	protected void updateUIState() {
		boolean valid = isValidData(new ValidationReport() {
			public void validateReport(JComponent comp, boolean valid) {
				if (comp != null && comp instanceof JTextFieldEx) {
					((JTextFieldEx) comp).setError(!valid);
				}
			}
		});
		boolean writable = !original.isValid() || original.canWrite(); // 新規または更新可能
		btnOK.setEnabled(valid && writable);
	}

	private interface ValidationReport {
		
		void validateReport(JComponent comp, boolean valid);
		
	}

	/**
	 * 入力データが有効であるか判定する.
	 * 
	 * @return 有効であればtrue
	 */
	protected boolean isValidData(ValidationReport report) {
		
		// ID, REVが英数字であるか判定
		Pattern pat = Pattern.compile("\\p{Graph}+");
		String id = txtCharacterID.getText().trim();
		String rev = txtCharacterRev.getText().trim();

		boolean validId = pat.matcher(id).matches();
		boolean validRev = pat.matcher(rev).matches();
		boolean validName = txtCharacterName.getText().trim().length() > 0;

		// レイヤーの不備判定
		boolean validLayers = true;
		int cnt = 0;
		int mx = layersTableModel.getRowCount();
		for (int idx = 0; idx < mx; idx++) {
			LayersTableRow layer = layersTableModel.getRow(idx);
			if (!layer.isValid()) {
				// レイヤーに不備がある
				validLayers = false;
				break;
			}
			cnt++;
		}
		if (cnt == 0) {
			// レイヤーがない
			validLayers = false;
		}

		if (report != null) {
			report.validateReport(txtCharacterID, validId);
			report.validateReport(txtCharacterRev, validRev);
			report.validateReport(txtCharacterName, validName);
		}
		
		return validLayers && validId && validRev && validName;
	}
	
	/**
	 * OKボタン押下
	 */
	protected void onOK() {
		if ( !isValidData(null)) {
			// 編集可能でないか、まだ保存可能になっていない場合はビープ音を鳴らして警告
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}

		// 編集可能であり、且つ、保存可能な状態であれば
		CharacterData newCd = createCharacterData(); 
		final Properties strings = LocalizedResourcePropertyLoader
			.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);
		if (original.isValid() && !original.isSameStructure(newCd)) {
			if (original.getRev().equals(newCd.getRev())) {
				// 構造が変更されているがREVが変らない場合
				int ret = JOptionPane.showConfirmDialog(this,
					strings.get("confirm.needchangerevision"),
					strings.getProperty("confirm"),
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE); 
				if (ret == JOptionPane.CANCEL_OPTION) {
					return;
				}

				if (ret == JOptionPane.YES_OPTION) {
					// リビジョンを生成して割り当てる
					CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
					newCd.setRev(persist.generateRev());
				}
				
			} else if ( !newCd.isUpperCompatibleStructure(original)){
				// 上位互換のない構造が変更されていることを通知する.
				if (JOptionPane.showConfirmDialog(this,
						strings.get("confirm.changestructre"),
						strings.getProperty("confirm"),
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}
			}
		}
		result = newCd;
		dispose();
		return;
	}
	
	/**
	 * 画面の情報からキャラクターデータを構築して返します.<br>
	 * 
	 * @return キャラクターデータ
	 */
	protected CharacterData createCharacterData() {
		CharacterData cd = new CharacterData();

		// オリジナルのDocBaseVを転記する.
		cd.setDocBase(original.getDocBase());
		
		// ID, REV
		cd.setId(txtCharacterID.getText().trim());
		cd.setRev(txtCharacterRev.getText().trim());

		// キャラクターセット名
		cd.setName(txtCharacterName.getText().trim());
		
		// 情報
		cd.setAuthor(txtAuthor.getText().trim());
		cd.setDescription(txtDescription.getText());
		
		// サイズ
		Dimension imageSize = new Dimension();
		imageSize.width = ((Number)(txtImageWidth.getValue())).intValue();
		imageSize.height = ((Number)(txtImageHeight.getValue())).intValue();
		cd.setImageSize(imageSize);
		
		// カラーグループ
		int mxColorGroup = colorGroupsTableModel.getRowCount();
		ArrayList<ColorGroup> colorGroups = new ArrayList<ColorGroup>();
		for (int idx = 0; idx < mxColorGroup; idx++) {
			colorGroups.add(colorGroupsTableModel.getRow(idx).convert());
		}
		cd.setColorGroups(colorGroups);
		
		// レイヤーの構築
		HashMap<CategoriesTableRow, List<Layer>> layerMap = new HashMap<CategoriesTableRow, List<Layer>>();
		int mxLayer = layersTableModel.getRowCount();
		for (int idx = 0; idx < mxLayer; idx++) {
			LayersTableRow editableLayer = layersTableModel.getRow(idx);
			Layer layer = editableLayer.toLayer();
			CategoriesTableRow partsCategory = editableLayer.getPartsCategory();
			if (layer != null && partsCategory != null) {
				List<Layer> layers = layerMap.get(partsCategory);
				if (layers == null) {
					layers = new ArrayList<Layer>();
					layerMap.put(partsCategory, layers);
				}
				layers.add(layer);
			}
		}
		
		// カテゴリおよびレイヤー
		ArrayList<PartsCategory> categories = new ArrayList<PartsCategory>();
		int mxCategory = categoriesTableModel.getRowCount();
		for (int idx = 0; idx < mxCategory; idx++) {
			CategoriesTableRow partsCategory = categoriesTableModel.getRow(idx);
			List<Layer> layers = layerMap.get(partsCategory);
			if (layers != null) {
				partsCategory.setLayers(layers);
				categories.add(partsCategory.convert());
			}
		}
		cd.setPartsCategories(categories.toArray(new PartsCategory[categories.size()]));

		// ディレクトリの監視
		cd.setWatchDirectory(chkWatchDir.isSelected());
		
		// パーツセット情報
		int mxPartssets = partssetsTableModel.getRowCount();
		for (int idx = 0; idx < mxPartssets; idx++) {
			PartsSet partsSet = partssetsTableModel.getRow(idx).convert();
			cd.addPartsSet(partsSet);
		}
		cd.setDefaultPartsSetId(partssetsTableModel.getDefaultPartsSetId());
		
		// お気に入りリンク情報
		int mxRecommendations = recommendationsTableModel.getRowCount();
		ArrayList<RecommendationURL> recommendationURLList = new ArrayList<RecommendationURL>();
		for (int idx = 0; idx < mxRecommendations; idx++) {
			RecommendationTableRow row = recommendationsTableModel.getRow(idx);
			String displayName = row.getLocalizedName();
			String url = row.getURL();
			if ((displayName != null && displayName.trim().length() > 0)
					&& (url != null && url.trim().length() > 0)) {
				RecommendationURL recommendationURL = new RecommendationURL();
				recommendationURL.setDisplayName(displayName.trim());
				recommendationURL.setUrl(url.trim());
				recommendationURLList.add(recommendationURL);
			}
		}
		CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
		CharacterData defaultCd = defProv
				.createDefaultCharacterData(DefaultCharacterDataVersion.V3);
		List<RecommendationURL> defaultRecommendationURLList = defaultCd.getRecommendationURLList();
		if (defaultRecommendationURLList != null && defaultRecommendationURLList.equals(recommendationURLList)) {
			// デフォルトのお勧めリストと内容が同じの場合は、明示的にリストを設定しない.
			recommendationURLList = null;
		}
		cd.setRecommendationURLList(recommendationURLList);
		
		return cd;
	}
	
	/**
	 * 結果を取得する. キャンセルされた場合はnullが返される.<br>
	 * 
	 * @return キャラクターデータ、またはnull
	 */
	public CharacterData getResult() {
		return result;
	}
	
}

/**
 * 編集用カラーグループ
 * 
 * @author seraphy
 */
class ColorGroupsTableRow {

	private final String id;
	
	private final boolean enabled;
	
	private String localizedName;

	public static final ColorGroupsTableRow NA = new ColorGroupsTableRow("n/a", "", false);
	
	
	public ColorGroupsTableRow(final String id, final String localizedName) {
		this(id, localizedName, true);
	}
	
	public static ColorGroupsTableRow valueOf(ColorGroup colorGroup) {
		if (colorGroup == null || !colorGroup.isEnabled()) {
			return NA;
		}
		return new ColorGroupsTableRow(colorGroup.getId(), colorGroup.getLocalizedName(), true);
	}
	
	public ColorGroup convert() {
		if (!isEnabled()) {
			return ColorGroup.NA;
		}
		return new ColorGroup(getId(), getLocalizedName());
	}
	
	private ColorGroupsTableRow(final String id, final String localizedName, final boolean enabled) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		this.id = id.trim();
		this.localizedName = (localizedName == null || localizedName.trim().length() == 0) ? id : localizedName;
		this.enabled = enabled;
	}
	
	public void setLocalizedName(String localizedName) {
		if (localizedName == null || localizedName.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		if (!enabled) {
			throw new UnsupportedOperationException("unmodified object.");
		}
		this.localizedName = localizedName;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public String getId() {
		return id;
	}
	
	public String getLocalizedName() {
		return localizedName;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ColorGroupsTableRow) {
			ColorGroupsTableRow o = (ColorGroupsTableRow) obj;
			return id.equals(o.getId());
		}
		return false;
	}
	
	public static boolean equals(ColorGroupsTableRow v1, ColorGroupsTableRow v2) {
		if (v1 == v2) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.equals(v2);
	}
	
	@Override
	public String toString() {
		return getLocalizedName();
	}

}


/**
 * カラーグループのテーブル編集モデル
 * 
 * @author seraphy
 */
class ColorGroupsTableModel extends AbstractTableModelWithComboBoxModel<ColorGroupsTableRow> {

	private static final long serialVersionUID = 2952439955567262351L;

	private static final String[] colorGroupColumnNames;
	
	private static final Logger logger = Logger.getLogger(ColorGroupsTableModel.class.getName());

	static {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(ProfileEditDialog.STRINGS_RESOURCE);
		colorGroupColumnNames = new String[] {
				strings.getProperty("colorgroup.column.colorgroupname"),
		};
	}
	
	private int serialCounter = 1;
	
	public int getColumnCount() {
		return colorGroupColumnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return colorGroupColumnNames[column];
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		ColorGroupsTableRow colorGroup = elements.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return colorGroup.getLocalizedName();
		}
		return "****";
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		ColorGroupsTableRow colorGroup = elements.get(rowIndex);
		try {
			switch (columnIndex) {
			case 0:
				String localizedName = (String) aValue;
				if (localizedName != null && localizedName.trim().length() > 0) {
					colorGroup.setLocalizedName(localizedName.trim());
				}
				break;
			default:
				return;
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			
		} catch (Exception ex) {
			logger.log(Level.FINE, "value set failed. (" + rowIndex + ", " + columnIndex + "): " + aValue, ex);
			// 無視する
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return isEditable();
	}

	public void addNewColorGroup() {
		String id = "cg" + UUID.randomUUID().toString();
		String localizedName = "ColorGroup" + (serialCounter++);
		ColorGroupsTableRow colorGroup = new ColorGroupsTableRow(id, localizedName);
		addRow(colorGroup);
	}
}

/**
 * カラーグループ用のコンボボックスモデルに対して最初のアイテムとしてN/Aを常に追加するモデルに変換するラップクラス.<br>
 * 
 * @author seraphy
 */
class FirstItemInjectionComboBoxModelWrapper<T> implements ComboBoxModel {
	
	private ComboBoxModel parent;
	
	private T selectedItem;
	
	private T firstItem;
	
	private LinkedList<ListDataListener> listDataListeners = new LinkedList<ListDataListener>();

	public FirstItemInjectionComboBoxModelWrapper(ComboBoxModel parent, T firstItem) {
		if (parent == null || firstItem == null) {
			throw new IllegalArgumentException();
		}
		
		this.parent = parent;
		this.firstItem = firstItem;

		parent.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent e) {
				fireListUpdated(convertRowIndex(e));
			}
			public void intervalAdded(ListDataEvent e) {
				fireListAdded(convertRowIndex(e));
			}
			public void intervalRemoved(ListDataEvent e) {
				fireListRemoved(convertRowIndex(e));
			}
			/**
			 * 親コンボボックスモデルのインデックスを+1したイベントに変換する.
			 * 
			 * @param e
			 *            元イベント
			 * @return インデックス変換後のイベント
			 */
			protected ListDataEvent convertRowIndex(ListDataEvent e) {
				return new ListDataEvent(e.getSource(), e.getType(), e
						.getIndex0() + 1, e.getIndex1() + 1);
			}
		});
	}

	protected void fireListUpdated(ListDataEvent e) {
		for (ListDataListener listener : listDataListeners) {
			listener.contentsChanged(e);
		}
	}

	protected void fireListAdded(ListDataEvent e) {
		for (ListDataListener listener : listDataListeners) {
			listener.intervalAdded(e);
		}
	}

	protected void fireListRemoved(ListDataEvent e) {
		for (ListDataListener listener : listDataListeners) {
			listener.intervalRemoved(e);
		}
	}
	
	public Object getSelectedItem() {
		return selectedItem;
	}

	@SuppressWarnings("unchecked")
	public void setSelectedItem(Object anItem) {
		selectedItem = (T) anItem;
		if (!firstItem.equals(anItem)) {
			parent.setSelectedItem(anItem);
		}
	}

	public void addListDataListener(ListDataListener l) {
		if (l != null) {
			listDataListeners.add(l);
		}
	}

	public void removeListDataListener(ListDataListener l) {
		if (l != null) {
			listDataListeners.remove(l);
		}
	}

	public Object getElementAt(int index) {
		if (index == 0) {
			return firstItem;
		}
		return parent.getElementAt(index - 1);
	}

	public int getSize() {
		return parent.getSize() + 1;
	}
}

class CategoriesTableRow implements Comparable<CategoriesTableRow> {

	/**
	 * 順序
	 */
	private int order;
	
	/**
	 * カテゴリ識別名
	 */
	private String categoryId;
	
	/**
	 * カテゴリ表示名
	 */
	private String localizedCategoryName;

	/**
	 * 複数選択可能?
	 */
	private boolean multipleSelectable;
	
	/**
	 * 表示行数
	 */
	private int visibleRows;
	
	/**
	 * レイヤー情報
	 */
	private ArrayList<Layer> layers = new ArrayList<Layer>();
	
	/**
	 * カテゴリを構築する.<br>
	 * 
	 * @param categoryId
	 *            カテゴリ識別名
	 * @param localizedCategoryName
	 *            カテゴリ表示名
	 * @param multipleSelectable
	 *            複数選択可能?
	 * @param layers
	 *            レイヤー情報の配列
	 */
	public CategoriesTableRow(final int order, final String categoryId, String localizedCategoryName,
			boolean multipleSelectable, int visibleRows, Layer[] layers) {
		if (categoryId == null || categoryId.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		if (layers == null) {
			layers = new Layer[0];
		}
		if (localizedCategoryName == null || localizedCategoryName.trim().length() == 0) {
			localizedCategoryName = categoryId;
		}
		this.order = order;
		this.categoryId = categoryId.trim();
		this.localizedCategoryName = localizedCategoryName.trim();
		this.multipleSelectable = multipleSelectable;
		this.layers.addAll(Arrays.asList(layers));
		this.visibleRows = visibleRows;
	}
	
	public CategoriesTableRow(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		this.order = partsCategory.getOrder();
		this.categoryId = partsCategory.getCategoryId();
		this.localizedCategoryName = partsCategory.getLocalizedCategoryName();
		this.multipleSelectable = partsCategory.isMultipleSelectable();
		this.layers.addAll(partsCategory.getLayers());
		this.visibleRows = partsCategory.getVisibleRows();
	}
	
	public PartsCategory convert() {
		return new PartsCategory(order, categoryId, localizedCategoryName,
				multipleSelectable, visibleRows, layers
						.toArray(new Layer[layers.size()]));
	}
	
	public int compareTo(CategoriesTableRow o) {
		if (o == this) {
			return 0;
		}
		int ret = order - o.order;
		if (ret == 0) {
			ret = localizedCategoryName.compareTo(o.localizedCategoryName);
		}
		if (ret == 0) {
			ret = categoryId.compareTo(o.categoryId);
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof CategoriesTableRow) {
			return categoryId.equals(((CategoriesTableRow) obj).getCategoryId());
		}
		return false;
	}
	
	public static boolean equals(CategoriesTableRow o1, CategoriesTableRow o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}
	
	/**
	 * 定義順を取得する
	 * 
	 * @return 定義順
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * 定義順を設定する
	 * 
	 * @param order
	 *            定義順
	 */
	public void setOrder(int order) {
		this.order = order;
	}
	
	/**
	 * 複数選択可能であるか?
	 * 
	 * @return 複数選択可能であるか?
	 */
	public boolean isMultipleSelectable() {
		return multipleSelectable;
	}

	/**
	 * 複数選択可能であるか設定する
	 * 
	 * @param multipleSelectable
	 *            複数選択可能であればtrue
	 */
	public void setMultipleSelectable(boolean multipleSelectable) {
		this.multipleSelectable = multipleSelectable;
	}

	/**
	 * 表示行数を取得する.
	 * 
	 * @return 表示行数
	 */
	public int getVisibleRows() {
		return visibleRows;
	}

	/**
	 * 表示行数を設定する
	 * 
	 * @param visibleRows
	 *            表示行数
	 */
	public void setVisibleRows(int visibleRows) {
		this.visibleRows = visibleRows;
	}
	
	/**
	 * このカテゴリに指定したレイヤーが含まれるか検証する.
	 * 
	 * @param layer
	 *            レイヤー
	 * @return 含まれる場合はtrue、含まれない場合はfalse
	 */
	public boolean hasLayer(Layer layer) {
		if (layer != null) {
			for (Layer memberLayer : layers) {
				if (Layer.equals(memberLayer, layer)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * レイヤー情報
	 * 
	 * @return レイヤー情報
	 */
	public Collection<Layer> getLayers() {
		return Collections.unmodifiableCollection(layers);
	}
	
	/**
	 * レイヤー情報
	 * 
	 * @param layers
	 */
	public void setLayers(Collection<Layer> layers) {
		this.layers.clear();
		if (layers != null) {
			this.layers.addAll(layers);
		}
	}
	
	/**
	 * レイヤーを取得する.<br>
	 * 該当するレイヤーがなければnull
	 * 
	 * @param layerId
	 *            レイヤー名
	 * @return レイヤーもしくはnull
	 */
	public Layer getLayer(String layerId) {
		if (layerId == null) {
			return null;
		}
		for (Layer layer : layers) {
			if (layer.getId().equals(layerId)) {
				return layer;
			}
		}
		return null;
	}
	
	/**
	 * カテゴリ識別名を取得する.
	 * 
	 * @return カテゴリ識別名
	 */
	public String getCategoryId() {
		return categoryId;
	}
	
	/**
	 * カテゴリ表示名を取得する.
	 * 
	 * @return カテゴリ表示名
	 */
	public String getLocalizedCategoryName() {
		return this.localizedCategoryName;
	}
	
	/**
	 * カテゴリ表示名を設定する.
	 * 
	 * @param localizedCategoryName
	 *            カテゴリ表示名
	 */
	public void setLocalizedCategoryName(String localizedCategoryName) {
		if (localizedCategoryName == null || localizedCategoryName.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		this.localizedCategoryName = localizedCategoryName.trim();
	}
	
	@Override
	public int hashCode() {
		return this.categoryId.hashCode();
	}
	
	@Override
	public String toString() {
		return getLocalizedCategoryName();
	}

}

/**
 * カテゴリのテーブル編集モデル.
 * 
 * @author seraphy
 * 
 */
class CategoriesTableModel extends AbstractTableModelWithComboBoxModel<CategoriesTableRow> {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(CategoriesTableModel.class.getName());

	public interface UsedCategoryDetector {
		
		List<LayersTableRow> getLayers(CategoriesTableRow partsCategory);
		
	}
	
	private static final String[] categoriesColumnName;
	
	private static final int[] categoriesColumnWidths;
	
	static {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(ProfileEditDialog.STRINGS_RESOURCE);
		categoriesColumnName = new String[] {
				strings.getProperty("categories.column.categoryname"),
				strings.getProperty("categories.column.multipleselectable"),
				strings.getProperty("categories.column.displayrowcount"),
				strings.getProperty("categories.column.usedlayers"),
		};
		categoriesColumnWidths = new int[] {
				Integer.parseInt(strings.getProperty("categories.column.categoryname.width")),
				Integer.parseInt(strings.getProperty("categories.column.multipleselectable.width")),
				Integer.parseInt(strings.getProperty("categories.column.displayrowcount.width")),
				Integer.parseInt(strings.getProperty("categories.column.usedlayers.width")),
		};
	}
	
	private int serialCounter = 1;

	private UsedCategoryDetector usedCategoryDetector;
	
	public CategoriesTableModel() {
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < categoriesColumnWidths.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(categoriesColumnWidths[idx]);
		}
	}

	public void setUsedCategoryDetector(
			UsedCategoryDetector usedCategoryDetector) {
		this.usedCategoryDetector = usedCategoryDetector;
	}
	
	public UsedCategoryDetector getUsedCategoryDetector() {
		return usedCategoryDetector;
	}
	
	public void addCategory() {
		String id = "cat" + UUID.randomUUID().toString();
		String name = "Category" + (serialCounter++);
		CategoriesTableRow partsCategory = new CategoriesTableRow(
				serialCounter, id, name, false, 10, null);
		addRow(partsCategory);
	}
	
	/**
	 * 定義順を振り直す.
	 */
	public void reorder() {
		int mx = elements.size();
		for (int idx = 0; idx < mx; idx++) {
			CategoriesTableRow partsCategory = elements.get(idx);
			partsCategory.setOrder(idx + 1);
		}
		fireTableDataChanged();
	}
	
	@Override
	public int moveDown(int rowIndex) {
		int ret = super.moveDown(rowIndex);
		reorder();
		return ret;
	}
	
	@Override
	public int moveUp(int rowIndex) {
		int ret = super.moveUp(rowIndex);
		reorder();
		return ret;
	}
	
	@Override
	public String getColumnName(int column) {
		return categoriesColumnName[column];
	}
	
	public int getColumnCount() {
		return categoriesColumnName.length;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		CategoriesTableRow partsCategory = elements.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return partsCategory.getLocalizedCategoryName();
		case 1:
			return Boolean.valueOf(partsCategory.isMultipleSelectable());
		case 2:
			return partsCategory.getVisibleRows();
		case 3:
			StringBuilder layerNames = new StringBuilder();
			List<LayersTableRow> layers = null;
			if (usedCategoryDetector != null) {
				layers = usedCategoryDetector.getLayers(partsCategory);
			}
			if (layers != null) {
				for (LayersTableRow layer : layers) {
					if (layerNames.length() > 0) {
						layerNames.append(", ");
					}
					layerNames.append(layer.getLayerName());
				}
			}
			return layerNames.toString();
		default:
			return "***";
		}
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		CategoriesTableRow partsCategory = elements.get(rowIndex);
		try {
			switch (columnIndex) {
			case 0:
				partsCategory.setLocalizedCategoryName((String) aValue);
				break;
			case 1:
				partsCategory.setMultipleSelectable(((Boolean) aValue).booleanValue());
				break;
			case 2:
				partsCategory.setVisibleRows(((Number) aValue).intValue());
				break;
			default:
				return;
			}
			fireTableCellUpdated(rowIndex, columnIndex);

		} catch (RuntimeException ex) {
			logger.log(Level.FINE, "value set failed. (" + rowIndex + ", " + columnIndex + "): " + aValue, ex);
			// 無視する.
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 1) {
			return Boolean.class;
		}
		if (columnIndex == 2) {
			return Integer.class;
		}
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex >= categoriesColumnName.length - 1) {
			return false;
		}
		return isEditable();
	}
}

/**
 * レイヤーのテーブル編集モデル
 * 
 * @author seraphy
 */
class LayersTableModel extends AbstractTableModelWithComboBoxModel<LayersTableRow> {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(LayersTableModel.class.getName());

	private static final String[] layerColumnNames;
	
	private static final int[] layersColumnWidths;
	
	private enum Columns {
		/**
		 * レイヤー名
		 */
		LAYER_NAME("layers.column.layername", String.class) {
			@Override
			public Object getValue(LayersTableRow layer) {
				return layer.getLayerName();
			}

			@Override
			public boolean setValue(LayersTableRow layer, Object aValue) {
				layer.setLayerName((String) aValue);
				return true;
			}
		},

		/**
		 * カテゴリ
		 */
		CATEGORY("layers.column.category", CategoriesTableRow.class) {
			@Override
			public Object getValue(LayersTableRow layer) {
				return layer.getPartsCategory();
			}

			@Override
			public boolean setValue(LayersTableRow layer, Object aValue) {
				layer.setPartsCategory((CategoriesTableRow) aValue);
				return true;
			}
		},

		/**
		 * カラーグループ
		 */
		COLOR_GROUP("layers.column.colorgroup", ColorGroupsTableRow.class) {
			@Override
			public Object getValue(LayersTableRow layer) {
				return layer.getColorGroup();
			}

			@Override
			public boolean setValue(LayersTableRow layer, Object aValue) {
				layer.setColorGroup((ColorGroupsTableRow) aValue);
				return true;
			}
		},

		/**
		 * 順序
		 */
		ORDER("layers.column.order", Integer.class) {
			@Override
			public Object getValue(LayersTableRow layer) {
				return layer.getOrder();
			}

			@Override
			public boolean setValue(LayersTableRow layer, Object aValue) {
				layer.setOrder(((Number) aValue).intValue());
				return true;
			}
		},

		/**
		 * カラーモデル
		 */
		COLOR_MODEL("layers.column.colorModel", ColorModels.class) {
			@Override
			public Object getValue(LayersTableRow layer) {
				return layer.getColorModel();
			}

			@Override
			public boolean setValue(LayersTableRow layer, Object aValue) {
				layer.setColorModel(((ColorModels) aValue));
				return true;
			}
		},

		/**
		 * ディレクトリ
		 */
		DIRECTORY("layers.column.directory", String.class) {
			@Override
			public Object getValue(LayersTableRow layer) {
				return layer.getDir();
			}

			@Override
			public boolean setValue(LayersTableRow layer, Object aValue) {
				layer.setDir((String) aValue);
				return true;
			}
		};

		private final String resourceKey;

		private final Class<?> typ;

		Columns(String resourceKey, Class<?> typ) {
			this.resourceKey = resourceKey;
			this.typ = typ;
		}

		public String getResourceKey() {
			return resourceKey;
		}

		public Class<?> getColumnClass() {
			return typ;
		}

		public abstract Object getValue(LayersTableRow layer);

		public abstract boolean setValue(LayersTableRow layer, Object aValue);
	}
	
	static {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(ProfileEditDialog.STRINGS_RESOURCE);
		int columnsLen = Columns.values().length;
		layerColumnNames = new String[columnsLen];
		layersColumnWidths = new int[columnsLen];
		for (Columns column : Columns.values()) {
			try {
				layerColumnNames[column.ordinal()] = strings.getProperty(column
						.getResourceKey());
				layersColumnWidths[column.ordinal()] = Integer.parseInt(strings
						.getProperty(column.getResourceKey() + ".width"));

			} catch (RuntimeException ex) {
				logger.log(Level.SEVERE, "resource not found. related=" + column, ex);
				throw ex;
			}
		}
	}

	private int serialCounter = 1;

	public LayersTableModel() {
		super();
	}

	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < layersColumnWidths.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(layersColumnWidths[idx]);
		}
	}

	public void addNewLayer() {
		LayersTableRow layer = new LayersTableRow();
		String layerId = "lay" + UUID.randomUUID().toString();
		String layerName = "Layer" + (serialCounter++);
		layer.setLayerId(layerId);
		layer.setLayerName(layerName);
		addRow(layer);
	}

	public int getColumnCount() {
		return layerColumnNames.length;
	}

	@Override
	public String getColumnName(int column) {
		return layerColumnNames[column];
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		LayersTableRow layer = elements.get(rowIndex);
		Columns column = Columns.values()[columnIndex];
		return column.getValue(layer);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		LayersTableRow layer = elements.get(rowIndex);
		try {
			Columns column = Columns.values()[columnIndex];
			if (column.setValue(layer, aValue)) {
				fireTableCellUpdated(rowIndex, columnIndex);
			}

		} catch (Exception ex) {
			logger.log(Level.FINE, "value set failed. (" + rowIndex + ", " + columnIndex + "): " + aValue, ex);
			// 無視する.
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Columns column = Columns.values()[columnIndex];
		return column.getColumnClass();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return isEditable();
	}

	public void sort() {
		Collections.sort(elements, new Comparator<LayersTableRow>() {
			public int compare(LayersTableRow o1, LayersTableRow o2) {
				int ret;

				CategoriesTableRow p1 = o1.getPartsCategory();
				CategoriesTableRow p2 = o2.getPartsCategory();
				
				if (p1 == p2) {
					ret = 0;
				} else if (p1 != null && p2 != null) {
					ret = p1.compareTo(p2);
				} else if (p1 == null) {
					ret = -1;
				} else {
					ret = 1;
				}

				if (ret == 0) {
					ret = o1.getOrder() - o2.getOrder();
				}

				if (ret == 0) {
					ret = o1.getLayerId().compareTo(o2.getLayerId());
				}
				return ret;
			}
		});
		fireTableDataChanged();
	}

	protected boolean isUsed(ColorGroupsTableRow colorGroup) {
		if (colorGroup != null) {
			for (LayersTableRow layer : elements) {
				if (ColorGroupsTableRow.equals(layer.getColorGroup(), colorGroup)) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean isUsed(CategoriesTableRow partsCategory) {
		if (partsCategory != null) {
			for (LayersTableRow layer : elements) {
				if (CategoriesTableRow.equals(layer.getPartsCategory(), partsCategory)) {
					return true;
				}
			}
		}
		return false;
	}
}

/**
 * レイヤーのテーブル編集モデルで使うレイヤー編集クラス
 * 
 * @author seraphy
 */
class LayersTableRow {

	private String layerId;
	
	private String layerName;
	
	private CategoriesTableRow partsCategory;
	
	private ColorGroupsTableRow colorGroup = ColorGroupsTableRow.NA;
	
	private int order;
	
	private String dir;
	
	private ColorModels colorModel = ColorModels.DEFAULT;

	public LayersTableRow() {
		super();
	}
	
	public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		if (layerId == null || layerId.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		this.layerId = layerId.trim();
	}

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		if (layerName == null || layerName.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		this.layerName = layerName.trim();
	}

	public CategoriesTableRow getPartsCategory() {
		return partsCategory;
	}

	public void setPartsCategory(CategoriesTableRow partsCategory) {
		this.partsCategory = partsCategory;
	}

	public ColorGroupsTableRow getColorGroup() {
		return colorGroup;
	}

	public void setColorGroup(ColorGroupsTableRow colorGroup) {
		if (colorGroup == null) {
			throw new IllegalArgumentException();
		}
		this.colorGroup = colorGroup;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		if (dir == null || dir.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		dir = dir.trim();
		
		if (dir.indexOf("/") >= 0 || dir.indexOf("\\") >= 0 || dir.indexOf("..") >= 0 || dir.endsWith(".")) {
			throw new IllegalArgumentException("not simple name: " + dir);
		}
		
		this.dir = dir;
	}
	
	public ColorModels getColorModel() {
		return colorModel;
	}

	public void setColorModel(ColorModels colorModel) {
		this.colorModel = colorModel;
	}

	public boolean isValid() {
		return layerName != null && layerName.trim().length() > 0
				&& dir != null && dir.trim().length() > 0 && partsCategory != null && colorGroup != null;
	}
	
	public Layer toLayer() {
		if (!isValid()) {
			return null;
		}
		ColorGroup colorGroup = getColorGroup().convert();
		return new Layer(
				getLayerId(),
				getLayerName(),
				getOrder(),
				colorGroup,
				colorGroup.isEnabled(),
				getDir(),
				getColorModel().name());
	}
}


/**
 * パーツセットのテーブルの行編集モデル
 * 
 * @author seraphy
 */
class PresetsTableRow {
	
	private PartsSet partsSet;
	
	public PresetsTableRow(PartsSet partsSet) {
		if (partsSet == null) {
			throw new IllegalArgumentException();
		}
		this.partsSet = partsSet.clone();
	}
	
	public String getPartsSetId() {
		return partsSet.getPartsSetId();
	}
	
	public String getLocalizedName() {
		return partsSet.getLocalizedName();
	}
	
	public void setLocalizedName(String localizedName) {
		partsSet.setLocalizedName(localizedName);
	}
	
	public boolean isPresetParts() {
		return partsSet.isPresetParts();
	}
	
	public void setPresetParts(boolean checked) {
		partsSet.setPresetParts(checked);
	}
	
	public PartsSet convert() {
		return partsSet.clone();
	}
	
}

/**
 * パーツセットのテーブル編集モデル
 * 
 * @author seraphy
 */
class PartssetsTableModel extends AbstractTableModelWithComboBoxModel<PresetsTableRow> {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(PartssetsTableModel.class.getName());

	private static final String[] partssetsColumnNames;
	
	private static final int[] partssetsColumnWidths;
	
	private String defaultPartsSetId;
	
	static {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(ProfileEditDialog.STRINGS_RESOURCE);
		partssetsColumnNames = new String[] {
				strings.getProperty("partssets.column.default"),
				strings.getProperty("partssets.column.preset"),
				strings.getProperty("partssets.column.partssetname"),
				strings.getProperty("partssets.column.usedpartsname"),
		};
		partssetsColumnWidths = new int[] {
				Integer.parseInt(strings.getProperty("partssets.column.default.width")),
				Integer.parseInt(strings.getProperty("partssets.column.preset.width")),
				Integer.parseInt(strings.getProperty("partssets.column.partssetname.width")),
				Integer.parseInt(strings.getProperty("partssets.column.usedpartsname.width")),
		};
	}

	public PartssetsTableModel() {
	}
	
	public void setDefaultPartsSetId(String defaultPartsSetId) {
		this.defaultPartsSetId = defaultPartsSetId;
	}
	
	public String getDefaultPartsSetId() {
		return defaultPartsSetId;
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < partssetsColumnWidths.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(partssetsColumnWidths[idx]);
		}
	}
	
	public int getColumnCount() {
		return partssetsColumnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return partssetsColumnNames[column];
	}
	
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		PresetsTableRow rowModel = elements.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return rowModel.getPartsSetId().equals(defaultPartsSetId);
		case 1:
			return Boolean.valueOf(rowModel.isPresetParts());
		case 2:
			return rowModel.getLocalizedName();
		case 3:
			return getUsedParts(rowModel);
		default:
			return null;
		}
	}
	
	private String getUsedParts(PresetsTableRow rowModel) {
		StringBuilder buf = new StringBuilder();
		PartsSet partsSet = rowModel.convert();
		ArrayList<PartsCategory> categories = new ArrayList<PartsCategory>(partsSet.keySet());
		Collections.sort(categories);
		for (PartsCategory category : categories) {
			if (buf.length() > 0) {
				buf.append(", ");
			}
			buf.append("[" + category.getLocalizedCategoryName() + "] ");
			List<PartsIdentifier> partsIdentifiers = partsSet.get(category);
			if (partsIdentifiers.isEmpty()) {
				buf.append("empty");
			} else {
				int mx = partsIdentifiers.size();
				for (int idx = 0; idx < mx; idx++) {
					if (idx != 0) {
						buf.append(", ");
					}
					buf.append(partsIdentifiers.get(idx).getLocalizedPartsName());
				}
			}
		}
		return buf.toString();
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		PresetsTableRow rowModel = elements.get(rowIndex);
		try {
			switch (columnIndex) {
			case 0:
				if (((Boolean) aValue).booleanValue()) {
						// デフォルトのパーツセットに指定した場合、プリセットもOnとなる。
					rowModel.setPresetParts(true);
					defaultPartsSetId = rowModel.getPartsSetId();
					fireTableDataChanged();
					return;
				}
				break;
			case 1:
				if (((Boolean) aValue).booleanValue()) {
					rowModel.setPresetParts(true);
				} else {
						// デフォルトのパーツセットをプリセットから外した場合、
						// デフォルトのパーツセットは未設定となる.
					rowModel.setPresetParts(false);
					if (rowModel.getPartsSetId().equals(defaultPartsSetId)) {
						defaultPartsSetId = null;
						fireTableRowsUpdated(rowIndex, rowIndex);
						return;
					}
				}
				break;
			case 2:
				String localizedName = (String) aValue;
				if (localizedName != null && localizedName.trim().length() > 0) {
					rowModel.setLocalizedName(localizedName.trim());
				}
				break;
			case 3:
				return;
			default:
				return;
			}
			fireTableCellUpdated(rowIndex, columnIndex);

		} catch (Exception ex) {
			logger.log(Level.FINE, "value set failed. (" + rowIndex + ", " + columnIndex + "): " + aValue, ex);
			// 無視する.
		}
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
		switch (columnIndex ) {
		case 0:
			return isEditable();
		case 1:
			return isEditable();
		case 2:
			return isEditable();
		case 3:
			return false;
		default:
		}
		return false;
	}
}


/**
 * お勧めリンクのテーブルの行編集モデル
 * 
 * @author seraphy
 */
class RecommendationTableRow {
	
	private RecommendationURL recommendationURL;
	
	public RecommendationTableRow(RecommendationURL recommendationURL) {
		if (recommendationURL == null) {
			throw new IllegalArgumentException();
		}
		this.recommendationURL = recommendationURL.clone();
	}
	
	public String getLocalizedName() {
		return recommendationURL.getDisplayName();
	}
	
	public void setLocalizedName(String localizedName) {
		recommendationURL.setDisplayName(localizedName);
	}
	
	public String getURL() {
		return recommendationURL.getUrl();
	}
	
	public void setURL(String url) {
		recommendationURL.setUrl(url);
	}
	
	public RecommendationURL convert() {
		return recommendationURL.clone();
	}
}

/**
 * お勧めリンクのテーブル編集モデル
 * 
 * @author seraphy
 */
class RecommendationTableModel extends AbstractTableModelWithComboBoxModel<RecommendationTableRow> {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(PartssetsTableModel.class.getName());

	private static final String[] partssetsColumnNames;
	
	private static final int[] partssetsColumnWidths;
	
	static {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(ProfileEditDialog.STRINGS_RESOURCE);
		partssetsColumnNames = new String[] {
				strings.getProperty("recommendations.column.displayName"),
				strings.getProperty("recommendations.column.url"),
		};
		partssetsColumnWidths = new int[] {
				Integer.parseInt(strings.getProperty("recommendations.column.displayName.width")),
				Integer.parseInt(strings.getProperty("recommendations.column.url.width")),
		};
	}

	public RecommendationTableModel() {
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < partssetsColumnWidths.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(partssetsColumnWidths[idx]);
		}
	}
	
	public void addNew() {
		addRow(new RecommendationTableRow(new RecommendationURL()));
	}
	
	public int getColumnCount() {
		return partssetsColumnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return partssetsColumnNames[column];
	}
	
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		RecommendationTableRow rowModel = elements.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return rowModel.getLocalizedName();
		case 1:
			return rowModel.getURL();
		default:
			return null;
		}
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		RecommendationTableRow rowModel = elements.get(rowIndex);
		try {
			switch (columnIndex) {
			case 0:
				if (aValue != null && ((String) aValue).trim().length() > 0) {
					rowModel.setLocalizedName((String) aValue);
				}
				break;
			case 1:
				if (aValue != null && ((String) aValue).trim().length() > 0) {
					rowModel.setURL((String) aValue);
				}
				break;
			default:
				return;
			}
			fireTableCellUpdated(rowIndex, columnIndex);

		} catch (Exception ex) {
			logger.log(Level.FINE, "value set failed. (" + rowIndex + ", " + columnIndex + "): " + aValue, ex);
			// 無視する.
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return String.class;
		default:
		}
		return String.class;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch (columnIndex ) {
		case 0:
			return isEditable();
		case 1:
			return isEditable();
		default:
		}
		return false;
	}
}
