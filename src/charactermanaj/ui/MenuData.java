package charactermanaj.ui;

import java.awt.event.ActionListener;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import charactermanaj.Main;

public class MenuData extends AbstractCollection<MenuData> {
	
	private String name;
	
	private boolean checkbox;
	
	private String text;
	
	private Character mnemonic;
	
	private String mnemonicDisp;
	
	private boolean ignoreMacOSX;
	
	private String shortcutKey;
	
	private ActionListener actionListener;
	
	private ArrayList<MenuData> children = new ArrayList<MenuData>();

	public MenuData() {
		this(null, false, null, null, false, null, null);
	}
	
	public MenuData(String text, boolean checkbox, Character mnemonic, String mnemonicDisp, boolean ignoreMacOSX, String shortcutKey, ActionListener actionListener) {
		this.text = text;
		this.checkbox = checkbox;
		this.mnemonic = mnemonic;
		this.mnemonicDisp = mnemonicDisp;
		this.ignoreMacOSX = ignoreMacOSX;
		this.shortcutKey = shortcutKey;
		this.actionListener = actionListener;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setCheckbox(boolean checkbox) {
		this.checkbox = checkbox;
	}
	
	public boolean isCheckbox() {
		return checkbox;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Character getMnemonic() {
		return mnemonic;
	}

	public void setMnemonic(Character mnemonic) {
		this.mnemonic = mnemonic;
	}
	
	public String getMnemonicDisp() {
		return mnemonicDisp;
	}
	
	public void setMnimonicDisp(String mnemonicDisp) {
		this.mnemonicDisp = mnemonicDisp;
	}

	public boolean isIgnoreMacOSX() {
		return ignoreMacOSX;
	}

	public void setIgnoreMacOSX(boolean ignoreMacOSX) {
		this.ignoreMacOSX = ignoreMacOSX;
	}
	
	public ActionListener getActionListener() {
		return actionListener;
	}
	
	public void setActionListener(ActionListener actionListener) {
		this.actionListener = actionListener;
	}
	
	public String getShortcutKey() {
		return shortcutKey;
	}
	
	public void setShortcutKey(String shortcutKey) {
		this.shortcutKey = shortcutKey;
	}
	
	@Override
	public int size() {
		return children.size();
	}
	
	@Override
	public Iterator<MenuData> iterator() {
		return children.iterator();
	}
	
	@Override
	public boolean add(MenuData o) {
		return children.add(o);
	}

	public boolean makeMenu(JMenuItem menu) {
		if (! isIgnoreMacOSX() || ! Main.isMacOSX()) {
			if (Main.isMacOSX()) {
				menu.setText(getText());
			} else {
				Character mnemonic = getMnemonic();
				String mnemonicDisp =getMnemonicDisp();
				if (mnemonicDisp == null) {
					mnemonicDisp = "";
				}
				menu.setName(getName());
				menu.setText(getText() + mnemonicDisp);
				if (mnemonic != null) {
					menu.setMnemonic(mnemonic);
				}
			}
			if (actionListener != null) {
				menu.addActionListener(actionListener);
			}
			if (shortcutKey != null && shortcutKey.length() > 0) {
				if (Main.isMacOSX()) {
					shortcutKey = shortcutKey.replace("?", "meta");
				} else {
					shortcutKey = shortcutKey.replace("?", "control");
				}
				KeyStroke ks = KeyStroke.getKeyStroke(shortcutKey);
				if (ks != null) {
					menu.setAccelerator(ks);
				}
			}
			return true;
		}
		return false;
	}
	
}
