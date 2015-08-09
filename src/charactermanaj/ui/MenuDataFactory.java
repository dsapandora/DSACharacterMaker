package charactermanaj.ui;

import java.awt.event.ActionListener;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;


public class MenuDataFactory extends AbstractCollection<MenuDataFactory> {
	
	private String name;
	
	private boolean checkbox;
	
	private ActionListener actionListener;
	
	private MenuDataFactory[] factories;
	
	public MenuDataFactory(String name) {
		this(name, false, null, null);
	}

	public MenuDataFactory(String name, MenuDataFactory[] factories) {
		this(name, false, null, factories);
	}

	public MenuDataFactory(String name, ActionListener actionListener) {
		this(name, false, actionListener, null);
	}

	public MenuDataFactory(String name, boolean checkbox, ActionListener actionListener) {
		this(name, checkbox, actionListener, null);
	}

	public String getName() {
		return name;
	}
	
	public boolean isCheckbox() {
		return checkbox;
	}
	
	public MenuDataFactory(String name, boolean checkbox, ActionListener actionListener, MenuDataFactory[] factories) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException();
		}
		if (factories == null) {
			factories = new MenuDataFactory[0];
		}
		this.name = name;
		this.checkbox = checkbox;
		this.actionListener = actionListener;
		this.factories = factories;
	}
	
	public MenuData createMenuData(Properties props) {
		if (props == null) {
			throw new IllegalArgumentException();
		}
		
		String text = props.getProperty(name + ".text");
		String mnemonic = props.getProperty(name + ".mnemonic");
		String mnemonicDisp = props.getProperty(name + ".mnemonicDisp");
		String ignoreMacOSX = props.getProperty(name + ".ignoreMacOSX");
		String shortcutKey = props.getProperty(name + ".shortcut-key");
		
		MenuData menuData = new MenuData();

		menuData.setName(getName());
		menuData.setCheckbox(isCheckbox());
		
		menuData.setText(text);
		if (mnemonic != null && mnemonic.length() > 0) {
			menuData.setMnemonic(mnemonic.charAt(0));
			menuData.setMnimonicDisp(mnemonicDisp);
		}
		menuData.setIgnoreMacOSX(ignoreMacOSX != null && Boolean.valueOf(ignoreMacOSX));
		menuData.setActionListener(actionListener);
		menuData.setShortcutKey(shortcutKey);

		for (MenuDataFactory factory : factories) {
			if (factory != null) {
				menuData.add(factory.createMenuData(props));
			} else {
				menuData.add(null);
			}
		}
		
		return menuData;
	}
	
	@Override
	public int size() {
		return factories.length;
	}
	
	@Override
	public Iterator<MenuDataFactory> iterator() {
		return new Iterator<MenuDataFactory>() {

			private int idx = 0;
			
			public boolean hasNext() {
				return idx < factories.length;
			}

			public MenuDataFactory next() {
				if (idx >= factories.length) {
					throw new NoSuchElementException();
				}
				return factories[idx++];
			}
			
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
}
