package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import charactermanaj.model.AppConfig;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.PartsSpecResolver;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.UIHelper;

/**
 * 各パーツの選択パネル(カテゴリ別)
 * @author seraphy
 */
public class ImageSelectPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	protected static final String STRINGS_RESOURCE = "languages/imageselectpanel";
	

	/**
	 * 変更通知を受けるリスナ
	 * @author seraphy
	 */
	public interface ImageSelectPanelListener extends EventListener {

		/**
		 * 選択が変更された場合
		 * @param event
		 */
		void onSelectChange(ImageSelectPanelEvent event);
		
		/**
		 * アイテムが選択された場合
		 * @param event
		 */
		void onChange(ImageSelectPanelEvent event);
		
		/**
		 * 色変更ボタンが押された場合
		 * @param event
		 */
		void onChangeColor(ImageSelectPanelEvent event);
		
		/**
		 * 設定ボタンが押された場合
		 * @param event
		 */
		void onPreferences(ImageSelectPanelEvent event);
		
		/**
		 * タイトルがクリックされた場合
		 * @param event
		 */
		void onTitleClick(ImageSelectPanelEvent event);

		/**
		 * タイトルがクリックされた場合
		 * @param event
		 */
		void onTitleDblClick(ImageSelectPanelEvent event);
	};

	
	/**
	 * 変更通知イベント
	 * @author seraphy
	 */
	public static class ImageSelectPanelEvent extends EventObject {
		
		private static final long serialVersionUID = 1L;
		
		public ImageSelectPanelEvent(ImageSelectPanel src) {
			super(src);
		}
		
		public ImageSelectPanel getImageSelectPanel() {
			return (ImageSelectPanel) getSource();
		}
	}

	/**
	 * 表示モード
	 * @author seraphy
	 */
	public enum DisplayMode {
		/**
		 * 最小化モード
		 */
		MINIMIZED,

		/**
		 * 通常モード
		 */
		NORMAL,
		
		/**
		 * 最大サイズフリーモード
		 */
		EXPANDED
	}
	
	/**
	 * パネルノ拡大・縮小時のステップサイズ
	 */
	private static final int rowStep = 2;

	/**
	 * 変更通知を受けるリスナー
	 */
	private final LinkedList<ImageSelectPanelListener> listeners = new LinkedList<ImageSelectPanelListener>();
	
	/**
	 * リストの一行の高さ
	 */
	private final int rowHeight;
	
	/**
	 * パネルの最小高さ (ボーダー上限 + ヘッダ行の高さ)
	 */
	private final int minHeight;
	
	/**
	 * 現在の表示行数
	 */
	private int numOfVisibleRows;
	
	/**
	 * 最小化モードであるか?
	 */
	private DisplayMode displayMode;
	
	/**
	 * パーツ情報ソース
	 */
	private PartsSpecResolver partsSpecResolver;
	
	/**
	 * パーツ選択テーブル
	 */
	private final JTable partsSelectTable;
	
	/**
	 * パーツ選択テーブルモデル
	 */
	private final PartsSelectListModel partsSelectTableModel;
	
	/**
	 * 選択中のアイテム(複数選択の場合はフォーカスされているもの)、もしくはnull
	 */
	private PartsIdentifier selectedPartsIdentifier;
	
	/**
	 * 選択中のアイテムのリスト(順序あり)、もしくは空
	 */
	private List<PartsIdentifier> selectedPartsIdentifiers = Collections.emptyList();
	
	/**
	 * このパネルが対象とするカテゴリ情報
	 */
	private final PartsCategory partsCategory;
	

	/**
	 * イメージ選択パネルを構築する
	 * @param partsCategory パーツカテゴリ
	 * @param partsSpecResolver キャラクターデータ
	 */
	public ImageSelectPanel(final PartsCategory partsCategory, final PartsSpecResolver partsSpecResolver) {
		if (partsCategory == null || partsSpecResolver == null) {
			throw new IllegalArgumentException();
		}
		this.partsCategory = partsCategory;
		this.partsSpecResolver = partsSpecResolver;

		setLayout(new BorderLayout());
		
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3),
				BorderFactory.createCompoundBorder(
						BorderFactory.createEtchedBorder(),
						BorderFactory.createEmptyBorder(3, 3, 3, 3))
					)
				);

		partsSelectTableModel = new PartsSelectListModel(partsCategory);

		final DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
		TableColumn checkColumn = new TableColumn(0, 32);
		checkColumn.setMaxWidth(42);
		columnModel.addColumn(checkColumn);
		columnModel.addColumn(new TableColumn(1, 100));

		final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					onSelectChange(new ImageSelectPanelEvent(ImageSelectPanel.this));
				}
			}
		});

		AppConfig appConfig = AppConfig.getInstance();
		
		final Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);
		
		final Color selectedItemColor = appConfig.getCheckedItemBgColor();
		
		partsSelectTable = new JTable(partsSelectTableModel, columnModel, selectionModel) {
			private static final long serialVersionUID = 1L;
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				if (isCellSelected(row, column) && hasFocus()) {
					// フォーカスのあるセル選択の背景色
					comp.setBackground(getSelectionBackground());
				} else {
					// フォーカスのないセル選択行
					Boolean chk = (Boolean) getModel().getValueAt(row, 0);
					comp.setForeground(getForeground());
					if (chk.booleanValue()) {
						// チェック済みの場合の背景色
						comp.setBackground(selectedItemColor);
					} else {
						// 通常の背景色
						comp.setBackground(getBackground());
					}
				}
				return comp;
			}
			@Override
			public String getToolTipText(MouseEvent event) {
				// マウスが置かれている行のツールチップとしてパーツ名を表示する.
				int row = rowAtPoint(event.getPoint());
				int mx = partsSelectTableModel.getRowCount();
				if (row >= 0 && row < mx) {
					PartsSelectRow rowModel = partsSelectTableModel.getRow(row);
					PartsIdentifier partsIdentifier = rowModel.getPartsIdentifier();
					PartsSpec partsSpec = partsSpecResolver.getPartsSpec(partsIdentifier);
					String suffix = "";
					if (partsSpec != null) {
						// パーツの作者名とバージョンがあれば、それを末尾につけて表示する.
						String author = partsSpec.getAuthor();
						double version = partsSpec.getVersion();
						if (author != null) {
							if (version > 0) {
								suffix = " (" + author + " " + version + ")";
							} else {
								suffix = " (" + author + ")";
							}
						}
					}
					return partsIdentifier.getLocalizedPartsName() + suffix;
				}
				return null;
			}
		};
		partsSelectTable.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				partsSelectTable.repaint();
			}
			@Override
			public void focusLost(FocusEvent e) {
				partsSelectTable.repaint();
			}
		});
		final JPopupMenu partsSelectTablePopupMenu = new JPopupMenu();
		Action actDeselectAll = new AbstractAction(
				strings.getProperty("popupmenu.deselectall")) {
			private static final long serialVersionUID = 9132032971228670868L;
			public void actionPerformed(ActionEvent e) {
				deselectAll();
			}
		};
		partsSelectTablePopupMenu.add(actDeselectAll);
		
		partsSelectTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				evaluatePopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				evaluatePopup(e);
			}
			private void evaluatePopup(MouseEvent e) {
				if ((partsCategory.isMultipleSelectable() || isDeselectableSingleCategory())
						&& e.isPopupTrigger()) {
					partsSelectTablePopupMenu.show(partsSelectTable, e.getX(), e.getY());
				}
			}
		});
		
		partsSelectTableModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE) {
					onChange(new ImageSelectPanelEvent(ImageSelectPanel.this));
				}
			}
		});
		partsSelectTable.setSelectionBackground(appConfig.getSelectedItemBgColor());
		if (partsCategory.isMultipleSelectable()) {
			partsSelectTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		} else {
			partsSelectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		partsSelectTable.setRowSelectionAllowed(true);
		partsSelectTable.setTableHeader(null);
		partsSelectTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		partsSelectTable.setShowVerticalLines(false);
		partsSelectTable.setShowHorizontalLines(false);

        InputMap im = partsSelectTable.getInputMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleCheck");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "resetCheck");
		ActionMap am = partsSelectTable.getActionMap();
		am.put("toggleCheck", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = partsSelectTable.getSelectedRows();
				boolean[] checks = partsSelectTableModel.getChecks(selectedRows);
				int checkedCount = 0;
				for (boolean checked : checks) {
					if (checked) {
						checkedCount++;
					}
				}
				if (checks.length == checkedCount) {
					// 選択しているアイテムのすべてがチェック済みである
					partsSelectTableModel.setChecks(false, selectedRows);
				} else {
					// 選択しているアイテムの一部もしくは全部がチェックされていない
					partsSelectTableModel.setChecks(true, selectedRows);
				}
			}
		});
		am.put("resetCheck", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				partsSelectTableModel.setChecks(false, partsSelectTable.getSelectedRows());
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(partsSelectTable);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		UIHelper uiUtl = UIHelper.getInstance();
		JButton leftBtn = uiUtl.createTransparentButton("icons/left.png", "icons/left2.png");
		JButton rightBtn = uiUtl.createTransparentButton("icons/right.png", "icons/right2.png");
		JButton colorBtn = uiUtl.createTransparentButton("icons/color.png", "icons/color2.png");
		JButton configBtn = uiUtl.createTransparentButton("icons/config.png", "icons/config2.png");

		leftBtn.setToolTipText(strings.getProperty("tooltip.shrink"));
		rightBtn.setToolTipText(strings.getProperty("tooltip.expand"));
		colorBtn.setToolTipText(strings.getProperty("tooltip.color"));
		configBtn.setToolTipText(strings.getProperty("tooltip.config"));
		
		leftBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isMinimizeMode()) {
					setMinimizeMode(false);
				} else {
					shrink();
				}
			}
		});
		rightBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isMinimizeMode()) {
					setMinimizeMode(false);
				} else {
					expand();
				}
			}
		});
		colorBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onChangeColor();
			}
		});
		configBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onPreferences();
			}
		});
		
		
		JPanel btnPanelGrp = new JPanel(new BorderLayout());

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(leftBtn);
		toolBar.add(rightBtn);
		toolBar.add(colorBtn);
		//toolBar.add(configBtn); // 設定ボタン (現在は非表示)

		btnPanelGrp.add(toolBar, BorderLayout.NORTH);

		if (partsCategory.isMultipleSelectable()) {
			UIHelper uiUty = UIHelper.getInstance();
			JButton upBtn = uiUty.createTransparentButton("icons/arrow_up.png", "icons/arrow_up2.png");
			JButton downBtn = uiUty.createTransparentButton("icons/arrow_down.png", "icons/arrow_down2.png");
			JButton sortBtn = uiUty.createTransparentButton("icons/sort.png", "icons/sort2.png");

			upBtn.setToolTipText(strings.getProperty("tooltip.up"));
			downBtn.setToolTipText(strings.getProperty("tooltip.down"));
			sortBtn.setToolTipText(strings.getProperty("tooltip.sort"));
			
			upBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onUp();
				}
			});
			downBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onDown();
				}
			});
			sortBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onSort();
				}
			});
			
			JToolBar toolBar2 = new JToolBar();
			toolBar2.setFloatable(false);
			toolBar2.add(upBtn);
			toolBar2.add(downBtn);
			toolBar2.add(sortBtn);
			btnPanelGrp.add(toolBar2, BorderLayout.SOUTH);
		}
		
		JPanel header = new JPanel(new BorderLayout());
		header.add(btnPanelGrp, BorderLayout.EAST);
		final JLabel title = new JLabel(" " + partsCategory.getLocalizedCategoryName() + " ");
		Font font = title.getFont();
		title.setFont(font.deriveFont(Font.BOLD));
		final Color defaultTitleColor = title.getForeground();
		final Color hilightColor = appConfig.getSelectPanelTitleColor();
		
		title.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// 正確に2回
					onTitleDblClick();
				} else if (e.getClickCount() == 1) {
					// 正確に1回
					onTitleClick();
				}
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				title.setForeground(hilightColor);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				title.setForeground(defaultTitleColor);
			}
		});
		
		header.add(title, BorderLayout.CENTER);
		
		add(header, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		rowHeight = partsSelectTable.getRowHeight();
		
		// パネルの最小高さ (ボーダー上下 + ヘッダ行高さ)
		Insets insets = getInsets();
		minHeight = header.getPreferredSize().height + insets.top + insets.bottom;
		
		// デフォルトのパネル幅を設定する.
		Dimension dim = new Dimension(200, 200);
		setPreferredSize(dim);

		// パネルの初期サイズ
		numOfVisibleRows = partsCategory.getVisibleRows();
		setDisplayMode(DisplayMode.NORMAL);
	}
	
	/**
	 * 表示行数から推奨のパネル高さを求める.<br>
	 * パネル高さは1行の高さ x 表示行数 + ヘッダ + ボーダー上下である.<br>
	 * @param numOfVisibleRows 表示行数
	 * @return 推奨のパネル高さ
	 */
	protected int calcPreferredHeight(int numOfVisibleRows) {
		return minHeight + Math.max(0, rowHeight * numOfVisibleRows);
	}
	
	/**
	 * パーツをパネルにロードします.<br>
	 * 既存の内容はリセットされたのち、現在の選択パーツ位置にスクロールします.<br>
	 */
	public void loadParts() {
		partsSelectTableModel.load(partsSpecResolver.getPartsSpecMap(partsCategory).keySet());
		scrollToSelectedRow();
	}
	
	/**
	 * このイメージ選択パネルの該当カテゴリを返します.<br>
	 * @return カテゴリ
	 */
	public PartsCategory getPartsCategory() {
		return partsCategory;
	}

	/**
	 * 現在選択している、すべてのパーツの選択を解除します.<br>
	 * 単一選択カテゴリであるかどうかを問わず、常にすべて解除されます.<br>
	 * 変更イベントが発生します.<br>
	 */
	public void deselectAll() {
		PartsSelectListModel rowModelList = (PartsSelectListModel) partsSelectTable.getModel();
		ArrayList<PartsSelectRow> rowModels = rowModelList.getRowModelList();

		// すべての選択を解除する.
		for (PartsSelectRow rowModel : rowModels) {
			rowModel.setChecked(false);
		}

		// コンポーネントではなく、モデルに対する直接変更であるため、イベントは発生しません.
		// そのため再描画させる必要があります.
		partsSelectTable.repaint();

		// アイテムの選択が変更されたことを通知する.
		onChange(new ImageSelectPanelEvent(ImageSelectPanel.this));
	}
	
	/**
	 * カテゴリのリストでパーツを選択しなおします.<br>
	 * 変更イベントは発生しません.<br>
	 * @param partsIdentifiers
	 */
	public void selectParts(Collection<PartsIdentifier> partsIdentifiers) {
		if (partsIdentifiers == null) {
			partsIdentifiers = Collections.emptyList();
		}
		PartsSelectListModel rowModelList = (PartsSelectListModel) partsSelectTable.getModel();
		ArrayList<PartsSelectRow> rowModels = rowModelList.getRowModelList();

		for (PartsSelectRow rowModel : rowModels) {
			rowModel.setChecked(false);
		}

		ArrayList<PartsIdentifier> partsIdentifiersBuf = new ArrayList<PartsIdentifier>(partsIdentifiers);
		Collections.reverse(partsIdentifiersBuf);
		
		for (PartsIdentifier partsIdentifier : partsIdentifiersBuf) {
			Iterator<PartsSelectRow> ite = rowModels.iterator();
			while (ite.hasNext()) {
				PartsSelectRow rowModel = ite.next();
				if (rowModel.getPartsIdentifier().equals(partsIdentifier)) {
					rowModel.setChecked(true);
					if (partsIdentifiersBuf.size() >= 2 && partsCategory.isMultipleSelectable()) {
						ite.remove();
						rowModels.add(0, rowModel);
					}
					break;
				}
			}
		}
		
		// 選択を保存する
		selectedPartsIdentifier = getSelectedPartsIdentifier();
		selectedPartsIdentifiers = getSelectedPartsIdentifiers();
		
		// コンポーネントではなく、モデルに対する直接変更であるため、イベントは発生しません.
		// そのため再描画させる必要があります.
		partsSelectTable.repaint();
		
		// あたらしく選択されたアイテムが表示されるようにスクロールします.
		scrollToSelectedRow();
	}
	
	/**
	 * カテゴリのリストで選択中のアイテムが見えるようにスクロールする.
	 */
	public void scrollToSelectedRow() {
		PartsSelectListModel rowModelList = (PartsSelectListModel) partsSelectTable.getModel();
		ArrayList<PartsSelectRow> rowModels = rowModelList.getRowModelList();
		int mx = rowModels.size();
		for (int row = 0; row < mx; row++) {
			if (rowModels.get(row).isChecked()) {
				Rectangle rct = partsSelectTable.getCellRect(row, 0, true);
				partsSelectTable.scrollRectToVisible(rct);
				break;
			}
		}
	}

	/**
	 * カテゴリのパネルを最小表示.<br>
	 * 最小化の場合は、高さは表示行数ゼロとなりタイトルとボーダーだけとなる.<br>
	 * 最小化解除した場合は、標準高さは既定、最大サイズはフリーとなる.<br>
	 * @param shrinkMode 最小化モードならばtrue、フリーモードならばfalse
	 */
	public void setMinimizeMode(boolean minimizeMode) {
		setDisplayMode(minimizeMode ? DisplayMode.MINIMIZED : DisplayMode.EXPANDED);
	}

	/**
	 * 表示モードを切り替えパネルサイズを調整する.<br>
	 * @param displayMode 表示モード
	 */
	public void setDisplayMode(DisplayMode displayMode) {
		if (displayMode == null) {
			displayMode = DisplayMode.NORMAL;
		}

		Dimension siz = getPreferredSize();
		Dimension sizMax = getMaximumSize();

		if (displayMode == DisplayMode.MINIMIZED) {
			int preferredHeight = calcPreferredHeight(0);
			siz.height = preferredHeight;
			sizMax.height = preferredHeight;

		} else if (displayMode == DisplayMode.EXPANDED) {
			int preferredHeight = calcPreferredHeight(numOfVisibleRows);
			siz.height = preferredHeight;
			sizMax.height = Integer.MAX_VALUE;

		} else {
			// DisplayMode.NORMALの場合
			int preferredHeight = calcPreferredHeight(numOfVisibleRows);
			siz.height = preferredHeight;
			sizMax.height = preferredHeight;
		}

		setPreferredSize(siz);
		setMinimumSize(siz);
		setMaximumSize(sizMax);
		
		this.displayMode = displayMode;
		revalidate();
	}
	
	public DisplayMode getDisplayMode() {
		return displayMode;
	}
	
	public boolean isMinimizeMode() {
		return displayMode == DisplayMode.MINIMIZED;
	}
	
	/**
	 * カテゴリのパネルを縮小する.<br>
	 * ただし、ヘッダ部よりは小さくならない.<br>
	 * 現在の表示モードが標準でなければ縮小せず標準に戻す.<br>
	 */
	public void shrink() {
		if (displayMode == DisplayMode.NORMAL) {
			// 表示行数を減ずる
			numOfVisibleRows = Math.max(0, numOfVisibleRows - rowStep);
		}
		// 通常モードの適用
		setDisplayMode(DisplayMode.NORMAL);
	}

	/**
	 * カテゴリのパネルを拡大する.<br>
	 * 現在の表示モードが標準でなければ拡大前せず標準に戻す.<br>
	 */
	public void expand() {
		if (displayMode == DisplayMode.NORMAL) {
			// 表示行数を加算する
			numOfVisibleRows += Math.max(0, rowStep);
		}
		// 通常モードの適用
		setDisplayMode(DisplayMode.NORMAL);
	}
	
	public void addImageSelectListener(ImageSelectPanelListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		listeners.add(listener);
	}
	
	public void removeImageSelectListener(ImageSelectPanelListener listener) {
		listeners.remove(listener);
	}
	
	public void requestListFocus() {
		partsSelectTable.requestFocus();
	}

	/**
	 * 指定したパーツ識別子にフォーカスを当てます.<br>
	 * 必要に応じてスクロールされます.<br>
	 * 該当するパーツ識別子がなければ何もしません.<br>
	 * @param partsIdentifier パーツ識別子
	 */
	public void setSelection(PartsIdentifier partsIdentifier) {
		if (partsIdentifier == null) {
			return;
		}
		PartsCategory partsCategory = partsIdentifier.getPartsCategory();
		if (!this.partsCategory.equals(partsCategory)) {
			return;
		}

		ArrayList<PartsSelectRow> rowModelList = ((PartsSelectListModel) partsSelectTable.getModel()).getRowModelList();
		int mx = rowModelList.size();
		for (int idx = 0; idx < mx; idx++) {
			PartsSelectRow partsSelectRow = rowModelList.get(idx);
			if (partsSelectRow.getPartsIdentifier().equals(partsIdentifier)) {
				partsSelectTable.getSelectionModel().setSelectionInterval(idx, idx);
				Rectangle rct = partsSelectTable.getCellRect(idx, 0, true);
				partsSelectTable.scrollRectToVisible(rct);
				partsSelectTable.requestFocus();
				return;
			}
		}
	}
	
	/**
	 * フォーカスのあるアイテムを1つ上に移動します.
	 */
	protected void onUp() {
		int selRow = partsSelectTable.getSelectedRow();
		if (selRow < 0) {
			return;
		}
		if (selRow > 0) {
			ArrayList<PartsSelectRow> rowModelList = ((PartsSelectListModel) partsSelectTable.getModel()).getRowModelList();
			PartsSelectRow rowModel = rowModelList.get(selRow);
			rowModelList.remove(selRow);
			rowModelList.add(selRow - 1, rowModel);
			partsSelectTable.setRowSelectionInterval(selRow - 1, selRow - 1);
			Rectangle rct = partsSelectTable.getCellRect(selRow - 1, 0, true);
			partsSelectTable.scrollRectToVisible(rct);
			onChange(new ImageSelectPanelEvent(this));
		}
		partsSelectTable.repaint();
		partsSelectTable.requestFocus();
	}

	/**
	 * フォーカスのあるアイテムを1つ下に移動します.
	 */
	protected void onDown() {
		int selRow = partsSelectTable.getSelectedRow();
		if (selRow < 0) {
			return;
		}
		int mx = partsSelectTable.getRowCount();
		if (selRow < mx - 1) {
			ArrayList<PartsSelectRow> rowModelList = ((PartsSelectListModel) partsSelectTable.getModel()).getRowModelList();
			PartsSelectRow rowModel = rowModelList.get(selRow);
			rowModelList.remove(selRow);
			rowModelList.add(selRow + 1, rowModel);
			partsSelectTable.setRowSelectionInterval(selRow + 1, selRow + 1);
			Rectangle rct = partsSelectTable.getCellRect(selRow + 1, 0, true);
			partsSelectTable.scrollRectToVisible(rct);
			onChange(new ImageSelectPanelEvent(this));
		}
		partsSelectTable.repaint();
		partsSelectTable.requestFocus();
	}
	
	/**
	 * 選択中のアイテムを選択順序を維持したまま上側に、それ以外は名前順で下側に集めるようにソートします.<br>
	 */
	protected void onSort() {
		if (partsSelectTable.getRowCount() > 0) {
			partsSelectTableModel.sort();
			partsSelectTable.setRowSelectionInterval(0, 0);
			Rectangle rct = partsSelectTable.getCellRect(0, 0, true);
			partsSelectTable.scrollRectToVisible(rct);
			partsSelectTable.repaint();
		}
		partsSelectTable.requestFocus();
	}

	/**
	 * タイトルがクリックされた場合
	 */
	protected void onTitleClick() {
		ImageSelectPanelEvent event = new ImageSelectPanelEvent(this);
		for (ImageSelectPanelListener listener : listeners) {
			listener.onTitleClick(event);
		}
	}
	
	/**
	 * タイトルがダブルクリックされた場合
	 */
	protected void onTitleDblClick() {
		ImageSelectPanelEvent event = new ImageSelectPanelEvent(this);
		for (ImageSelectPanelListener listener : listeners) {
			listener.onTitleDblClick(event);
		}
	}
	
	/**
	 * カラー変更ボタンが押下された場合
	 * @param event
	 */
	protected void onChangeColor() {
		ImageSelectPanelEvent event = new ImageSelectPanelEvent(this);
		for (ImageSelectPanelListener listener : listeners) {
			listener.onChangeColor(event);
		}
	}

	/**
	 * 設定ボタンが押下された場合
	 * @param event
	 */
	protected void onPreferences() {
		ImageSelectPanelEvent event = new ImageSelectPanelEvent(this);
		for (ImageSelectPanelListener listener : listeners) {
			listener.onPreferences(event);
		}
	}

	/**
	 * アイテムのチェック状態が変更された場合.
	 * @param event
	 */
	protected void onChange(ImageSelectPanelEvent event) {
		List<PartsIdentifier> selectedNews = getSelectedPartsIdentifiers();
		if (!selectedNews.equals(selectedPartsIdentifiers)) {
			selectedPartsIdentifiers = selectedNews;
			for (ImageSelectPanelListener listener : listeners) {
				listener.onChange(event);
			}
			onSelectChange(event);
		}
	}
	
	/**
	 * アイテムの選択(フォーカス)が変更された場合.
	 * @param event
	 */
	protected void onSelectChange(ImageSelectPanelEvent event) {
		PartsIdentifier selectedNew = getSelectedPartsIdentifier();
		if (!PartsIdentifier.equals(selectedNew, selectedPartsIdentifier)) {
			selectedPartsIdentifier = selectedNew;
			for (ImageSelectPanelListener listener : listeners) {
				listener.onSelectChange(event);
			}
		}
	}
	
	/**
	 * 使用中のアイテムの一覧を返す.(選択順)<br>
	 * @return 使用中のアイテムの一覧.(選択順)、ひとつもなければ空
	 */
	public List<PartsIdentifier> getSelectedPartsIdentifiers() {
		return partsSelectTableModel.getSelectedPartsIdentifiers();
	}
	
	/**
	 * 使用中のアイテムを返す.<br>
	 * 複数選択可能である場合は、使用中のアイテムでフォーカスがある最初のアイテムを返す.<br>
	 * 単一選択の場合は、最初の使用中アイテムを返す.<br>
	 * 複数選択可能で、使用中のアイテムにひとつもフォーカスがあたってない場合は、
	 * 最初の使用中アイテムを返す.<br>
	 * 使用中アイテムがなければnullを返す.
	 * @return 使用中アイテム、もしくはnull
	 */
	public PartsIdentifier getSelectedPartsIdentifier() {
		
		// フォーカスがあたっていて、且つ、チェック状態のアイテムを上から順に走査し、
		// 該当があれば、最初のものを返す.
		int[] selRows = partsSelectTable.getSelectedRows();
		Arrays.sort(selRows);
		for (int selRow : selRows) {
			PartsSelectRow row = partsSelectTableModel.getRow(selRow);
			if (row.isChecked()) {
				return row.getPartsIdentifier();
			}
		}

		// チェック状態のアイテムの最初のものを返す.
		List<PartsIdentifier> checkedRows = getSelectedPartsIdentifiers();
		if (checkedRows.size() > 0) {
			return checkedRows.get(0);
		}

		// 該当なし
		return null;
	}
	
	/**
	 * 選択選択パーツカテゴリの選択解除を許可するか?<br>
	 * @return 許可する場合はtrue
	 */
	public boolean isDeselectableSingleCategory() {
		return partsSelectTableModel.isDeselectableSingleCategory();
	}
	
	/**
	 * 選択選択パーツカテゴリの選択解除を許可するか設定する.<br>
	 * @param deselectable 許可する場合はtrue
	 */
	public void setDeselectableSingleCategory(boolean deselectable) {
		partsSelectTableModel.setDeselectableSingleCategory(deselectable);
	}
}


