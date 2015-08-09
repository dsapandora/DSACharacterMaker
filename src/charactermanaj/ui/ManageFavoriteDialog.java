package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsSet;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.UIHelper;

/**
 * お気に入りの編集ダイアログ
 * 
 * @author seraphy
 */
public class ManageFavoriteDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	protected static final String STRINGS_RESOURCE = "languages/managefavoritesdialog";

	private CharacterData characterData;
	
	private PartsSetListTableModel partsSetListModel;
	
	private JTable partsSetList;

	private FavoriteManageCallback callback;

	private Action actSelect;

	private Action actDelete;

	private Action actRename;

	public static class PartsSetListTableModel extends AbstractTableModel {

		/**
		 * シリアライズバージョンID
		 */
		private static final long serialVersionUID = 3012538368342673506L;

		/**
		 * パーツセットのリスト
		 */
		private List<PartsSet> partsSetList = Collections.emptyList();

		private enum Columns {
			DISPLAY_NAME("Name") {
				@Override
				public Object getValue(PartsSet partsSet) {
					if (partsSet != null) {
						return partsSet.getLocalizedName();
					}
					return null;
				}
			},
			IS_PRESET("Type") {
				@Override
				public Object getValue(PartsSet partsSet) {
					if (partsSet != null) {
						return partsSet.isPresetParts()
								? "Preset"
								: "Favorites";
					}
					return null;
				}
			};

			private String columnName;

			private Columns(String columnName) {
				this.columnName = columnName;
			}

			public Class<?> getColumnClass() {
				return String.class;
			}

			public String getColumnName() {
				return columnName;
			}

			public abstract Object getValue(PartsSet partsSet);
		}

		private static Columns[] columns = Columns.values();

		public int getColumnCount() {
			return columns.length;
		}

		public int getRowCount() {
			return partsSetList.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			PartsSet partsSet = getRow(rowIndex);
			return columns[columnIndex].getValue(partsSet);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columns[columnIndex].getColumnClass();
		}

		@Override
		public String getColumnName(int column) {
			return columns[column].getColumnName();
		}

		public PartsSet getRow(int rowIndex) {
			return partsSetList.get(rowIndex);
		}

		public void updateRow(int rowIndex, PartsSet partsSet) {
			partsSetList.set(rowIndex, partsSet);
			fireTableRowsUpdated(rowIndex, rowIndex);
		}

		public List<PartsSet> getPartsSetList() {
			return new ArrayList<PartsSet>(partsSetList);
		}

		public void setPartsSetList(List<PartsSet> partsSetList) {
			if (partsSetList == null) {
				partsSetList = Collections.emptyList();
			}
			this.partsSetList = new ArrayList<PartsSet>(partsSetList);
			fireTableDataChanged();
		}
	}

	/**
	 * パーツセットの選択および保存を行うためのコールバック.
	 */
	public interface FavoriteManageCallback {

		/**
		 * 引数で指定されたパーツセットを表示する.
		 * 
		 * @param partsSet
		 */
		void selectFavorites(PartsSet partsSet);

		/**
		 * 指定したキャラクターデータのお気に入りを保存する.<br>
		 * presetを変更した場合はcharacter.xmlを更新するためにsavePreset引数をtrueとする.<br>
		 * 
		 * @param characterData
		 *            お気に入りを保存するキャラクターデータ
		 * @param savePreset
		 *            character.xmlを更新する場合(presetの更新)
		 */
		void updateFavorites(CharacterData characterData, boolean savePreset);
	}

	public ManageFavoriteDialog(JFrame parent, CharacterData characterData) {
		super(parent, false);
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
		
		Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		setTitle(strings.getProperty("manageFavorites"));
		
		this.characterData = characterData;
		
		characterData.getPartsSets();
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		partsSetListModel = new PartsSetListTableModel();
		partsSetList = new JTable(partsSetListModel);
		partsSetList.setRowSelectionAllowed(true);
		partsSetList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		partsSetList.setTableHeader(null);
		partsSetList.getColumnModel().getColumn(1).setMaxWidth(150);
		
		partsSetList.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						updateButtonUI();
					}
				});

		actSelect = new AbstractAction(strings.getProperty("select")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSelect();
			}
		};
		actDelete = new AbstractAction(strings.getProperty("remove")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onDelete();
			}
		};
		actRename = new AbstractAction(strings.getProperty("rename")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onRename();
			}
		};

		JPanel buttonsPanel = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		buttonsPanel.setLayout(gb);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		buttonsPanel.add(new JButton(actSelect), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		buttonsPanel.add(new JButton(actDelete), gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		buttonsPanel.add(new JButton(actRename), gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weighty = 1.;
		buttonsPanel.add(Box.createGlue(), gbc);

		
		JPanel panel2 = new JPanel();
		panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 42));
		panel2.setLayout(new BorderLayout());
		Action actCancel = new AbstractAction(strings.getProperty("close")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};
		JButton btnClose = new JButton(actCancel);
		panel2.add(btnClose, BorderLayout.EAST);

		JScrollPane scr = new JScrollPane(partsSetList);
		scr.setBorder(BorderFactory.createEtchedBorder());
		scr.setPreferredSize(new Dimension(300, 150));
		
		contentPane.add(scr, BorderLayout.CENTER);
		contentPane.add(buttonsPanel, BorderLayout.EAST);
		contentPane.add(panel2, BorderLayout.SOUTH);

		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(btnClose);
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteFav");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeManageFavoriteDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeManageFavoriteDialog");
		am.put("deleteFav", actDelete);
		am.put("closeManageFavoriteDialog", actCancel);

		setSize(400, 500);
		setLocationRelativeTo(parent);
		
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(new JMenuItem(actSelect));
		popupMenu.add(new JMenuItem(actRename));
		popupMenu.add(new JMenuItem(actDelete));

		partsSetList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					onSelect();
				}
			}
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					// 右クリックによる選択
					Point pt = e.getPoint();
					int rowIndex = partsSetList.rowAtPoint(pt);
					if (rowIndex >= 0) {
						int[] selrows = partsSetList.getSelectedRows();
						if (!Arrays.asList(selrows).contains(rowIndex)) {
							// 現在の選択行以外を右クリックした場合、その行を選択行とする.
							ListSelectionModel selModel = partsSetList
									.getSelectionModel();
							selModel.setSelectionInterval(rowIndex, rowIndex);
						}
					}
				}
				evaluatePopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				evaluatePopup(e);
			}
			private void evaluatePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popupMenu.show(partsSetList, e.getX(), e.getY());
				}
			}
		});

		initListModel();

		updateButtonUI();
	}
	
	/**
	 * 現在のキャラクターデータの最新の状態でお気に入り一覧を更新する.
	 */
	public void initListModel() {
		ArrayList<PartsSet> partssets = new ArrayList<PartsSet>();
		for (PartsSet partsset : characterData.getPartsSets().values()) {
			partssets.add(partsset);
		}
		Collections.sort(partssets, PartsSet.DEFAULT_COMPARATOR);
		partsSetListModel.setPartsSetList(partssets);
	}

	protected void updateButtonUI() {
		int[] rows = partsSetList.getSelectedRows();
		actSelect.setEnabled(rows.length == 1);
		actRename.setEnabled(rows.length == 1);
		actDelete.setEnabled(rows.length >= 1);
	}

	/**
	 * 選択されている「お気に入り」のパーツセットの一覧を取得する.<br>
	 * プリセットが選択されている場合、それは除外される.<br>
	 * 
	 * @param beep
	 *            プリセットが選択されている場合にビープを鳴らすか？
	 * @return お気に入りのパーツセットのリスト、選択がなければ空のリスト.
	 */
	protected List<PartsSet> getSelectedPartsSet() {
		ArrayList<PartsSet> selectedPartsSet = new ArrayList<PartsSet>();
		int[] rows = partsSetList.getSelectedRows();
		for (int row : rows) {
			PartsSet partsSet = partsSetListModel.getRow(row);
			selectedPartsSet.add(partsSet);
		}
		return selectedPartsSet;
	}

	/**
	 * お気に入りの削除
	 */
	protected void onDelete() {
		List<PartsSet> removePartsSet = getSelectedPartsSet();
		if (removePartsSet.isEmpty() || callback == null) {
			return;
		}

		// 削除の確認ダイアログ
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		String msg = strings.getProperty("favorite.remove.confirm");
		JOptionPane optionPane = new JOptionPane(msg,
				JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION) {
			private static final long serialVersionUID = 1L;
			@Override
			public void selectInitialValue() {
				String noBtnCaption = UIManager
						.getString("OptionPane.noButtonText");
				for (JButton btn : UIHelper.getInstance().getDescendantOfClass(
						JButton.class, this)) {
					if (btn.getText().equals(noBtnCaption)) {
						// 「いいえ」ボタンにフォーカスを設定
						btn.requestFocus();
					}
				}
			}
		};
		JDialog dlg = optionPane.createDialog(ManageFavoriteDialog.this,
				strings.getProperty("confirm.remove"));
		dlg.setVisible(true);
		Object ret = optionPane.getValue();
		if (ret == null || ((Number) ret).intValue() != JOptionPane.YES_OPTION) {
			return;
		}

		// お気に入りリストから削除する.
		boolean dirty = false;
		boolean deletePreset = false;
		Map<String, PartsSet> partsSetMap = characterData.getPartsSets();
		for (PartsSet partsSet : removePartsSet) {
			Iterator<Map.Entry<String, PartsSet>> ite = partsSetMap.entrySet().iterator();
			while (ite.hasNext()) {
				Map.Entry<String, PartsSet> entry = ite.next();
				PartsSet target = entry.getValue();
				if (target == partsSet) {
					dirty = true;
					if (target.isPresetParts()) {
						// presetを削除した場合はcharacter.xmlの更新が必要
						deletePreset = true;
					}
					ite.remove();
				}
			}
		}
		if (dirty) {
			callback.updateFavorites(characterData, deletePreset);
			initListModel();
		}
	}
	
	/**
	 * お気に入りのリネーム
	 */
	protected void onRename() {
		int row = partsSetList.getSelectedRow();
		if (row < 0 || callback == null) {
			return;
		}
		PartsSet partsSet = partsSetListModel.getRow(row);

		Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		String localizedName = JOptionPane.showInputDialog(this,
				strings.getProperty("inputName"), partsSet.getLocalizedName());
		if (localizedName != null) {
			partsSet.setLocalizedName(localizedName);
			callback.updateFavorites(characterData, partsSet.isPresetParts());
			initListModel();
		}
	}
	
	/**
	 * 選択したお気に入りを表示する.
	 */
	protected void onSelect() {
		int row = partsSetList.getSelectedRow();
		if (row < 0) {
			return;
		}
		PartsSet partsSet = partsSetListModel.getRow(row);
		if (callback != null) {
			callback.selectFavorites(partsSet);
		}
	}

	protected void onClose() {
		dispose();
	}
	
	public void setFavoriteManageCallback(FavoriteManageCallback callback) {
		this.callback = callback;
	}

	public FavoriteManageCallback getFavoriteManageCallback() {
		return callback;
	}
}
