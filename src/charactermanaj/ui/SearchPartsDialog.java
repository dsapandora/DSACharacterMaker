package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumnModel;

import charactermanaj.model.AppConfig;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.PartsSpecResolver;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.ui.model.PartsSelectionManager;
import charactermanaj.util.LocalizedResourcePropertyLoader;

public class SearchPartsDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	
	private static final WeakHashMap<SearchPartsDialog, Object> ALL_DIALOGS
		= new WeakHashMap<SearchPartsDialog, Object>();
	
	
	protected static final String STRINGS_RESOURCE = "languages/searchpartsdialog";

	private PartsSpecResolver partsSpecResolver;
	
	private PartsSelectionManager partsSelectionManager;
	
	private JTable searchPartsTable;
	
	
	private SearchPartsTableModel searchPartsTableModel;
	
	private JTextField txtPartsName;
	
	private JComboBox cmbAuthors;
	
	private JComboBox cmbCategories;
	
	
	public static SearchPartsDialog[] getDialogs() {
		return ALL_DIALOGS.keySet().toArray(new SearchPartsDialog[ALL_DIALOGS.size()]);
	}
	
	
	public SearchPartsDialog(JFrame parent,
			PartsSpecResolver partsSpecResolver,
			PartsSelectionManager partsSelectionManager) {
		super(parent, false);
		
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException();
		}

		this.partsSpecResolver = partsSpecResolver;
		this.partsSelectionManager = partsSelectionManager;
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
		
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		setTitle(strings.getProperty("title"));
		
		// モデル
		searchPartsTableModel = new SearchPartsTableModel();
		
		
		// 検索条件パネル
		
		JPanel searchCondPanel = new JPanel();
		GridBagLayout searchCondPanelLayout = new GridBagLayout();
		searchCondPanel.setLayout(searchCondPanelLayout);
		GridBagConstraints gbc = new GridBagConstraints();
		
		searchCondPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory
						.createTitledBorder(strings.getProperty("search.condition"))));
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		searchCondPanel.add(new JLabel(strings.getProperty("partsname"), JLabel.RIGHT), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		txtPartsName = new JTextField();
		searchCondPanel.add(txtPartsName, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		searchCondPanel.add(new JLabel(strings.getProperty("author"), JLabel.RIGHT), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1.;
		ArrayList<String> authors = new ArrayList<String>();
		authors.add("");
		authors.addAll(getAuthors(partsSpecResolver));
		cmbAuthors = new JComboBox(authors.toArray(new String[authors.size()]));
		searchCondPanel.add(cmbAuthors, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		searchCondPanel.add(new JLabel(strings.getProperty("partscategory"), JLabel.RIGHT), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.weightx = 1.;
		ArrayList<PartsCategory> categories = new ArrayList<PartsCategory>();
		categories.add(null);
		categories.addAll(partsSpecResolver.getPartsCategories());
		cmbCategories = new JComboBox(categories.toArray(new PartsCategory[categories.size()]));
		searchCondPanel.add(cmbCategories, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 1.;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JButton btnClear = new JButton(new AbstractAction(strings.getProperty("clear")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				txtPartsName.setText("");
			}
		});
		searchCondPanel.add(btnClear, gbc);
		
		// 検索結果
		JPanel searchResult = new JPanel(new BorderLayout());
		searchResult.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 5, 0, 5), BorderFactory
						.createTitledBorder(strings.getProperty("results"))));
		searchPartsTable = new JTable(searchPartsTableModel);
		searchPartsTable.setShowGrid(true);
		searchPartsTable.setGridColor(AppConfig.getInstance().getGridColor());
		JScrollPane searchPartsTableSP = new JScrollPane(searchPartsTable);
		searchPartsTableModel.adjustColumnModel(searchPartsTable.getColumnModel());
		searchResult.add(searchPartsTableSP, BorderLayout.CENTER);
		searchPartsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// ダブルクリック
					// 正確に2回
					onSelect();
				}
			}
		});
		
		// テーブルのキーイベント
		ActionMap tblAm = searchPartsTable.getActionMap();
		InputMap tblIm = searchPartsTable.getInputMap();
		tblIm.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "onSelect");
		tblAm.put("onSelect", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSelect();
			}
		});
		
		JPanel btnPanel = new JPanel();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
		GridBagLayout btnPanelLayout = new GridBagLayout();
		btnPanel.setLayout(btnPanelLayout);

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

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		JButton btnSelect = new JButton(new AbstractAction(strings.getProperty("select")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSelect();
			}
		});
		btnPanel.add(btnSelect, gbc);
		searchResult.add(btnPanel, BorderLayout.SOUTH);
		
		
		// 検索条件の入力が変更されたことを検知するリスナの登録
		
		txtPartsName.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				onChangeCondition();
			}
			public void insertUpdate(DocumentEvent e) {
				onChangeCondition();
			}
			public void changedUpdate(DocumentEvent e) {
				onChangeCondition();
			}
		});
		
		ActionListener changeListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onChangeCondition();
			}
		};
		cmbAuthors.addActionListener(changeListener);
		cmbCategories.addActionListener(changeListener);
		
		// ESCキーとCTRL-Wで閉じる.
		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeSearchDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeSearchDialog");
		am.put("closeSearchDialog", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		});
		
		// ウィンドウがアクティブになったときに検索フィールドにフォーカスをあてる
		addWindowFocusListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		    	txtPartsName.requestFocusInWindow();
		    }
		});

		// 画面の設定
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(searchCondPanel, BorderLayout.NORTH);
		contentPane.add(searchResult, BorderLayout.CENTER);
		
		setSize(250, 300);
		setLocationRelativeTo(parent);
		
		// ダイアログの登録
		ALL_DIALOGS.put(this, null);
	}
	
	public List<String> getAuthors(PartsSpecResolver partsSpecResolver) {
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException();
		}
		HashSet<String> authorsSet = new HashSet<String>();
		for (PartsCategory category : partsSpecResolver.getPartsCategories()) {
			for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsSpecResolver.getPartsSpecMap(category).entrySet()) {
				PartsSpec partsSpec = entry.getValue();
				String author = partsSpec.getAuthor();
				if (author != null) {
					authorsSet.add(author);
				}
			}
		}
		
		ArrayList<String> authors = new ArrayList<String>(authorsSet); 
		Collections.sort(authors);
		return authors;
	}
	
	protected void onClose() {
		dispose();
	}
	
	/**
	 * 「選択」ボタンまたはテーブルのダブルクリックのハンドラ.<br>
	 * 選択されている行のパーツ識別子をもとに、パーツにフォーカスをあてる.<br>
	 */
	protected void onSelect() {
		int selRow = searchPartsTable.getSelectedRow();
		if (selRow >= 0) {
			Map.Entry<PartsIdentifier, PartsSpec> entry = searchPartsTableModel.getRow(selRow);
			PartsIdentifier partsIdentifier = entry.getKey();
			partsSelectionManager.setSelection(partsIdentifier);
		}
	}
	
	protected void onChangeCondition() {
		String partsNamesRaw = txtPartsName.getText();
		partsNamesRaw = partsNamesRaw.replace("　", " "); // 全角空白を半角に変換
		String[] condPartsNames = partsNamesRaw.split("\\s+");
		
		PartsCategory condPartsCategory = (PartsCategory) cmbCategories.getSelectedItem();
		String condAuthor = (String) cmbAuthors.getSelectedItem();
		if (condAuthor != null && condAuthor.trim().length() == 0) {
			condAuthor = null;
		}
		
		ArrayList<Map.Entry<PartsIdentifier, PartsSpec>> partsIdentifiers = new ArrayList<Map.Entry<PartsIdentifier, PartsSpec>>();
		
		for (PartsCategory partsCategory : partsSpecResolver.getPartsCategories()) {
			if (condPartsCategory != null && !condPartsCategory.equals(partsCategory)) {
				continue;
			}
			for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsSpecResolver.getPartsSpecMap(partsCategory).entrySet()) {
				PartsIdentifier partsIdentifier = entry.getKey();
				PartsSpec partsSpec = entry.getValue();
				if (condAuthor != null) {
					String author = partsSpec.getAuthor();
					if (author == null || !author.equals(condAuthor)) {
						continue;
					}
				}
				String localizedPartsName = partsIdentifier.getLocalizedPartsName();
				if (localizedPartsName != null) {
					for (String condPartsName : condPartsNames) {
						if (localizedPartsName.indexOf(condPartsName) >= 0) {
							partsIdentifiers.add(entry);
							continue;
						}
					}
				}
			}
		}
		Collections.sort(partsIdentifiers, new Comparator<Map.Entry<PartsIdentifier, PartsSpec>>() {
			public int compare(Entry<PartsIdentifier, PartsSpec> o1,
					Entry<PartsIdentifier, PartsSpec> o2) {
				PartsIdentifier partsIdentifier1 = o1.getKey();
				PartsIdentifier partsIdentifier2 = o2.getKey();
				return partsIdentifier1.compareTo(partsIdentifier2);
			}
		});
		searchPartsTableModel.initModel(partsIdentifiers);
	}
	
}