/**
 * リストの行モデル.<br>
 * パーツデータ、表示名と使用中フラグを管理する.
 * @author seraphy
 */
final class PartsSelectRow implements Serializable, Comparable<PartsSelectRow> {
	
	private static final long serialVersionUID = 5732273802364827L;

	private PartsIdentifier partsIdentifier;
	
	private boolean checked;
	
	private int displayOrder;
	
	public PartsSelectRow(final PartsIdentifier partsIdentifier, final boolean checked) {
		this.partsIdentifier = partsIdentifier;
		this.checked = checked;
	}
	
	/**
	 * 選択されているものを上、そうでないものを下に振り分ける。
	 * 選択されているもの同士、選択されていないもの同士は、互いのディスプレイ順でソートされる.<br>
	 * 選択されているもの同士、選択されていないもの同士で、且つ、同一のディスプレイ順序であればパーツの表示名順でソートされる.<br>
	 * @param o 対象
	 * @return 比較結果
	 */
	public int compareTo(PartsSelectRow o) {
		int ret = (checked == o.checked) ? 0 : (checked ? -1 : 1);
		if (ret == 0 && checked) {
			ret = displayOrder - o.displayOrder;
		}
		if (ret == 0) {
			ret = partsIdentifier.compareTo(o.partsIdentifier);
		}
		return ret;
	}
	
	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}
	
	public int getDisplayOrder() {
		return this.displayOrder;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof PartsSelectRow) {
			return this.compareTo((PartsSelectRow) obj) == 0;
		}
		return false;
	}
	
	public int hashCode() {
		return partsIdentifier.hashCode();
	}
	
	public PartsIdentifier getPartsIdentifier() {
		return partsIdentifier;
	}
	
	/**
	 * {@link PartsIdentifier#getLocalizedPartsName()}に委譲します.
	 * @return パーツ名
	 */
	public String getPartsName() {
		return partsIdentifier.getLocalizedPartsName();
	}
	
	public boolean isChecked() {
		return checked;
	}
	
	public void setChecked(boolean checked) {
		this.checked = checked;
	}
}


