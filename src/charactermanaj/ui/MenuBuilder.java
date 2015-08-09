package charactermanaj.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import charactermanaj.ui.scrollablemenu.JScrollableMenu;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * メニューを構築します.
 * 
 * @author seraphy
 */
public class MenuBuilder {

	/**
	 * メニュー項目のリソース
	 */
	protected static final String MENU_STRINGS_RESOURCE = "menu/menu";

	/**
	 * アンチエイリアスの設定が必要か?
	 */
	private static final boolean needAntiAlias = isNeedAntialias();

	/**
	 * メニュー項目のアンチエイリアスが必要か判定する.<br>
	 * java.specification.versionが1.5で始まる場合は必要とみなす.<br>
	 * 
	 * @return アンチエイリアスが必要であればtrue
	 */
	private static boolean isNeedAntialias() {
		return System.getProperty("java.specification.version").startsWith("1.5");
	}

	/**
	 * 生成したメニューのマップ
	 */
	private final HashMap<String, JMenu> menuMap = new HashMap<String, JMenu>();

	/**
	 * 生成したメニュー項目のマップ
	 */
	private final HashMap<String, JMenuItem> menuItemMap = new HashMap<String, JMenuItem>();

	/**
	 * 生成されたメニューを名前を指定して取得します.<br>
	 * 存在しない場合は実行時例外が発生します.<br>
	 * 
	 * @param name
	 *            メニュー名
	 * @return メニュー
	 */
	public JMenu getJMenu(String name) {
		JMenu menu = menuMap.get(name);
		if (menu == null) {
			throw new RuntimeException("登録されていないメニューです. " + name);
		}
		return menu;
	}
	
	/**
	 * 生成されたメニュー項目を名前を指定して取得します.<br>
	 * 存在しない場合は実行時例外が発生します.<br>
	 * 
	 * @param name
	 *            メニュー項目名
	 * @return メニュー項目
	 */
	public JMenuItem getJMenuItem(String name) {
		JMenuItem menuItem = menuItemMap.get(name);
		if (menuItem == null) {
			throw new RuntimeException("登録されていないメニュー項目です. " + name);
		}
		return menuItem;
	}
	
	/**
	 * メニュー設定に従いメニューバーを構築して返します.<br>
	 * 生成したメニューとメニュー項目は、{@link #getJMenu(String)}, {@link #getJMenuItem(String)}
	 * で取得できます.<br>
	 * 
	 * @param menus
	 *            メニュー設定
	 * @return 構築されたメニューバー
	 */
	public JMenuBar createMenuBar(MenuDataFactory[] menus) {
		
		// メニューリソース
		Properties menuProps = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(MENU_STRINGS_RESOURCE);

		// メニューバー
		final JMenuBar menuBar = createJMenuBar();

		// 現在のメニュー設定をクリアする.
		menuMap.clear();
		menuItemMap.clear();
		
		// メニュー設定に従いメニューを構築する.
		for (MenuDataFactory menuDataFactory : menus) {
			MenuData menuData = menuDataFactory.createMenuData(menuProps);
			createMenu(new MenuAppender() {
				public void addMenu(JMenu menu) {
					menuBar.add(menu);
				}
			}, menuData, menuProps);
		}
		
		return menuBar;
	}
	
	private interface MenuAppender {
		
		void addMenu(JMenu menu);
		
	}
	
	protected void createMenu(MenuAppender parentMenu, MenuData menuData, Properties menuProps) {
		final JMenu menu = createJMenu();
		if (menuData.makeMenu(menu)) {
			parentMenu.addMenu(menu);
			menuMap.put(menuData.getName(), menu);
			
			for (MenuData child : menuData) {
				if (child == null) {
					// セパレータ
					menu.add(new JSeparator());

				} else if (child.getActionListener() == null) {
					// アクションリスナなしの場合はサブメニューと見なす
					createMenu(new MenuAppender() {
						public void addMenu(JMenu childMenu) {
							menu.add(childMenu);
						}
					}, child, menuProps);
				
				} else {
					// メニュー項目(チェックボックスつきメニュー項目を含む)
					JMenuItem menuItem;
					if (child.isCheckbox()) {
						menuItem = createJCheckBoxMenuItem();
					} else {
						menuItem = createJMenuItem();
					}
					if (child.makeMenu(menuItem)) {
						menu.add(menuItem);
						menuItemMap.put(child.getName(), menuItem);
					}
				}
			}
		}
	}
	
	/**
	 * JMenuBarを構築します.<br>
	 * アンチエイリアスが必要な場合はアンチエイリアスが設定されます.<br>
	 * 
	 * @return JMenuBar
	 */
	public JMenuBar createJMenuBar() {
		return new JMenuBar() {
			private static final long serialVersionUID = 1L;
			@Override
			public void paint(Graphics g) {
				setAntiAlias(g);
				super.paint(g);
			}
		};
	}
	
	/**
	 * JMenuを構築します.<br>
	 * アンチエイリアスが必要な場合はアンチエイリアスが設定されます.<br>
	 * 
	 * @return JMenu
	 */
	public JMenu createJMenu() {
		if (JScrollableMenu.isScreenMenu()) {
			return new JMenu() {
				private static final long serialVersionUID = 1L;
				@Override
				public void paint(Graphics g) {
					setAntiAlias(g);
					super.paint(g);
				}
			};
		} else {
			return new JScrollableMenu() {
				private static final long serialVersionUID = 1L;
				@Override
				public void paint(Graphics g) {
					setAntiAlias(g);
					super.paint(g);
				}
			};
		}
	}
	
	/**
	 * JCheckBoxMenuItemを構築します.<br>
	 * アンチエイリアスが必要な場合はアンチエイリアスが設定されます.<br>
	 * 
	 * @return JCheckBoxMenuItem
	 */
	public JCheckBoxMenuItem createJCheckBoxMenuItem() {
		return new JCheckBoxMenuItem() {
			private static final long serialVersionUID = 1L;
			@Override
			public void paint(Graphics g) {
				setAntiAlias(g);
				super.paint(g);
			}
		};
	}
	
	/**
	 * JMenuItemを構築します.<br>
	 * アンチエイリアスが必要な場合はアンチエイリアスが設定されます.<br>
	 * 
	 * @return JMenuItem
	 */
	public JMenuItem createJMenuItem() {
		return new JMenuItem() {
			private static final long serialVersionUID = 1L;
			@Override
			public void paint(Graphics g) {
				setAntiAlias(g);
				super.paint(g);
			}
		};
	}
	
	/**
	 * アンチエイリアスを有効にする.
	 * 
	 * @param g
	 */
	private static void setAntiAlias(Graphics g) {
		if (needAntiAlias) {
			((Graphics2D) g).setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
	}
}
