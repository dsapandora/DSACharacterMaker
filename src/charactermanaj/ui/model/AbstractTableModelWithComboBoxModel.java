package charactermanaj.ui.model;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;


public /**
 * テーブルモデルをベースに、それをコンボボックスモデルとしても利用できるように拡張した抽象基底クラス.<br>
 * @author seraphy
 */
abstract class AbstractTableModelWithComboBoxModel<T> extends AbstractTableModel implements ComboBoxModel {
	
	private static final long serialVersionUID = -6775939667002896930L;

	protected ArrayList<T> elements = new ArrayList<T>();
	
	private boolean editable = true;
	
	public boolean removeRow(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= elements.size()) {
			return false;
		}
		elements.remove(rowIndex);
		fireTableRowsDeleted(rowIndex, rowIndex);
		return true;
	}
	
	/**
	 * 編集可能フラグ.<br>
	 * モデル自身は、このフラグについて何ら関知せず、常に編集可能である.<br>
	 * @param editable
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	/**
	 * 編集可能フラグ.<br>
	 * モデル自身は、このフラグについて何ら関知せず、常に編集可能である.<br>
	 * 初期状態でtrueである.<br>
	 * @return
	 */
	public boolean isEditable() {
		return editable;
	}
	
	public void clear() {
		elements.clear();
		fireTableDataChanged();
	}
	
	public T getRow(int index) {
		return elements.get(index);
	}
	
	public Object getElementAt(int index) {
		return getRow(index);
	}

	public boolean addRow(T obj) {
		if (obj == null) {
			throw new IllegalArgumentException();
		}
		boolean ret = elements.add(obj);
		int row = elements.size() - 1;
		fireTableRowsInserted(row, row);
		return ret;
	}
	
	public int moveUp(int rowIndex) {
		if (rowIndex < 1 || rowIndex >= elements.size()) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return rowIndex;
		}
		T value = elements.get(rowIndex);
		elements.remove(rowIndex);
		elements.add(rowIndex - 1, value);
		fireTableRowsUpdated(rowIndex -1, rowIndex);
		return rowIndex - 1;
	}
	
	public int moveDown(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= elements.size() - 1) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return rowIndex;
		}
		T value = elements.get(rowIndex);
		elements.remove(rowIndex);
		elements.add(rowIndex + 1, value);
		fireTableRowsUpdated(rowIndex, rowIndex + 1);
		return rowIndex + 1;
	}

	/////////////////////////////////////////
	@Override
	public void fireTableCellUpdated(int row, int column) {
		super.fireTableCellUpdated(row, column);
		fireListUpdated(row, row);
	}
	
	@Override
	public void fireTableRowsDeleted(int firstRow, int lastRow) {
		super.fireTableRowsDeleted(firstRow, lastRow);
		fireListRemoved(firstRow, lastRow);
	}
	
	@Override
	public void fireTableRowsInserted(int firstRow, int lastRow) {
		super.fireTableRowsInserted(firstRow, lastRow);
		fireListAdded(firstRow, lastRow);
	}
	
	@Override
	public void fireTableRowsUpdated(int firstRow, int lastRow) {
		super.fireTableRowsUpdated(firstRow, lastRow);
		fireListUpdated(firstRow, lastRow);
	}
	
	@Override
	public void fireTableDataChanged() {
		super.fireTableDataChanged();
		int siz = getRowCount();
		if (siz > 0) {
			fireListUpdated(0, siz - 1);
		}
	}
	
	public int getRowCount() {
		return elements.size();
	}
	

	//////////////////////////////

	private Object selectedObject;
	
	private LinkedList<ListDataListener> listDataListeners = new LinkedList<ListDataListener>();
	
	public Object getSelectedItem() {
		return selectedObject;
	}
	
	public void setSelectedItem(Object anItem) {
		selectedObject = anItem;
	}

	public void removeListDataListener(ListDataListener l) {
		if (l != null) {
			listDataListeners.remove(l);
		}
	}

	public void addListDataListener(ListDataListener l) {
		if (l != null) {
			listDataListeners.add(l);
		}
	}
	
	public void fireListUpdated(int firstRow, int lastRow) {
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, firstRow, lastRow);
		for (ListDataListener listener : listDataListeners) {
			listener.contentsChanged(e);
		}
	}

	public void fireListAdded(int firstRow, int lastRow) {
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, firstRow, lastRow);
		for (ListDataListener listener : listDataListeners) {
			listener.intervalAdded(e);
		}
	}

	public void fireListRemoved(int firstRow, int lastRow) {
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, firstRow, lastRow);
		for (ListDataListener listener : listDataListeners) {
			listener.intervalRemoved(e);
		}
	}
	
	public int getSize() {
		return getRowCount();
	}
}