/**
 * リストのモデル
 * @author seraphy
 */
class PartsSelectListModel extends AbstractTableModel {

	private static final long serialVersionUID = 7604828023134579608L;

	private PartsCategory partsCategory;
	
	private ArrayList<PartsSelectRow> partsSelectRowList; 

	/**
	 * カテゴリが複数パーツでない場合でも選択解除を許可するフラグ.
	 */
	private boolean deselectableSingleCategory;

	public PartsSelectListModel(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		this.partsSelectRowList = new ArrayList<PartsSelectRow>();
		this.partsCategory = partsCategory;
	}
	
	public void load(Collection<PartsIdentifier> partsIdentifiers) {
		if (partsIdentifiers == null) {
			throw new IllegalArgumentException();
		}
		
		// 現在選択されているパーツを保存する
		HashMap<PartsIdentifier, Integer> selectedPartsIdentifiers = new HashMap<PartsIdentifier, Integer>();
		for (PartsIdentifier partsIdentifier : getSelectedPartsIdentifiers()) {
			selectedPartsIdentifiers.put(partsIdentifier, selectedPartsIdentifiers.size());
		}
		
		// パーツイメージマップからパーツ名を列挙する.
		ArrayList<PartsSelectRow> partsSelectList = new ArrayList<PartsSelectRow>();
		for (PartsIdentifier partsIdentifier : partsIdentifiers) {
			Integer selIndex = selectedPartsIdentifiers.get(partsIdentifier);
			PartsSelectRow rowModel = new PartsSelectRow(partsIdentifier, selIndex != null);
			// 選択されているものは、選択されているものの順序を維持する.それ以外は名前順でソートされる.
			int order = (selIndex != null) ? selIndex.intValue() : 0;
			rowModel.setDisplayOrder(order);
			partsSelectList.add(rowModel);
		}

		if (partsCategory.isMultipleSelectable()) {
			// パーツを選択有無(順序維持)・名前順に並び替える.
			Collections.sort(partsSelectList);
		} else {
			// 単一選択モード時はパーツ識別子でソートする.
			Collections.sort(partsSelectList, new Comparator<PartsSelectRow>() {
				public int compare(PartsSelectRow o1, PartsSelectRow o2) {
					return o1.getPartsIdentifier().compareTo(o2.getPartsIdentifier());
				}
			});
		}

		this.partsSelectRowList = partsSelectList;
		fireTableDataChanged();
	}
	