class SearchPartsTableModel extends AbstractTableModelWithComboBoxModel<Map.Entry<PartsIdentifier, PartsSpec>> {

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMN_NAMES;
	
	private static final int[] COLUMN_WIDTHS;
	
	static {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(SearchPartsDialog.STRINGS_RESOURCE);
		
		COLUMN_NAMES = new String[] {
				strings.getProperty("column.partsname"),
				strings.getProperty("column.category"),
				strings.getProperty("column.author"),
		};
		COLUMN_WIDTHS = new int[] {
				Integer.parseInt(strings.getProperty("column.partsname.width")),
				Integer.parseInt(strings.getProperty("column.category.width")),
				Integer.parseInt(strings.getProperty("column.author.width")),
		};
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		for (int idx = 0; idx < COLUMN_WIDTHS.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(COLUMN_WIDTHS[idx]);
		}
	}
	
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		Map.Entry<PartsIdentifier, PartsSpec> row = getRow(rowIndex);
		
		PartsIdentifier partsIdentifier = row.getKey();
		PartsSpec partsSpec = row.getValue();
		
		switch (columnIndex) {
		case 0:
			return partsIdentifier.getLocalizedPartsName();
		case 1:
			return partsIdentifier.getPartsCategory().getLocalizedCategoryName();
		case 2:
			return partsSpec.getAuthor();
		}
		return "";
	}
	
	public void initModel(List<Map.Entry<PartsIdentifier, PartsSpec>> partsIdentifiers) {
		clear();
		if (partsIdentifiers != null) {
			for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsIdentifiers) {
				addRow(entry);
			}
		}
	}
	
}

