package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import charactermanaj.Main;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsAuthorInfo;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.PartsManageData.PartsKey;
import charactermanaj.model.PartsManageData.PartsVersionInfo;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.io.PartsInfoXMLReader;
import charactermanaj.model.io.PartsInfoXMLWriter;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;

public class PartsManageDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	protected static final String STRINGS_RESOURCE = "languages/partsmanagedialog";
	
	
	private static final Logger logger = Logger.getLogger(PartsManageDialog.class.getName());

	private CharacterData characterData;
	
	private PartsManageTableModel partsManageTableModel;
	
	private JTable partsManageTable;
	
	private JTextField txtHomepage;
	
	private JTextField txtAuthor;
	
	private boolean updated;

	
	public PartsManageDialog(JFrame parent, CharacterData characterData) {
		super(parent, true);
		
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		this.characterData = characterData;
		
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

		Container contentPane = getContentPane();
		
		// パーツリストテーブル
		JPanel partsListPanel = new JPanel();
		partsListPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory
						.createTitledBorder(strings.getProperty("partslist"))));

		GridBagLayout partsListPanelLayout = new GridBagLayout();
		partsListPanel.setLayout(partsListPanelLayout);
		
		partsManageTableModel = new PartsManageTableModel();
		partsManageTable = new JTable(partsManageTableModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int rowIdx, int columnIdx) {
				PartsManageTableModel.Columns column = PartsManageTableModel.Columns
						.values()[columnIdx];
				Component comp = super.prepareRenderer(renderer, rowIdx, columnIdx);
				PartsManageTableRow row = partsManageTableModel.getRow(rowIdx);

				Timestamp current = row.getTimestamp();
				Timestamp lastModified = row.getLastModified();

				boolean warnings = false;

				if (current != null && !current.equals(lastModified)) {
					// 現在のパーツの最終更新日と、パーツ管理情報の作成時のパーツの最終更新日が不一致の場合
					warnings = true;
				}

				// 背景色、警告行は赤色に
				if (warnings && column == PartsManageTableModel.Columns.LastModified) {
					AppConfig appConfig = AppConfig.getInstance();
					Color invalidBgColor = appConfig.getInvalidBgColor();
					comp.setBackground(invalidBgColor);
				} else {
					if (isCellSelected(rowIdx, columnIdx)) {
						comp.setBackground(getSelectionBackground());
					} else {
						comp.setBackground(getBackground());
					}
				}

				return comp;
			}
		};
		partsManageTable.setShowGrid(true);
		partsManageTable.setGridColor(AppConfig.getInstance().getGridColor());
		partsManageTableModel.adjustColumnModel(partsManageTable.getColumnModel());
		partsManageTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		partsManageTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		JScrollPane partsManageTableSP = new JScrollPane(partsManageTable);
		
		partsManageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					onChangeSelection();
				}
			}
		});
		
		partsManageTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				onTableDataChange(e.getFirstRow(), e.getLastRow());
			}
		});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 4;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		partsListPanel.add(partsManageTableSP, gbc);
		
		Action actSortByName = new AbstractAction(strings.getProperty("sortByName")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSortByName();
			}
		};
		Action actSortByAuthor = new AbstractAction(strings.getProperty("sortByAuthor")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSortByAuthor();
			}
		};
		Action actSortByTimestamp = new AbstractAction(strings.getProperty("sortByTimestamp")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSortByTimestamp();
			}
		};
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		partsListPanel.add(new JButton(actSortByName), gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		partsListPanel.add(new JButton(actSortByAuthor), gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		partsListPanel.add(new JButton(actSortByTimestamp), gbc);

		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		partsListPanel.add(Box.createHorizontalGlue(), gbc);

		contentPane.add(partsListPanel, BorderLayout.CENTER);

		// テーブルのコンテキストメニュー
		final JPopupMenu popupMenu = new JPopupMenu();
		Action actApplyAllLastModified = new AbstractAction(strings.getProperty("applyAllLastModified")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onApplyAllLastModified();
			}
		};
		Action actApplyAllDownloadURL = new AbstractAction(strings.getProperty("applyAllDownloadURL")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onApplyAllDownloadURL();
			}
		};
		Action actApplyAllVersion = new AbstractAction(strings.getProperty("applyAllVersion")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onApplyAllVersion();
			}
		};
		popupMenu.add(actApplyAllLastModified);
		popupMenu.add(new JSeparator());
		popupMenu.add(actApplyAllVersion);
		popupMenu.add(actApplyAllDownloadURL);
		
		partsManageTable.setComponentPopupMenu(popupMenu);
		
		// 作者情報パネル
		JPanel authorPanel = new JPanel();
		authorPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createEmptyBorder(5, 5, 5, 5), BorderFactory
				.createTitledBorder(strings.getProperty("author.info"))));
		GridBagLayout authorPanelLayout = new GridBagLayout();
		authorPanel.setLayout(authorPanelLayout);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		authorPanel.add(new JLabel(strings.getProperty("author"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.;
		txtAuthor = new JTextField();
		authorPanel.add(txtAuthor, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		authorPanel.add(new JLabel(strings.getProperty("homepage"), JLabel.RIGHT), gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		txtHomepage = new JTextField();
		authorPanel.add(txtHomepage, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		Action actBrowseHomepage = new AbstractAction(strings.getProperty("open")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onBrosweHomepage();
			}
		};
		authorPanel.add(new JButton(actBrowseHomepage), gbc);

		if (!DesktopUtilities.isSupported()) {
			actBrowseHomepage.setEnabled(false);
		}
		
		txtAuthor.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				onEditAuthor();
			}
			public void insertUpdate(DocumentEvent e) {
				onEditAuthor();
			}
			public void changedUpdate(DocumentEvent e) {
				onEditAuthor();
			}
		});
		txtHomepage.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				onEditHomepage();
			}
			public void insertUpdate(DocumentEvent e) {
				onEditHomepage();
			}
			public void changedUpdate(DocumentEvent e) {
				onEditHomepage();
			}
		});
		
		
		// ボタンパネル
		JPanel btnPanel = new JPanel();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 45));
		GridBagLayout btnPanelLayout = new GridBagLayout();
		btnPanel.setLayout(btnPanelLayout);
		
		Action actClose = new AbstractAction(strings.getProperty("cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};
		Action actOK = new AbstractAction(strings.getProperty("update")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		};
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 2 : 1;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		btnPanel.add(new JButton(actOK), gbc);

		gbc.gridx = Main.isLinuxOrMacOSX() ? 1 : 2;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		btnPanel.add(new JButton(actClose), gbc);
		
		// ダイアログ下部
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(authorPanel, BorderLayout.NORTH);
		southPanel.add(btnPanel, BorderLayout.SOUTH);

		contentPane.add(southPanel, BorderLayout.SOUTH);
		
		// キーボード
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closePartsManageDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closePartsManageDialog");
		am.put("closePartsManageDialog", actClose);

		// モデル構築
		partsManageTableModel.initModel(characterData);
		
		// ウィンドウ配置
		setSize(500, 400);
		setLocationRelativeTo(parent);
	}

	private Semaphore authorEditSemaphore = new Semaphore(1);
	
	protected void onChangeSelection() {
		try {
			authorEditSemaphore.acquire();
			try {
				int [] selRows = partsManageTable.getSelectedRows();
				HashSet<String> authors = new HashSet<String>();
				for (int selRow : selRows) {
					PartsManageTableRow row = partsManageTableModel.getRow(selRow);
					authors.add(row.getAuthor() == null ? "" : row.getAuthor());
				}
				if (authors.size() > 1) {
					AppConfig appConfig = AppConfig.getInstance();
					txtAuthor.setBackground(appConfig.getAuthorEditConflictBgColor());
					txtHomepage.setBackground(appConfig.getAuthorEditConflictBgColor());
				} else {
					Color bgColor = UIManager.getColor("TextField.background");
					if (bgColor == null) {
						bgColor = Color.white;
					}
					txtAuthor.setBackground(bgColor);
					txtHomepage.setBackground(bgColor);
				}
				if (authors.isEmpty()) {
					// 選択されているauthorがない場合は全部編集不可
					txtAuthor.setEditable(false);
					txtAuthor.setText("");
					txtHomepage.setEditable(false);
					txtHomepage.setText("");
				} else {
					// 選択されているAuthorが1つ以上あればAuthorは編集可
					txtAuthor.setEditable(true);
					txtHomepage.setEditable(true);
					if (authors.size() == 1) {
						// 選択されているAuthorが一個であれば、それを表示
						String author = authors.iterator().next();
						String homepage = partsManageTableModel.getHomepage(author);
						txtAuthor.setText(author);
						txtHomepage.setText(homepage);
					} else {
						// 選択されているAuthorが二個以上あれば編集可能だがテキストには表示しない.
						txtAuthor.setText("");
						txtHomepage.setText("");
					}
				}
			} finally {
				authorEditSemaphore.release();
			}

		} catch (InterruptedException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);

		} catch (RuntimeException ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void onTableDataChange(int firstRow, int lastRow) {
		onChangeSelection();
	}
	
	protected void onApplyAllLastModified() {
		int[] selRows = partsManageTable.getSelectedRows();
		if (selRows.length == 0) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		Arrays.sort(selRows);

		for (int selRow : selRows) {
			PartsManageTableRow row = partsManageTableModel.getRow(selRow);
			Timestamp dt = row.getTimestamp();
			row.setLastModified(dt);
		}
		partsManageTableModel.fireTableRowsUpdated(selRows[0],
				selRows[selRows.length - 1]);
	}

	protected void onApplyAllDownloadURL() {
		int[] selRows = partsManageTable.getSelectedRows();
		if (selRows.length == 0) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		Arrays.sort(selRows);

		HashSet<String> authors = new HashSet<String>();
		for (int selRow : selRows) {
			PartsManageTableRow row = partsManageTableModel.getRow(selRow);
			authors.add(row.getAuthor() == null ? "" : row.getAuthor());
		}

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		if (authors.size() > 1) {
			if (JOptionPane.showConfirmDialog(this,
					strings.getProperty("confirm.authorConflict"),
					strings.getProperty("confirm"),
					JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return;
			}
		}
		
		PartsManageTableRow firstRow = partsManageTableModel.getRow(selRows[0]);
		String downloadURL = firstRow.getDownloadURL();
		if (downloadURL == null) {
			downloadURL = "";
		}
		String downloadURL_new = JOptionPane.showInputDialog(this, strings.getProperty("input.downloadURL"), downloadURL);
		if (downloadURL_new == null || downloadURL.equals(downloadURL_new)) {
			// キャンセルされたか、内容に変化ない場合は何もしない
			return;
		}
		
		for (int selRow : selRows) {
			PartsManageTableRow row = partsManageTableModel.getRow(selRow);
			row.setDownloadURL(downloadURL_new);

			Timestamp dt = row.getTimestamp();
			row.setLastModified(dt);
		}
		partsManageTableModel.fireTableRowsUpdated(selRows[0], selRows[selRows.length - 1]);
	}
	
	protected void onApplyAllVersion() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		int[] selRows = partsManageTable.getSelectedRows();
		if (selRows.length == 0) {
			tk.beep();
			return;
		}
		Arrays.sort(selRows);

		HashSet<String> authors = new HashSet<String>();
		for (int selRow : selRows) {
			PartsManageTableRow row = partsManageTableModel.getRow(selRow);
			authors.add(row.getAuthor() == null ? "" : row.getAuthor());
		}

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		if (authors.size() > 1) {
			if (JOptionPane.showConfirmDialog(this,
					strings.getProperty("confirm.authorConflict"),
					strings.getProperty("confirm"),
					JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return;
			}
		}

		PartsManageTableRow firstRow = partsManageTableModel.getRow(selRows[0]);
		double version = firstRow.getVersion();
		String strVersion = (version < 0) ? "" : Double.toString(version);
		String strVersion_new = JOptionPane.showInputDialog(this,
				strings.getProperty("input.version"), strVersion);
		if (strVersion_new == null || strVersion.equals(strVersion_new)) {
			// キャンセルされたか、内容に変化ない場合は何もしない
			return;
		}
		double version_new;
		try {
			version_new = Double.parseDouble(strVersion_new);
		} catch (Exception ex) {
			// 数値として不正であれば何もしない.
			tk.beep();
			return;
		}
		
		for (int selRow : selRows) {
			PartsManageTableRow row = partsManageTableModel.getRow(selRow);
			row.setVersion(version_new);

			Timestamp dt = row.getTimestamp();
			row.setLastModified(dt);
		}
		partsManageTableModel.fireTableRowsUpdated(selRows[0], selRows[selRows.length - 1]);
	}
	
	protected void onEditHomepage() {
		try {
			if (!authorEditSemaphore.tryAcquire()) {
				return;
			}
			try {
				String author = txtAuthor.getText();
				String homepage = txtHomepage.getText();
				partsManageTableModel.setHomepage(author, homepage);
			} finally {
				authorEditSemaphore.release();
			}
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	protected void onEditAuthor() {
		try {
			if (!authorEditSemaphore.tryAcquire()) {
				return;
			}
			try {
				String author = txtAuthor.getText();
				int[] selRows = partsManageTable.getSelectedRows();
				int firstRow = -1;
				int lastRow = Integer.MAX_VALUE;
				for (int selRow : selRows) {
					PartsManageTableRow row = partsManageTableModel.getRow(selRow);
					String oldValue = row.getAuthor();
					if (oldValue == null || !oldValue.equals(author)) {
						row.setAuthor(author);

						Timestamp dt = row.getTimestamp();
						row.setLastModified(dt);

						firstRow = Math.max(firstRow, selRow);
						lastRow = Math.min(lastRow, selRow);
					}
				}
				
				String homepage = partsManageTableModel.getHomepage(author);
				if (homepage == null) {
					homepage = "";
				}
				txtHomepage.setText(homepage);
				
				if (firstRow >= 0) {
					partsManageTable.repaint();
				}
			} finally {
				authorEditSemaphore.release();				
			}
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void onClose() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);
		if (JOptionPane.showConfirmDialog(this,
				strings.getProperty("confirm.cancel"),
				strings.getProperty("confirm"),
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			return;
		}
		updated = false;
		dispose();
	}
	
	protected void onBrosweHomepage() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		String homepage = txtHomepage.getText();
		if (homepage == null || homepage.trim().length() == 0) {
			tk.beep();
			return;
		}
		try {
			URI uri = new URI(homepage);
			DesktopUtilities.browse(uri);

		} catch (Exception ex) {
			tk.beep();
			logger.log(Level.INFO, "browse failed. : " + homepage, ex);
		}
	}
	
	protected void onSortByAuthor() {
		partsManageTableModel.sortByAuthor();
	}
	
	protected void onSortByName() {
		partsManageTableModel.sortByName();
	}
	
	protected void onSortByTimestamp() {
		partsManageTableModel.sortByTimestamp();
	}
	
	protected void onOK() {
		if (partsManageTable.isEditing()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		
		int mx = partsManageTableModel.getRowCount();

		// 作者ごとのホームページ情報の取得
		// (同一作者につきホームページは一つ)
		HashMap<String, PartsAuthorInfo> authorInfoMap = new HashMap<String, PartsAuthorInfo>();
		for (int idx = 0; idx < mx; idx++) {
			PartsManageTableRow row = partsManageTableModel.getRow(idx);
			String author = row.getAuthor();
			String homepage = row.getHomepage();
			if (author != null && author.trim().length() > 0) {
				PartsAuthorInfo authorInfo = authorInfoMap.get(author.trim());
				if (authorInfo == null) {
					authorInfo = new PartsAuthorInfo();
					authorInfo.setAuthor(author.trim());
					authorInfoMap.put(authorInfo.getAuthor(), authorInfo);
				}
				authorInfo.setHomePage(homepage);
			}
		}

		PartsManageData partsManageData = new PartsManageData();
		
		// パーツごとの作者とバージョン、ダウンロード先の取得
		for (int idx = 0; idx < mx; idx++) {
			PartsManageTableRow row = partsManageTableModel.getRow(idx);
			
			String author = row.getAuthor();
			PartsAuthorInfo partsAuthorInfo = null;
			if (author != null && author.trim().length() > 0) {
				partsAuthorInfo = authorInfoMap.get(author.trim());
			}
			
			double version = row.getVersion();
			String downloadURL = row.getDownloadURL();
			String localizedName = row.getLocalizedName();
			Timestamp lastModified = row.getLastModified();
			
			PartsManageData.PartsVersionInfo versionInfo = new PartsManageData.PartsVersionInfo();
			versionInfo.setVersion(version);
			versionInfo.setDownloadURL(downloadURL);
			versionInfo.setLastModified(lastModified);

			PartsIdentifier partsIdentifier = row.getPartsIdentifier();
			
			PartsManageData.PartsKey partsKey = new PartsManageData.PartsKey(partsIdentifier);
			partsManageData.putPartsInfo(partsKey, localizedName,
					partsAuthorInfo, versionInfo);
		}
		
		// パーツ管理情報を書き込む.
		PartsInfoXMLWriter xmlWriter = new PartsInfoXMLWriter();
		try {
			URI docBase = characterData.getDocBase();
			xmlWriter.savePartsManageData(docBase, partsManageData);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}
		
		updated = true;
		dispose();
	}

	/**
	 * パーツ管理情報が更新されたか?
	 * 
	 * @return 更新された場合はtrue、そうでなければfalse
	 */
	public boolean isUpdated() {
		return updated;
	}
}

class PartsManageTableRow {
	
	private PartsIdentifier partsIdentifier;
	
	private Timestamp timestamp;
	
	private String localizedName;
	
	private String author;
	
	private String homepage;
	
	private String downloadURL;
	
	private double version;
	
	private Timestamp lastModified;

	public PartsManageTableRow(PartsIdentifier partsIdentifier,
			PartsSpec partsSpec, Timestamp lastModified) {
		if (partsIdentifier == null || partsSpec == null) {
			throw new IllegalArgumentException();
		}
		this.partsIdentifier = partsIdentifier;
		this.localizedName = partsIdentifier.getLocalizedPartsName();

		this.timestamp = new Timestamp(partsSpec.getPartsFiles().lastModified());
		
		this.lastModified = lastModified;
		
		PartsAuthorInfo autherInfo = partsSpec.getAuthorInfo();
		if (autherInfo != null) {
			author = autherInfo.getAuthor();
			homepage = autherInfo.getHomePage();
		}
		if (author == null) {
			author = "";
		}
		if (homepage == null) {
			homepage = "";
		}
		downloadURL = partsSpec.getDownloadURL();
		version = partsSpec.getVersion();
	}
	
	public PartsIdentifier getPartsIdentifier() {
		return partsIdentifier;
	}
	
	public Timestamp getTimestamp() {
		return timestamp;
	}

	public String getLocalizedName() {
		return localizedName;
	}
	
	public void setLocalizedName(String localizedName) {
		if (localizedName == null || localizedName.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		this.localizedName = localizedName;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public String getDownloadURL() {
		return downloadURL;
	}
	
	public double getVersion() {
		return version;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}
	
	public void setVersion(double version) {
		this.version = version;
	}
	
	public String getHomepage() {
		return homepage;
	}
	
	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}
	
	public Timestamp getLastModified() {
		return lastModified;
	}

	public void setLastModified(Timestamp lastModified) {
		this.lastModified = lastModified;
	}
}

class PartsManageTableModel extends AbstractTableModelWithComboBoxModel<PartsManageTableRow> {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger
			.getLogger(PartsManageTableModel.class.getName());

	private static Properties strings = LocalizedResourcePropertyLoader
			.getCachedInstance().getLocalizedProperties(
					PartsManageDialog.STRINGS_RESOURCE);

	/**
	 * カラムの定義
	 */
	public enum Columns {
		PartsID("column.partsid", false, String.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getPartsIdentifier().getPartsName();
			}
		},
		LastModified("column.lastmodified", false, String.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getTimestamp().toString();
			}
		},
		Category("column.category", false, String.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getPartsIdentifier().getPartsCategory()
						.getLocalizedCategoryName();
			}
		},
		PartsName("column.partsname", true, String.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getLocalizedName();
			}
			@Override
			public void setValue(PartsManageTableRow row, Object value) {
				String localizedName = (String) value;
				if (localizedName != null && localizedName.trim().length() > 0) {
					String oldValue = row.getLocalizedName();
					if (oldValue != null && oldValue.equals(localizedName)) {
						return; // 変化なし
					}
					row.setLocalizedName(localizedName);
				}
			}
		},
		Author("column.author", true, String.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getAuthor();
			}
			@Override
			public void setValue(PartsManageTableRow row, Object value) {
				String author = (String) value;
				if (author == null) {
					author = "";
				}
				String oldValue = row.getAuthor();
				if (oldValue != null && oldValue.equals(author)) {
					return; // 変化なし
				}
				row.setAuthor(author);
			}
		},
		Version("column.version", true, Double.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getVersion() > 0 ? row.getVersion() : null;
			}
			@Override
			public void setValue(PartsManageTableRow row, Object value) {
				Double version = (Double) value;
				if (version == null || version.doubleValue() <= 0) {
					version = Double.valueOf(0.);
				}
				Double oldValue = row.getVersion();
				if (oldValue != null && oldValue.equals(version)) {
					return; // 変化なし
				}
				row.setVersion(version);
			}
		},
		DownloadURL("column.downloadURL", true, String.class) {
			@Override
			public Object getValue(PartsManageTableRow row) {
				return row.getDownloadURL();
			}
			@Override
			public void setValue(PartsManageTableRow row, Object value) {
				String downloadURL = (String) value;
				if (downloadURL == null) {
					downloadURL = "";
				}
				String oldValue = row.getDownloadURL();
				if (oldValue != null && oldValue.equals(downloadURL)) {
					return; // 変化なし
				}
				row.setDownloadURL(downloadURL);
			}
		};

		private final Class<?> columnClass;

		private final boolean editable;

		private final String columnName;

		private String displayName;

		private int width;

		private Columns(String columnName, boolean editable,
				Class<?> columnClass) {
			this.columnName = columnName;
			this.editable = editable;
			this.columnClass = columnClass;
		}

		public abstract Object getValue(PartsManageTableRow row);

		public boolean isEditable() {
			return editable;
		}

		public Class<?> getColumnClass() {
			return columnClass;
		}

		public String getDisplayName() {
			init();
			return displayName;
		}

		public int getWidth() {
			init();
			return width;
		}

		public void setValue(PartsManageTableRow row, Object value) {
			// 何もしない.
		}

		private void init() {
			if (displayName != null) {
				return;
			}
			displayName = strings.getProperty(columnName);
			width = Integer
					.parseInt(strings.getProperty(columnName + ".width"));
		}
	}

	
	public int getColumnCount() {
		return Columns.values().length;
	}
	
	@Override
	public String getColumnName(int column) {
		return Columns.values()[column].getDisplayName();
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		Columns[] columns = Columns.values();
		for (int idx = 0; idx < columns.length; idx++) {
			columnModel.getColumn(idx).setPreferredWidth(
					columns[idx].getWidth());
		}
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		PartsManageTableRow row = getRow(rowIndex);
		Columns column = Columns.values()[columnIndex];
		return column.getValue(row);
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		PartsManageTableRow row = getRow(rowIndex);
		Columns column = Columns.values()[columnIndex];
		if (!column.isEditable()) {
			return;
		}
		column.setValue(row, aValue);

		// 更新日を最新にする
		Timestamp dt = row.getTimestamp();
		row.setLastModified(dt);

		// 変更通知
		fireTableRowsUpdated(rowIndex, columnIndex);
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Columns column = Columns.values()[columnIndex];
		return column.getColumnClass();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		Columns column = Columns.values()[columnIndex];
		return column.isEditable();
	}
	
	public void initModel(CharacterData characterData) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		clear();

		// 既存のパーツ管理情報ファイルがあれば読み込む
		URI docBase = characterData.getDocBase();
		PartsManageData partsManageData = null;
		if (docBase != null) {
			try {
				PartsInfoXMLReader reader = new PartsInfoXMLReader();
				partsManageData = reader.loadPartsManageData(docBase);

			} catch (Exception ex) {
				logger.log(Level.WARNING, ex.toString(), ex);
			}
		}
		if (partsManageData == null) {
			partsManageData = new PartsManageData();
		}

		for (PartsCategory partsCategory : characterData.getPartsCategories()) {
			for (Map.Entry<PartsIdentifier, PartsSpec> entry : characterData
					.getPartsSpecMap(partsCategory).entrySet()) {
				PartsIdentifier partsIdentifier = entry.getKey();
				PartsSpec partsSpec = entry.getValue();
				
				// パーツ管理情報ファイルから、パーツ管理情報を設定した時点の
				// ファイルサイズや更新日時などを読み取る.
				PartsKey partsKey = new PartsKey(partsIdentifier);
				PartsVersionInfo versionInfo = partsManageData
						.getVersion(partsKey);

				Timestamp lastModified = null;

				if (versionInfo != null) {
					lastModified = versionInfo.getLastModified();
				}

				PartsManageTableRow row = new PartsManageTableRow(
						partsIdentifier, partsSpec, lastModified);
				addRow(row);
			}
		}

		sortByAuthor();
	}

	/**
	 * ホームページを設定する.<br>
	 * ホームページはAuthorに対して1つであるが、Authorが自由編集可能であるため便宜的にRowに持たせている.<br>
	 * 結果として同じAuthorに対して同じ値を設定する必要がある.<br>
	 * ホームページはテーブルに表示されないのでリスナーへの通知は行わない.<br>
	 * 
	 * @param author
	 *            作者、空またはnullは何もしない.
	 * @param homepage
	 *            ホームページ
	 */
	public void setHomepage(String author, String homepage) {
		if (author == null || author.length() == 0) {
			return;
		}
		for (PartsManageTableRow row : elements) {
			String targetAuthor = row.getAuthor();
			if (targetAuthor == null) {
				targetAuthor = "";
			}
			if (targetAuthor.equals(author)) {
				row.setHomepage(homepage);
			}
		}
	}

	/**
	 * ホームページを取得する.<br>
	 * 該当する作者がないか、作者がnullまたは空の場合は常にnullを返す.<br>
	 * 
	 * @param author
	 *            作者
	 * @return ホームページ、またはnull
	 */
	public String getHomepage(String author) {
		if (author == null || author.length() == 0) {
			return null;
		}
		for (PartsManageTableRow row : elements) {
			String targetAuthor = row.getAuthor();
			if (targetAuthor == null) {
				targetAuthor = "";
			}
			if (targetAuthor.equals(author)) {
				return row.getHomepage();
			}
		}
		return null;
	}
	
	protected static final Comparator<PartsManageTableRow> NAMED_SORTER
		= new Comparator<PartsManageTableRow>() {
		public int compare(PartsManageTableRow o1, PartsManageTableRow o2) {
			// カテゴリで順序づけ
			int ret = o1.getPartsIdentifier().getPartsCategory().compareTo(
					o2.getPartsIdentifier().getPartsCategory());
			if (ret == 0) {
				// 表示名で順序づけ
				String lnm1 = o1.getLocalizedName();
				String lnm2 = o2.getLocalizedName();
				if (lnm1 == null) {
					lnm1 = "";
				}
				if (lnm2 == null) {
					lnm2 = "";
				}
				ret = lnm1.compareTo(lnm2);
			}
			if (ret == 0) {
				// それでも判定できなければ元の識別子で判定する.
				ret = o1.getPartsIdentifier().compareTo(o2.getPartsIdentifier());
			}
			return ret;
		}
	};
	
	public void sortByName() {
		Collections.sort(elements, NAMED_SORTER);
		fireTableDataChanged();
	}
	
	public void sortByTimestamp() {
		Collections.sort(elements, new Comparator<PartsManageTableRow>() {
			public int compare(PartsManageTableRow o1, PartsManageTableRow o2) {
				// 更新日で順序づけ (新しいもの順)
				int ret = o1.getTimestamp().compareTo(o2.getTimestamp()) * -1;
				if (ret == 0) {
					// それでも判定できなければ名前順と同じ
					ret = NAMED_SORTER.compare(o1, o2);
				}
				return ret;
			}
		});
		fireTableDataChanged();
	}

	public void sortByAuthor() {
		Collections.sort(elements, new Comparator<PartsManageTableRow>() {
			public int compare(PartsManageTableRow o1, PartsManageTableRow o2) {
				// 作者で順序づけ
				String author1 = o1.getAuthor();
				String author2 = o2.getAuthor();
				if (author1 == null) {
					author1 = "";
				}
				if (author2 == null) {
					author2 = "";
				}
				int ret = author1.compareTo(author2);
				if (ret == 0) {
					// それでも判定できなければ名前順と同じ
					ret = NAMED_SORTER.compare(o1, o2);
				}
				return ret;
			}
		});
		fireTableDataChanged();
	}
}
