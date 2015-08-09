package charactermanaj.util;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * デスクトップへのアクセスを提供するユーテリティ.<br>
 * JDK6の機能を使うため、JDK5以前では何もしない.(エラーにはならない)
 * @author seraphy
 */
public class DesktopUtilities {

	/**
	 * ロガー 
	 */
	private static final Logger logger = Logger.getLogger(DesktopUtilities.class.getName());

	/**
	 * デスクトップオブジェクト。JDK6以降でなければ、もしくはデスクトップをサポートしていなければnull
	 */
	private static Object desktopObj;

	/**
	 * ブラウズメソッド
	 */
	private static Method methodBrowse;
	
	/**
	 * 編集メソッド
	 */
	private static Method methodEdit;

	/**
	 * 開くメソッド
	 */
	private static Method methodOpen;

	static {
		try {
			Class<?> clz = Class.forName("java.awt.Desktop");
			Method mtdGetDesktop = clz.getMethod("getDesktop");
			methodBrowse = clz.getMethod("browse", URI.class);
			methodEdit = clz.getMethod("edit", File.class);
			methodOpen = clz.getMethod("open", File.class);
			desktopObj = mtdGetDesktop.invoke(null);

		} catch (ClassNotFoundException ex) {
			// JDK6以降でない場合
			logger.log(Level.CONFIG, "AWT Desktop is not suuported.");
			desktopObj = null;
			
		} catch (Exception ex) {
			// その他の例外は基本的に発生しないが、発生したとしても
			// 単にサポートしていないと見なして継続する.
			logger.log(Level.SEVERE, "AWT Desktop failed.", ex);
			desktopObj = null;
		}
	}
	

	private DesktopUtilities() {
		throw new RuntimeException("utilities class.");
	}
	
	public static boolean isSupported() {
		return desktopObj != null;
	}
	
	protected static boolean callMethod(Method method, Object arg) throws IOException {
		if (desktopObj == null) {
			return false;
		}
		try {
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "invoke: " + method + "/arg=" + arg);
			}
			method.invoke(desktopObj, arg);
			return true;

		} catch (InvocationTargetException ex) {
			Throwable iex = ex.getCause();
			if (iex != null && iex instanceof IOException) {
				throw (IOException) iex;
			}
			throw new RuntimeException(ex.getMessage(), ex);

		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	/**
	 * ファイルを開く.
	 * @param uri ファイル
	 * @return サポートしていない場合はfalse、実行できればtrue。
	 * @throws IOException 実行できなかった場合
	 */
	public static boolean browse(URI uri) throws IOException {
		return callMethod(methodBrowse, uri);
	}
	
	/**
	 * 指定したdocBaseの親ディレクトリを開く.
	 * @param docBase
	 * @return サポートしていない場合はfalse、実行できればtrue。
	 * @throws IOException 実行できなかった場合
	 */
	public static boolean browseBaseDir(URI docBase) throws IOException {
		File baseDir = null;
		try {
			if (docBase != null) {
				baseDir = new File(docBase).getParentFile();
			}
		} catch (Exception ex) {
			baseDir = null;
		}
		if (baseDir == null) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();
			return false;
		}

		return DesktopUtilities.open(baseDir);
	}

	/**
	 * ファイルを編集する.
	 * @param file ファイル
	 * @return サポートしていない場合はfalse、実行できればtrue。
	 * @throws IOException 実行できなかった場合
	 */
	public static boolean edit(File file) throws IOException {
		return callMethod(methodEdit, file);
	}

	/**
	 * ファイルを編集する.
	 * @param file ファイル
	 * @return サポートしていない場合はfalse、実行できればtrue。
	 * @throws IOException 実行できなかった場合
	 */
	public static boolean open(File file) throws IOException {
		return callMethod(methodOpen, file);
	}

	/**
	 * ブラウザでURLを開きます.<br>
	 * JDK1.6未満の場合はブラウザを開く代わりにURLと、それを説明するメッセージボックスが表示されます.<br>
	 * @param parent 親フレーム、またはダイアログ
	 * @param url URL
	 * @param description ブラウズがサポートされていない場合に表示するダイアログでのURLの説明文
	 */
	public static void browse(final Component parent, final String url, final String description) {
		try {
			URI helpURI = new URI(url);

			if (!DesktopUtilities.browse(helpURI) ){
				// JDK5で実行中の場合

				JPanel panel = new JPanel();
				BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
				panel.setLayout(layout);
				panel.add(new JLabel(description));
				JTextField txtURL = new JTextField(url);
				panel.add(txtURL);
				JOptionPane.showMessageDialog(parent, panel);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(parent, ex);
		}
	}
	
	
	/**
	 * ブラウザでURLを開くアクションの生成.<br>
	 * エラー時はエラーダイアログが開かれる.<br>
	 * 返されるアクションがとるアクションイベントは無視されるため、nullを渡しても問題ありません.<br>
	 * @param parent 親フレーム、またはダイアログ
	 * @param url URLの文字列
	 * @param description ブラウズがサポートされていない場合に表示するダイアログでのURLの説明文
	 * @return アクション
	 */
	public static ActionListener createBrowseAction(final Component parent, final String url, final String description) {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browse(parent, url, description);
			}
		};
	}
	
}
