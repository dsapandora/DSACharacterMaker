package charactermanaj.ui.util;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JFrame;

/**
 * ウィンドウの位置を調整するサポートクラス.<br>
 * 
 * @author seraphy
 */
public final class WindowAdjustLocationSupport {

	/**
	 * プライベートコンストラクタ
	 */
	private WindowAdjustLocationSupport() {
		super();
	}

	/**
	 * ウィンドウの表示位置をメインウィンドウの右側に調整する.<br>
	 * 横位置Xはメインフレームの右側とし、縦位置Yはメインフレームの上位置からのoffset_yを加えた位置とする.<br>
	 * 
	 * @param mainWindow
	 *            基準位置となるメインウィンドウ
	 * @param window
	 *            位置を調整するウィンドウ
	 * @param offset_y
	 *            表示のYオフセット
	 * @param sameHeight
	 *            高さをメインウィンドウにそろえるか？
	 */
	public static void alignRight(JFrame mainWindow, Window window,
			int offset_y, boolean sameHeight) {
		// メインウィンドウよりも左側に位置づけする.
		// 縦位置はメインウィンドウの上端からオフセットを加えたものとする.
		Point pt = mainWindow.getLocation();
		Insets insets = mainWindow.getInsets();
		pt.x += mainWindow.getWidth();
		pt.y += (offset_y * insets.top);

		// メインスクリーンサイズを取得する.
		GraphicsEnvironment genv = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		Rectangle desktopSize = genv.getMaximumWindowBounds(); // メインスクリーンのサイズ(デスクトップ領域のみ)

		// メインスクリーンサイズを超えた場合は、はみ出た分を移動する.
		if ((pt.x + window.getWidth()) > desktopSize.width) {
			pt.x -= ((pt.x + window.getWidth()) - desktopSize.width);
		}
		if ((pt.y + window.getHeight()) > desktopSize.height) {
			pt.y -= ((pt.y + window.getHeight()) - desktopSize.height);
		}

		window.setLocation(pt);

		// 高さはメインフレームと同じにする.
		if (sameHeight) {
			Dimension siz = window.getSize();
			siz.height = mainWindow.getHeight() - offset_y;
			window.setSize(siz);
		}
	}
}
