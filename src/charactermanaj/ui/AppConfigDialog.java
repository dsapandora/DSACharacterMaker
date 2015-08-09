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
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import charactermanaj.Main;
import charactermanaj.model.AppConfig;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.util.ConfigurationDirUtilities;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.SetupLocalization;


/**
 * アプリケーション設定ダイアログ
 * 
 * @author seraphy
 */
public class AppConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(AppConfigDialog.class.getName());

	private AppConfigTableModel appConfigTableModel;
	
	private JTable appConfigTable;
	
	private JCheckBox chkResetDoNotAskAgain;
	
	private RecentCharactersDir recentCharactersDir;
	
	public AppConfigDialog(JFrame parent) {
		super(parent, true);
		try {
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					onClose();
				}
			});

			initComponent();
			
			loadData();
			
		} catch (RuntimeException ex) {
			logger.log(Level.SEVERE, "appConfig construct failed.", ex);
			dispose();
			throw ex;
		}
	}
	
	private void initComponent() {
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties("languages/appconfigdialog");
		
		setTitle(strings.getProperty("title"));

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		// buttons
		JPanel btnPanel = new JPanel();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 45));
		GridBagLayout btnPanelLayout = new GridBagLayout();
		btnPanel.setLayout(btnPanelLayout);
		
		GridBagConstraints gbc = new GridBagConstraints();
		
		Action actApply = new AbstractAction(strings.getProperty("btn.apply")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onUpdate();
			}
		};
		Action actCancel = new AbstractAction(strings.getProperty("btn.cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};
		Action actLocalization = new AbstractAction(strings.getProperty("btn.setupLocalization")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSetupLocalization();
			}
		};
		
		chkResetDoNotAskAgain = new JCheckBox(strings.getProperty("chk.askForCharactersDir"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		btnPanel.add(chkResetDoNotAskAgain, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		btnPanel.add(new JButton(actLocalization), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 2 : 1;
		gbc.weightx = 0.;
		JButton btnApply = new JButton(actApply);
		btnPanel.add(btnApply, gbc);
		
		gbc.gridx = Main.isLinuxOrMacOSX() ? 1 : 2;
		gbc.weightx = 0.;
		JButton btnCancel = new JButton(actCancel);
		btnPanel.add(btnCancel, gbc);
		
		add(btnPanel, BorderLayout.SOUTH);
		
		setSize(350, 400);
		setLocationRelativeTo(getParent());
		
		// Notes
		JLabel lblCaution = new JLabel(strings.getProperty("caution"), JLabel.CENTER);
		lblCaution.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		lblCaution.setForeground(Color.red);
		contentPane.add(lblCaution, BorderLayout.NORTH);
		
		// Model
		appConfigTableModel = new AppConfigTableModel();

		// JTable
		AppConfig appConfig = AppConfig.getInstance();
		final Color invalidBgColor = appConfig.getInvalidBgColor();
		appConfigTable = new JTable(appConfigTableModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				AppConfigRowModel rowModel = appConfigTableModel.getRow(row);
				if (rowModel.isRejected()) {
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
			@Override
			public String getToolTipText(MouseEvent event) {
				int row = rowAtPoint(event.getPoint());
				int col = columnAtPoint(event.getPoint());
				if (col == 0) {
					AppConfigRowModel rowModel = appConfigTableModel.getRow(row);
					return rowModel.getDisplayName();
				}
				return super.getToolTipText(event);
			}
		};
		appConfigTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		appConfigTable.setShowGrid(true);
		appConfigTable.setGridColor(AppConfig.getInstance().getGridColor());
		appConfigTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		appConfigTableModel.adjustColumnModel(appConfigTable.getColumnModel());
		JScrollPane appConfigTableSP = new JScrollPane(appConfigTable);
		appConfigTableSP.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 3, 0, 3),
				BorderFactory.createTitledBorder(strings.getProperty("table.caption")))
				);
		appConfigTableSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		contentPane.add(appConfigTableSP, BorderLayout.CENTER);
		
		// RootPane
		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeAppConfigDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeAppConfigDialog");
		am.put("closeAppConfigDialog", actCancel);

		// 保存先が無効であれば適用ボタンを有効にしない.
		boolean enableSave = !appConfig.getPrioritySaveFileList().isEmpty();
		btnApply.setEnabled(enableSave);
	}
	
	private void loadData() {
		Properties original = AppConfig.getInstance().getProperties();
		appConfigTableModel.initModel(original);

		try {
			recentCharactersDir = RecentCharactersDir.load();

			if (recentCharactersDir != null) {
				File lastUseCharactersDir = recentCharactersDir.getLastUseCharacterDir();
				boolean enableLastUseCharacterDir = lastUseCharactersDir != null && lastUseCharactersDir.isDirectory(); 
				boolean doNotAskAgain = enableLastUseCharacterDir && recentCharactersDir.isDoNotAskAgain();
				chkResetDoNotAskAgain.setEnabled(enableLastUseCharacterDir);
				chkResetDoNotAskAgain.setSelected(!doNotAskAgain);
			}

		} catch (Exception ex) {
			recentCharactersDir = null;
			logger.log(Level.WARNING, "RecentCharactersDir load failed.", ex);
		}
	}

	/**
	 * ローカライズリソースをユーザディレクトリ上に展開する.
	 */
	protected void onSetupLocalization() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties("languages/appconfigdialog");
		if (JOptionPane.showConfirmDialog(this,
				strings.getProperty("setupLocalization"),
				strings.getProperty("confirm.setupLocalization.caption"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
			return;
		}
		
		try {
			File baseDir = ConfigurationDirUtilities.getUserDataDir();
			SetupLocalization setup = new SetupLocalization(baseDir);
			setup.setupToLocal(
					EnumSet.allOf(SetupLocalization.Resources.class), true);

			File resourceDir = setup.getResourceDir();
			DesktopUtilities.open(resourceDir);

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void onClose() {
		if (appConfigTableModel.isModified()) {
			Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
					.getLocalizedProperties("languages/appconfigdialog");
			if (JOptionPane.showConfirmDialog(this, strings.getProperty("confirm.close"),
					strings.getProperty("confirm.close.caption"), JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
				return;
			}
		}
		dispose();
	}
	
	protected void onUpdate() {
		
		if (appConfigTable.isEditing()) {
			// 編集中ならば許可しない.
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return;
		}
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties("languages/appconfigdialog");

		// 編集されたプロパティを取得する.
		Properties props = appConfigTableModel.getProperties();

		// 編集されたプロパティが適用可能か検証する.
		Set<String> rejectNames = AppConfig.checkProperties(props);
		if (!rejectNames.isEmpty()) {
			// エラーがある場合
			appConfigTableModel.setRejectNames(rejectNames);

			JOptionPane.showMessageDialog(this, strings.getProperty("error.message"),
					strings.getProperty("error.caption"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			// アプリケーション設定を更新し、保存する.
			AppConfig appConfig = AppConfig.getInstance();
			appConfig.update(props);
			appConfig.saveConfig();

			// キャラクターデータディレクトリの起動時の選択
			if (chkResetDoNotAskAgain.isEnabled()) {
				boolean doNotAskAgain = !chkResetDoNotAskAgain.isSelected();
				if (doNotAskAgain != recentCharactersDir.isDoNotAskAgain()) {
					recentCharactersDir.setDoNotAskAgain(doNotAskAgain);
					recentCharactersDir.saveRecents();
				}
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
			return;
		}
		
		// アプリケーションの再起動が必要なことを示すダイアログを表示する.
		String message = strings.getProperty("caution");
		JOptionPane.showMessageDialog(this, message);

		dispose();
	}
	
}

class AppConfigRowModel implements Comparable<AppConfigRowModel> {

	private Properties target;
	
	private String key;
	
	private String displayName;
	
	private boolean rejected;
	
	public AppConfigRowModel(Properties target, String key, String displayName) {
		this.target = target;
		this.key = key;
		this.displayName = displayName;
	}

	@Override
	public int hashCode() {
		return key == null ? 0 : key.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof AppConfigRowModel) {
			AppConfigRowModel o = (AppConfigRowModel) obj;
			return key == null ? o.key == null : key.equals(o.key);
		}
		return false;
	}
	
	public int compareTo(AppConfigRowModel o) {
		if (o == this) {
			return 0;
		}
		int ret = displayName.compareTo(o.displayName);
		if (ret == 0) {
			ret = key.compareTo(o.key);
		}
		return ret;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setValue(String value) {
		if (value == null) {
			value = "";
		}
		target.setProperty(key, value);
	}
	
	public String getValue() {
		return target.getProperty(key);
	}

	public String getDisplayName() {
		int sep = displayName.indexOf(';');
		if (sep >= 0) {
			return displayName.substring(sep + 1);
		}
		return displayName;
	}
	
	public boolean isRejected() {
		return rejected;
	}
	
	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}
}

class AppConfigTableModel extends AbstractTableModelWithComboBoxModel<AppConfigRowModel> {

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMN_NAMES;
	
	private static final int[] COLUMN_WIDTHS;
	
	private Properties target = new Properties();
	
	private Properties original;
	
	static {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties("languages/appconfigdialog");

		COLUMN_NAMES = new String[] {
				strings.getProperty("column.key"),
				strings.getProperty("column.value"),
		};
		COLUMN_WIDTHS = new int[] {
				Integer.parseInt(strings.getProperty("column.key.width")),
				Integer.parseInt(strings.getProperty("column.value.width")),
		};
	}
	
	public void initModel(Properties original) {
		clear();
		target.clear();

		this.original = original;
		if (original != null) {
			target.putAll(original);
			
			Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
					.getLocalizedProperties("languages/appconfigdialog");

			for (Object key : target.keySet()) {
				String displayName = strings.getProperty((String) key);
				if (displayName == null || displayName.length() == 0) {
					displayName = (String) key;
				}
				
				AppConfigRowModel rowModel = new AppConfigRowModel(target, (String) key, displayName);
				addRow(rowModel);
			}
		}

		sort();
	}
	
	public void sort() {
		Collections.sort(elements);
		fireTableDataChanged();
	}
	
	public void setRejectNames(Set<String> rejectNames) {
		for (AppConfigRowModel rowModel : elements) {
			String key = rowModel.getKey();
			boolean rejected = (rejectNames != null && rejectNames.contains(key));
			rowModel.setRejected(rejected);
		}
		fireTableDataChanged();
	}
	
	/**
	 * 編集されているか?
	 * 
	 * @return 編集されていればtrue、そうでなければfalse
	 */
	public boolean isModified() {
		if (original == null) {
			return true;
		}
		return !original.equals(target);
	}
	
	public Properties getProperties() {
		return target;
	}
	
	
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}
	
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 1) {
			return true;
		}
		return false;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		AppConfigRowModel rowModel = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return rowModel.getDisplayName();
		case 1:
			return rowModel.getValue();
		}
		return "";
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		AppConfigRowModel rowModel = getRow(rowIndex);
		if (columnIndex == 1) {
			rowModel.setValue((String) aValue);
			fireTableCellUpdated(rowIndex, columnIndex);
		}
	}
	
	public void adjustColumnModel(TableColumnModel columnModel) {
		int mx = columnModel.getColumnCount();
		for (int idx = 0; idx < mx; idx++) {
			columnModel.getColumn(idx).setWidth(COLUMN_WIDTHS[idx]);
		}
	}
}
