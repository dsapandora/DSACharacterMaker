package charactermanaj.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;


public final class UIHelper extends ResourceLoader {

	private static final UIHelper singleton = new UIHelper();
	
	private UIHelper() {
		super();
	}
	
	public static final UIHelper getInstance() {
		return singleton;
	}
	
	/**
	 * 指定したコンテナに含まれる指定したコンポーネント型のすべてのコンポーネントを返す.<br>
	 * 一つも該当するものがなければ空を返す
	 * @param <T> 対象のコンポーネントのクラス型
	 * @param clz クラス
	 * @param container 対象のコンテナ
	 * @return コンポーネントのコレクション、もしくは空
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getDescendantOfClass(Class<T> clz, Container container) {
		if (container == null || clz == null) {
			throw new IllegalArgumentException();
		}
		Collection<Component> components = new ArrayList<Component>();
		getDescendantOfClass(clz, container, components);
		return (Collection<T>) components;
	}
	
	private void getDescendantOfClass(Class<?> clz, Container container, Collection<Component> results) {
		if (container == null) {
			return;
		}
		Component[] children = (container instanceof JMenu) ?
				((JMenu) container).getMenuComponents() : container.getComponents();
		int mx = children.length;
		for (int idx = 0; idx < mx; idx++) {
			Component comp = children[idx];
			if (clz.isInstance(comp)) {
				results.add(comp);
			} else if (comp instanceof Container) {
				getDescendantOfClass(clz, (Container) comp, results);
			}
		}
	}
	
	/**
	 * 2つのステートをもつアイコンを作成します.<br>
	 * このアイコンは、使用するコンポーネントがAbstractButton派生クラスであれば、isSelectedの結果が
	 * trueである場合は2番目のアイコンイメージを表示します.<br>
	 * isSelectedの結果がfalseであるか、もしくはAbstractButton派生クラスでなければ
	 * 最初のアイコンイメージを表示します.<br>
	 * @param iconName1 アイコン1
	 * @param iconName2 アイコン2
	 * @return アイコン
	 */
	public Icon createTwoStateIcon(String iconName1, String iconName2) {
		if (iconName1 == null || iconName2 == null || iconName1.length() == 0
				|| iconName2.length() == 0) {
			throw new IllegalArgumentException();
		}
		final BufferedImage pinIcon1 = getImage(iconName1);
		final BufferedImage pinIcon2 = getImage(iconName2);
		
		Icon icon = new Icon() {
			public void paintIcon(Component c, Graphics g, int x, int y) {
				boolean selected = false;
				if (c instanceof AbstractButton) {
					AbstractButton btn = (AbstractButton) c;
					selected = btn.isSelected();
				}
				BufferedImage iconImage;
				if ( !selected) {
					iconImage = pinIcon1;
				} else {
					iconImage = pinIcon2;
				}
				int w = iconImage.getWidth();
				int h = iconImage.getHeight();
				g.drawImage(iconImage, x, y, w, h, 0, 0, w, h, null);
			}
			public int getIconHeight() {
				return pinIcon1.getHeight();
			}
			public int getIconWidth() {
				return pinIcon1.getWidth();
			}
		};
		
		return icon;
	}

	/**
	 * アイコンボタン(非透過)を作成して返す.<br>
	 * リソースが取得できない場合は実行時例外が返される.<br>
	 * @param iconName 画像リソース名
	 * @return アイコンボタン
	 */
	public JButton createIconButton(String iconName) {
		if (iconName == null || iconName.length() == 0) {
			throw new IllegalArgumentException();
		}
		JButton btn = new JButton();
		btn.setIcon(new ImageIcon(getImage(iconName)));
		
		return btn;
	}
	
	/**
	 * 通常時の画像のみをもつ透過ボタンを作成して返す.<br>
	 * リソースが取得できない場合は実行時例外が返される.<br>
	 * @param normal 通常時の画像リソース
	 * @return 透過ボタン
	 */
	public JButton createTransparentButton(String normal) {
		return createTransparentButton(normal, null);
	}

	/**
	 * リソースから通常とホバー時の画像をもつ透過ボタンを作成して返す.<br>
	 * リソースが取得できない場合は実行時例外が返される.<br>
	 * @param normal 通常時の画像リソース
	 * @param rollover ホバー時の画像リソース
	 * @return 透過ボタン
	 */
	public JButton createTransparentButton(String normal, String rollover) {
		if (normal == null || normal.length() == 0) {
			throw new IllegalArgumentException();
		}
		ImageIcon normIcon = new ImageIcon(getImage(normal));
		JButton btn = new JButton(normIcon);

		if (rollover != null && rollover.length() != 0) {
			ImageIcon rolloverIcon = new ImageIcon(getImage(rollover));
			btn.setRolloverEnabled(true);
			btn.setRolloverIcon(rolloverIcon);
			btn.setPressedIcon(rolloverIcon);
		}

		btn.setOpaque(false);
		btn.setBorderPainted(false);
		btn.setContentAreaFilled(false);

		return btn;
	}
	
	/**
	 * リソースから画像を取得する.<br>
	 * 画像が取得できない場合は実行時例外を返す.<br>
	 * @param name リソース
	 * @return 画像
	 */
	public BufferedImage getImage(String name) {
		URL url = getResource(name);
		if (url == null) {
			throw new RuntimeException("resource not found. " + name);
		}
		try {
			return ImageIO.read(url);

		} catch (IOException ex) {
			throw new RuntimeException("image load error." + ex.getMessage(), ex);
		}
	}
		
}
