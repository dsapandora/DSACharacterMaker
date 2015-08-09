package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import charactermanaj.model.AppConfig;
import charactermanaj.util.DesktopUtilities;
import charactermanaj.util.DirectoryConfig;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedTextResource;
import charactermanaj.util.SystemUtil;


/**
 * Aboutボックスを表示する.
 * 
 * @author seraphy
 */
public class AboutBox {

	/**
	 * ロガー
	 */
	private static Logger logger = Logger.getLogger(AboutBox.class.getName());

	private JFrame parent;
	
	public AboutBox(JFrame parent) {
		if (parent == null) {
			throw new IllegalArgumentException();
		}
		this.parent = parent;
	}

	/**
	 * Aboutボックスを表示する.
	 */
	public void showAboutBox() {

		final JTabbedPane tabs = new JTabbedPane();
		tabs.setPreferredSize(new Dimension(500, 400));

		final JPanel aboutPanel = createAboutPanel();
		final JSysInfoPanel sysInfoPanel = new JSysInfoPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void onGc() {
				super.onGc();
				setText(getSysInfoText());
			}
		};
		
		tabs.addTab("About", aboutPanel);
		tabs.addTab("System", sysInfoPanel);

		tabs.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (tabs.getSelectedIndex() == 1) {
					sysInfoPanel.setText(getSysInfoText());
				}
			}
		});
		
		JOptionPane.showMessageDialog(parent, tabs, "About", JOptionPane.INFORMATION_MESSAGE);
	}
	
	protected JPanel createAboutPanel() {
		LocalizedTextResource textResource = new LocalizedTextResource() {
			@Override
			protected URL getResource(String resourceName) {
				return getClass().getClassLoader().getResource(resourceName);
			}
		};

		String message = textResource.getText("appinfo/about.html",
				Charset.forName("UTF-8"));

		AppConfig appConfig = AppConfig.getInstance();

		String versionInfo = appConfig.getImplementationVersion();
		String specificationVersionInfo = appConfig.getSpecificationVersion();
		
		message = message.replace("@@IMPLEMENTS-VERSIONINFO@@", versionInfo);
		message = message.replace("@@SPECIFICATION-VERSIONINFO@@", specificationVersionInfo);
		
		JPanel aboutPanel = new JPanel(new BorderLayout());
		JEditorPane editorPane = new JEditorPane();
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					URL url = e.getURL();
					if (url != null) {
						try {
							if (!DesktopUtilities.browse(url.toURI())) {
								JOptionPane.showMessageDialog(parent,
										url.toString());
							}

						} catch (Exception ex) {
							ErrorMessageHelper.showErrorDialog(parent, ex);
						}
					}
				}
			}
		});
		editorPane.setEditable(false);
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setContentType("text/html");

		// HTML上のcharsetの指定を無視する.
		Document doc = editorPane.getDocument();
		doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);

		// editorPane.setText(message);
		// HTML上のcontent-typeを無視する設定はread時のみ有効のようなのでreadを使う.
		EditorKit editorKit = editorPane.getEditorKit();
		try {
			StringReader rd = new StringReader(message);
			try {
				editorKit.read(rd, doc, 0);

			} finally {
				rd.close();
			}
		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.toString(), ex);
		}

		editorPane.setSelectionStart(0);
		editorPane.setSelectionEnd(0);
		aboutPanel.add(new JScrollPane(editorPane), BorderLayout.CENTER);
		
		return aboutPanel;
	}
	
	/**
	 * システム情報を取得してHTML形式の文字列として返す.<br>
	 * ランタイム情報、システムプロパティ情報、環境変数情報を取得する.<br>
	 * 
	 * @return システム情報のHTML文字列
	 */
	protected String getSysInfoText() {

		// ランタイム情報の取得
		
		long freeMem, totalMem, maxMem;
		Runtime rt = Runtime.getRuntime();

		totalMem = rt.totalMemory() / 1024;
		freeMem = rt.freeMemory() / 1024;
		maxMem = rt.maxMemory() / 1024;
		
		DecimalFormat decimalFmt = new DecimalFormat("#,###,##0");
		
		StringBuilder buf = new StringBuilder();
		buf.append("<html>");
		buf.append("<h2>Runtime Information</h2>");
		buf.append("<table border=\"0\">");
		buf.append("<tr><td>Max Memory:</td><td>" + decimalFmt.format(maxMem) + " KiB</td></tr>");
		buf.append("<tr><td>Total Memory:</td><td>" + decimalFmt.format(totalMem) + " KiB</td></tr>");
		buf.append("<tr><td>Free Memory:</td><td>" + decimalFmt.format(freeMem) + " KiB</td></tr>");
		buf.append("</table>");
		
		// キャラクターデータベースの取得
		
		DirectoryConfig dirConfig = DirectoryConfig.getInstance();
		String charactersDir = null;
		try {
			charactersDir = dirConfig.getCharactersDir().toString();
		} catch (RuntimeException ex) {
			charactersDir = "**INVALID**";
		}
		
		buf.append("<h2>Character Database</h2>");
		buf.append("<table border=\"1\">");
		buf.append("<tr><td>Location</td>");
		buf.append("<td>" + escape(charactersDir) + "</td></tr>");
		buf.append("</table>");
		
		// サポートしているエンコーディングの列挙
		buf.append("<h2>Available Charsets</h2>");
		Charset defaultCharset = Charset.defaultCharset();

		StringBuilder bufChars = new StringBuilder();
		boolean foundWin31j = false;
		bufChars.append("<table border=\"1\">");
		for (Map.Entry<String, Charset> entry : Charset.availableCharsets().entrySet()) {
			String name = entry.getKey();
			Charset charset = entry.getValue();

			boolean isDef = charset.equals(defaultCharset);

			boolean win31j = name.toLowerCase().equals("windows-31j");
			StringBuilder aliasBuf = new StringBuilder();
			for (String alias : charset.aliases()) {
				if (aliasBuf.length() > 0) {
					aliasBuf.append(", ");
				}
				aliasBuf.append(alias);
				if (alias.toLowerCase().equals("cswindows31j")) {
					win31j = true;
				}
			}
			foundWin31j = foundWin31j || win31j;

			bufChars.append("<tr><td>");
			if (isDef || win31j) {
				bufChars.append("<b>");
			}
			if (win31j) {
				bufChars.append("<span style=\"color: red;\">");
			}
			bufChars.append(name);
			if (win31j) {
				bufChars.append("</span>");
			}
			if (isDef || win31j) {
				bufChars.append("*<b>");
			}
			bufChars.append("</td><td>" + aliasBuf.toString() + "</td></tr>");
		}
		bufChars.append("</table>");

		if (!foundWin31j) {
			buf.append("<p><span style=\"color: red;\">This system is not supporting Japanese.</span></p>");
		}
		buf.append(bufChars.toString());
		
		// システムプロパティの取得
		
		buf.append("<h2>System Properties</h2><table border=\"1\">");
		try {
			Properties sysprops = System.getProperties();
			Enumeration<?> enmKey = sysprops.keys();
			TreeMap<String, String> propMap = new TreeMap<String, String>(); // プロパティキーのアルファベッド順にソート
			while (enmKey.hasMoreElements()) {
				String key = (String) enmKey.nextElement();
				String value = sysprops.getProperty(key);
				propMap.put(key, value == null ? "null" : value);
			}
			for (Map.Entry<String, String> entry : propMap.entrySet()) {
				buf.append("<tr>");
				buf.append("<td>" + escape(entry.getKey()) + "</td><td>" + escape(entry.getValue()) + "</td>");
				buf.append("</tr>");
			}
			buf.append("</table>");
		} catch (Exception ex) {
			buf.append(escape(ex.toString()));
		}
		
		// 環境変数の取得
		
		buf.append("<h2>System Environments</h2>");
		try {
			TreeMap<String, String> envMap = new TreeMap<String, String>(); // 環境変数名のアルファベット順にソート
			envMap.putAll(System.getenv());
			buf.append("<table border=\"1\">");
			for (Map.Entry<String, String> entry : envMap.entrySet()) {
				buf.append("<tr>");
				buf.append("<td>" + escape(entry.getKey()) + "</td><td>" + escape(entry.getValue()) + "</td>");
				buf.append("</tr>");
			}
			buf.append("</table>");
		} catch (Exception ex) {
			buf.append(escape(ex.toString()));
		}

		// HTMLとして文字列を返す.

		buf.append("</html>");
		return buf.toString();
	}
	
	protected String escape(String value) {
		if (value == null) {
			return null;
		}
		value = value.replace("&", "&amp;");
		value = value.replace("<", "&lt;");
		value = value.replace(">", "&gt;");
		return value;
	}
}

class JSysInfoPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JEditorPane editorPane;
	
	public JSysInfoPanel() {
		super(new BorderLayout());
		
		editorPane = new JEditorPane();
		editorPane.setEditable(false);
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		editorPane.setContentType("text/html");
		editorPane.setText("");
		
		JButton btnSave = new JButton(new AbstractAction("save") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onSave();
			}
		});

		JButton btnGc = new JButton(new AbstractAction("garbage collect") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onGc();
			}
		});

		JPanel btnPanel = new JPanel(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		btnPanel.add(btnSave, BorderLayout.EAST);
		btnPanel.add(btnGc, BorderLayout.WEST);
		
		add(new JScrollPane(editorPane), BorderLayout.CENTER);
		add(btnPanel, BorderLayout.SOUTH);
	}
	
	public void setText(String message) {
		editorPane.setText(message);
		editorPane.setSelectionStart(0);
		editorPane.setSelectionEnd(0);
	}
	
	protected void onGc() {
		SystemUtil.gc();
	}
	
	protected void onSave() {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File("sysinfo.html"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = chooser.getSelectedFile();
		try {
			FileOutputStream os = new FileOutputStream(file);
			try {
				Writer wr = new OutputStreamWriter(os, Charset.forName("UTF-8"));
				try {
					wr.write(editorPane.getText());
					wr.flush();

				} finally {
					wr.close();
				}

			} finally {
				os.close();
			}
			
		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
}
