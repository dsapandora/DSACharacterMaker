package charactermanaj.ui;

import static java.lang.Math.max;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.PNGFileImageHeader;
import charactermanaj.graphics.io.PNGFileImageHeaderReader;
import charactermanaj.model.AppConfig;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.PartsSpecResolver;
import charactermanaj.model.io.PartsImageCollectionParser;
import charactermanaj.model.io.PartsImageCollectionParser.PartsImageCollectionHandler;
import charactermanaj.ui.model.AbstractTableModelWithComboBoxModel;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 情報ダイアログを開く
 * @author seraphy
 */
public class InformationDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(InformationDialog.class.getName());
	
	protected static final String STRINGS_RESOURCE = "languages/informationdialog";

	private JTable informationTable;
	
	private InformationTableModel informationTableModel;
	
	private boolean modeOpen;

	public InformationDialog(JFrame parent, PartsSpecResolver partsSpecResolver, PartsSet partsSet) {
		super(parent, true);
		
		AppConfig appConfig = AppConfig.getInstance();
		modeOpen = appConfig.isInformationDialogOpenMethod();
		
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException("partsSpecResolver is null");
		}
		if (partsSet == null) {
			throw new IllegalArgumentException("partsSet is null");
		}
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});
		
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);
		
		setTitle(strings.getProperty("title"));
		
		informationTableModel = new InformationTableModel();
		
		final PNGFileImageHeaderReader pngHeaderReader = PNGFileImageHeaderReader.getInstance();
		
		PartsImageCollectionParser parser = new PartsImageCollectionParser(partsSpecResolver);
		parser.parse(partsSet, new PartsImageCollectionHandler() {
			public void detectImageSource(PartsIdentifier partsIdentifier, Layer layer,
					final ImageResource imageResource, ColorConvertParameter param) {
				
				AbstractAction act = new AbstractAction(strings.getProperty(modeOpen ? "btn.edit.open" : "btn.edit.edit")) {
					private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e) {
						onOpen(imageResource);
					}
				};

				URI uri = imageResource.getURI();
				if (uri != null && "file".equals(uri.getScheme()) && DesktopUtilities.isSupported()) {
					act.setEnabled(true);
				} else {
					act.setEnabled(false);
				}
				
				PNGFileImageHeader pngHeader;
				try {
					pngHeader = pngHeaderReader.readHeader(uri);
				} catch (IOException ex) {
					logger.log(Level.WARNING, "PNG Header loading error.: " + uri, ex);
					pngHeader = null;
				}

				InformationModel information = new InformationModel(partsIdentifier, layer, imageResource, param, pngHeader, act);
				informationTableModel.addRow(information);
			}
		});
		informationTableModel.sort();
		
		informationTable = new JTable(informationTableModel) {
			private static final long serialVersionUID = 1L;
			// セルの幅を大きいものにあわせる
			public Component prepareRenderer(final TableCellRenderer renderer,
					final int row, final int column) {
				final Component prepareRenderer = super.prepareRenderer(renderer, row, column);
				final TableColumn tableColumn = getColumnModel().getColumn(column);
				int preferredWidth = max(prepareRenderer
						.getPreferredSize().width, tableColumn
						.getPreferredWidth()); // セルかヘッダのどちらか幅の大きいほう
				if (tableColumn.getPreferredWidth() != preferredWidth) {
					tableColumn.setPreferredWidth(preferredWidth);
				}
				return prepareRenderer;
			}
		};
		informationTableModel.adjustColumnModel(informationTable.getColumnModel());
		informationTable.setShowGrid(true);
		informationTable.setGridColor(appConfig.getGridColor());
		informationTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		informationTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		informationTable.setRowHeight(informationTable.getRowHeight() + 4);
		
		informationTable.setDefaultRenderer(JButton.class, new ButtonCellRender());
		informationTable.setDefaultEditor(JButton.class, new ButtonCellEditor());
		
		// セルデータの幅にあわせる(事前に)
		for (int row = 0; row < informationTable.getRowCount(); row++) {
			for (int col = 0; col < informationTable.getColumnCount(); col++) {
				TableCellRenderer renderer = informationTable.getCellRenderer(row, col);
				informationTable.prepareRenderer(renderer, row, col);
			}
		}
		
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(new AbstractAction(strings.getProperty("popupmenu.copyPath")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onCopyFilePath();
			}
		});
		
		informationTable.setComponentPopupMenu(popupMenu);
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		JScrollPane informationTableSP = new JScrollPane(informationTable);
		JPanel informationTableSPPabel = new JPanel(new BorderLayout());
		informationTableSPPabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		informationTableSPPabel.add(informationTableSP, BorderLayout.CENTER);
		contentPane.add(informationTableSPPabel, BorderLayout.CENTER);

		AbstractAction actClose = new AbstractAction(strings.getProperty("btnClose")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};
		
		JPanel btnPanel = new JPanel();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 45));
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
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		JButton btnClose = new JButton(actClose);
		btnPanel.add(btnClose, gbc);
		
		contentPane.add(btnPanel, BorderLayout.SOUTH);
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(btnClose);
		
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "closeInformationDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "closeInformationDialog");
		am.put("closeInformationDialog", actClose);
		
		pack();
		setLocationRelativeTo(parent);
	}
	
	protected void onClose() {
		dispose();
	}
	
	protected void onCopyFilePath() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (int selRow : informationTable.getSelectedRows()) {
			InformationModel information = informationTableModel.getRow(selRow);
			pw.println(information.getImageResourceName());
		}

		Toolkit tk = Toolkit.getDefaultToolkit();

		String text = sw.toString();
		if (text.length() == 0) {
			tk.beep();
			return;
		}

		StringSelection textSelection = new StringSelection(sw.toString());

		Clipboard cb = tk.getSystemClipboard();
		cb.setContents(textSelection, null);
	}
	
	protected void onOpen(ImageResource imageResource) {
		try {
			URI uri = imageResource.getURI();
			if (uri != null && "file".equals(uri.getScheme())) {
				File file = new File(uri);
				DesktopUtilities.open(file);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
}

class InformationTableModel extends AbstractTableModelWithComboBoxModel<InformationModel> {
	
	private static final long serialVersionUID = 1L;

	private static final String[] columnNames;
	
	private static final int[] columnWidths;
	
	public static final int COLUMN_BUTTON;
	
	static {
		final Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(InformationDialog.STRINGS_RESOURCE);

		columnNames = new String[] {
				strings.getProperty("column.partsName"),
				strings.getProperty("column.categoryName"),
				strings.getProperty("column.layerName"),
				strings.getProperty("column.layerOrder"),
				strings.getProperty("column.imagesize"),
				strings.getProperty("column.colortype"),
				strings.getProperty("column.imageName"),
				strings.getProperty("column.editbtn"),
		};
		COLUMN_BUTTON = 7;
		columnWidths = new int[] {
				Integer.parseInt(strings.getProperty("column.partsName.width")),
				Integer.parseInt(strings.getProperty("column.categoryName.width")),
				Integer.parseInt(strings.getProperty("column.layerName.width")),
				Integer.parseInt(strings.getProperty("column.layerOrder.width")),
				Integer.parseInt(strings.getProperty("column.layerOrder.imagesize.width")),
				Integer.parseInt(strings.getProperty("column.layerOrder.colortype.width")),
				Integer.parseInt(strings.getProperty("column.imageName.width")),
				Integer.parseInt(strings.getProperty("column.editbtn.width")),
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
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return String.class;
		case 2:
			return String.class;
		case 3:
			return Integer.class;
		case 4:
			return String.class;
		case 5:
			return String.class;
		case 6:
			return String.class;
		case 7:
			return JButton.class;
		}
		return String.class;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		InformationModel information = getRow(rowIndex);
		switch (columnIndex) {
		case 0:
			return information.getPartsName();
		case 1:
			return information.getCategoryName();
		case 2:
			return information.getLayerName();
		case 3:
			return information.getLayerOrder();
		case 4:
			return information.getImageSizeStr();
		case 5:
			return information.getColorTypeStr();
		case 6:
			return information.getImageResourceName();
		case 7:
			return information.getButton();
		}
		return "";
	}
	
	public void sort() {
		Collections.sort(elements);
		fireTableDataChanged();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		InformationModel information = getRow(rowIndex);
		if (columnIndex == COLUMN_BUTTON) {
			return information.getButton().isEnabled();
		}
		return false;
	}
	
}

class InformationModel implements Comparable<InformationModel> {
	
	private PartsIdentifier partsIdentifier;

	private Layer layer;
	
	private ImageResource imageResource;
	
	private JButton btnOpen;
	
	private PNGFileImageHeader pngHeader;
	
	public InformationModel(PartsIdentifier partsIdentifier, Layer layer,
			ImageResource imageResource,
			ColorConvertParameter colorConvertParameter,
			PNGFileImageHeader pngHeader,
			AbstractAction actOpen) {
		this.partsIdentifier = partsIdentifier;
		this.layer = layer;
		this.imageResource = imageResource;
		this.pngHeader = pngHeader;
		this.btnOpen = new JButton(actOpen) {
			private static final long serialVersionUID = 1L;
			@Override
			public String toString() {
				// JTableをクリップボードにコピーしたときに設定されるカラムの文字列表現
				return "open";
			}
		};
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
		if (obj != null && obj instanceof InformationModel) {
			InformationModel o = (InformationModel) obj;
			return partsIdentifier.equals(o.partsIdentifier)
					&& layer.equals(o.layer);
		}
		return false;
	}
	
	public int compareTo(InformationModel o) {
		int ret = partsIdentifier.compareTo(o.partsIdentifier);
		if (ret == 0) {
			ret = layer.compareTo(o.layer);
		}
		if (ret == 0) {
			ret = imageResource.compareTo(o.imageResource);
		}
		return ret;
	}

	public String getPartsName() {
		return this.partsIdentifier.getLocalizedPartsName();
	}
	
	public String getCategoryName() {
		return this.partsIdentifier.getPartsCategory().getLocalizedCategoryName();
	}
	
	public String getLayerName() {
		return this.layer.getLocalizedName();
	}
	
	public int getLayerOrder() {
		return this.layer.getOrder();
	}
	
	public String getImageResourceName() {
		return this.imageResource.getFullName();
	}
	
	public JButton getButton() {
		return btnOpen;
	}
	
	public String getImageSizeStr() {
		if (pngHeader == null) {
			return "INVALID";
		}
		return pngHeader.getWidth() + "x" + pngHeader.getHeight();
	}
	
	public String getColorTypeStr() {
		if (pngHeader == null) {
			return "INVALID";
		}

		StringBuilder buf = new StringBuilder();
		
		int colorType = pngHeader.getColorType();
		if ((colorType & 0x01) != 0) {
			buf.append("Indexed ");
		}
		if ((colorType & 0x02) != 0) {
			buf.append("Color ");

		} else {
			buf.append("Greyscale ");
		}

		if (colorType == 6 || pngHeader.hasTransparencyInformation()) {
			// 6:TrueColor または アルファ情報がある場合のみアルファ有りとする.
			buf.append("Alpha ");
		}

		buf.append(pngHeader.getBitDepth() + "bit");
		
		return buf.toString().trim();
	}
}

/**
 * ボタンレンダー.<br>
 * @author seraphy
 */
class ButtonCellRender extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		return (JButton) value;
	}
}

/**
 * ボタンエディタ.<br>
 * @author seraphy
 */
class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {

	private static final long serialVersionUID = 1L;

	public Component getTableCellEditorComponent(final JTable table, final Object value,
			final boolean isSelected, final int row, final int column) {
		final JButton orgBtn = (JButton) value;
		final JButton btn = new JButton(new AbstractAction(orgBtn.getText()) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				fireEditingCanceled();
				for (ActionListener listener : orgBtn.getActionListeners()) {
					listener.actionPerformed(e);
				}
			}
		});
		return btn;
	}

	public Object getCellEditorValue() {
		return null;
	}
	
}