	/**
	 * 選択選択パーツカテゴリの選択解除を許可するか?<br>
	 * @return 許可する場合はtrue
	 */
	public boolean isDeselectableSingleCategory() {
		return deselectableSingleCategory;
	}
	
	/**
	 * 選択選択パーツカテゴリの選択解除を許可するか設定する.<br>
	 * @param deselectable 許可する場合はtrue
	 */
	public void setDeselectableSingleCategory(boolean deselectable) {
		this.deselectableSingleCategory = deselectable;
	}

	public PartsSelectRow getRow(int rowIndex) {
		return partsSelectRowList.get(rowIndex);
	}
	
	public ArrayList<PartsSelectRow> getRowModelList() {
		return this.partsSelectRowList;
	}
	
	public int getColumnCount() {
		// ヘッダは非表示のためヘッダ名は取得する必要なし.
		// col 0: 選択ボックス
		// col 1: パーツ表示名
		return 2;
	}
	
	public int getRowCount() {
		return partsSelectRowList.size();
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		PartsSelectRow rowModel = partsSelectRowList.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return Boolean.valueOf(rowModel.isChecked());
		case 1:
			return rowModel.getPartsName();
		default:
		}
		return "";
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return Boolean.class;
		case 1:
			return String.class;
		default:
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
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex != 0) {
			return;
		}
		PartsSelectRow rowModel = partsSelectRowList.get(rowIndex);
		boolean checked = ((Boolean) aValue).booleanValue();
		
		if (!checked && rowModel.isChecked() && !deselectableSingleCategory
				&& !partsCategory.isMultipleSelectable()) {
			// 複数選択が可能でない場合、現在選択中のチェックは一つしかないはずのため、これを外すことはしない。
			// ただし単一選択パーツカテゴリでの選択解除が許可されている場合を除く。
			return;
		}
		
		rowModel.setChecked(checked);

		// カテゴリが複数パーツ選択を許可しておらず、且つ、チェックをつけた場合、
		// すでにチェックされている他の パーツ行のチェックを外す必要がある。
		boolean unchecked = false;
		if (checked && !partsCategory.isMultipleSelectable()) {
			int mx = partsSelectRowList.size();
			for (int idx = 0; idx < mx; idx++) {
				if (idx != rowIndex) {
					PartsSelectRow otherRow = partsSelectRowList.get(idx);
					if (otherRow.isChecked()) {
						otherRow.setChecked(false);
						unchecked = true;
					}
				}
			}
		}
		if (!unchecked) {
			// 指定されたセルの変更のみなので単一変更を通知する.
			fireTableCellUpdated(rowIndex, columnIndex);
		} else {
			// 他のセルも変更されたので一括変更を通知する.
			fireTableDataChanged();
		}
	}
	
	/**
	 * 選択されているパーツを上に、それ以外を下に振り分ける.<br>
	 * それぞれはパーツの表示名順でソートされる.<br>
	 */
	public void sort() {
		int mx = partsSelectRowList.size();
		for (int idx = 0; idx < mx; idx++) {
			partsSelectRowList.get(idx).setDisplayOrder(idx);
		}
		Collections.sort(partsSelectRowList);
		fireTableDataChanged();
	}
	
	/**
	 * チェックされているパーツのパーツ識別子のリストを返す.<br>
	 * リストの順序はパーツの表示されている順序と等しい.<br>
	 * 選択がなければ空のリストが返される.
	 * @return  チェックされているパーツのパーツ識別子のリスト、もしくは空
	 */
	public List<PartsIdentifier> getSelectedPartsIdentifiers() {
		ArrayList<PartsIdentifier> selectedRows = new ArrayList<PartsIdentifier>();
		for (PartsSelectRow rowModel : partsSelectRowList) {
			if (rowModel.isChecked()) {
				selectedRows.add(rowModel.getPartsIdentifier());
			}
		}
		return selectedRows;
	}
	
	/**
	 * 指定したインデックスのパーツのチェック状態を返す.
	 * @param rowIndexes 調べるインデックスの配列
	 * @return 引数に対応したインデックスのチェック状態、nullまたは空の場合は空を返す
	 */
	public boolean[] getChecks(int[] rowIndexes) {
		if (rowIndexes == null) {
			rowIndexes = new int[0];
		}
		int mx = rowIndexes.length;
		boolean[] results = new boolean[mx];
		for (int idx = 0; idx < mx; idx++) {
			int rowIndex = rowIndexes[idx];
			PartsSelectRow row = partsSelectRowList.get(rowIndex);
			results[idx] = row.isChecked();
		}
		return results;
	}
	
	/**
	 * 指定したインデックスのチェック状態を設定する.
	 * @param checked チェックする場合はtrue、チェックを解除する場合はfalse
	 * @param selectedRows インデックスの配列、nullまたは空の場合は何もしない.
	 */
	public void setChecks(boolean checked, int[] selectedRows) {
		if (selectedRows == null || selectedRows.length == 0) {
			return;
		}
		ArrayList<Integer> affectRows = new ArrayList<Integer>();
		if (!checked) {
			// 選択解除
			if (!partsCategory.isMultipleSelectable()) {
				// 複数選択可能でない場合、選択はひとつしかないはずなので
				// クリアする必要はない。
				return;
			}
			// 選択を解除する.
			for (int selRow : selectedRows) {
				PartsSelectRow row = partsSelectRowList.get(selRow);
				if (row.isChecked()) {
					row.setChecked(false);
					affectRows.add(selRow);
				}
			}
		} else {
			// 選択
			if (partsCategory.isMultipleSelectable()) {
				// 複数選択可能であれば単純に選択を有効にする
				for (int selRow : selectedRows) {
					PartsSelectRow row = partsSelectRowList.get(selRow);
					if (!row.isChecked()) {
						row.setChecked(true);
						affectRows.add(selRow);
					}
				}
			} else {
				// 複数選択可能でない場合は最初のアイテムのみをチェックをつけ、
				// それ以外のチェックを外す.
				int selRow = selectedRows[0];
				PartsSelectRow row = partsSelectRowList.get(selRow);
				if (!row.isChecked()) {
					row.setChecked(true);
					affectRows.add(selRow);
					int mx = partsSelectRowList.size();
					for (int idx = 0; idx < mx; idx++) {
						PartsSelectRow otherRow = partsSelectRowList.get(idx);
						if (idx != selRow) {
							if (otherRow.isChecked()) {
								otherRow.setChecked(false);
								affectRows.add(idx);
							}
						}
					}
				}
			}
		}
		if (affectRows.isEmpty()) {
			// なにも変わりないのでイベントも発生しない.
			return;
		}
		// 変更された最初の行から最後の行までの範囲で変更を通知する.
		// (変更されていない中間も含まれる)
		int minIdx = 0;
		int maxIdx = 0;
		for (int idx : affectRows) {
			minIdx = Math.min(minIdx, idx);
			maxIdx = Math.max(maxIdx, idx);
		}
		fireTableRowsUpdated(minIdx, maxIdx);
	}
}

