package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import charactermanaj.Main;
import charactermanaj.model.io.WorkingSetPersist;
import charactermanaj.ui.util.FileDropTarget;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 起動時にキャラクターデータディレクトリを選択するためのモーダルダイアログ.<br>
 * 
 * @author seraphy
 */
public class SelectCharatersDirDialog extends JDialog {

	private static final long serialVersionUID = -888834575856349442L;

	private static final Logger logger = Logger.getLogger(SelectCharatersDirDialog.class.getName());

	/**
	 * 最後に使用したキャラクターデータディレクトリと、その履歴情報.
	 */
	private final RecentCharactersDir recentCharactersDir;
	
	/**
	 * 既定のディレクトリ
	 */
	private File defaultCharactersDir;
	
	
	/**
	 * 選択されたディレクトリ
	 */
	private File selectedCharacterDir;
	
	/**
	 * 次回起動時に問い合わせない
	 */
	private boolean doNotAskAgain;
	
	
	/**
	 * ディレクトリ選択コンボ
	 */
	private JComboBox combDir;
	
	/**
	 * 次回起動時に問い合わせないチェックボックス
	 */
	private JCheckBox chkDoNotAsk;
	
	
	
	public File getDefaultCharactersDir() {
		return defaultCharactersDir;
	}

	public void setDefaultCharactersDir(File defaultCharactersDir) {
		this.defaultCharactersDir = defaultCharactersDir;
	}
	
	public File getSelectedCharacterDir() {
		return selectedCharacterDir;
	}
	
	public boolean isDoNotAskAgain() {
		return doNotAskAgain;
	}
	
	/**
	 * コンストラクタ
	 * 
	 * @param parent
	 *            親(通常は、null)
	 * @param recentCharactersDir
	 *            最後に使用したキャラクターデータディレクトリと、その履歴情報.
	 */
	protected SelectCharatersDirDialog(JFrame parent, RecentCharactersDir recentCharactersDir) {
		super(parent, true);
		try {
			if (recentCharactersDir == null) {
				throw new IllegalArgumentException(
						"recentCharactersDirにnullは指定できません。");
			}
			this.recentCharactersDir = recentCharactersDir;
			
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					onClose();
				}
			});
			initComponent();
			
		} catch (RuntimeException ex) {
			logger.log(Level.SEVERE, "キャラクターディレクトリ選択ダイアログの生成に失敗しました。", ex);
			dispose();
			throw ex;
		}
	}
	
	private void initComponent() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties("languages/selectCharatersDirDialog");
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout(3, 3));
		
		AbstractAction actOk = new AbstractAction(strings.getProperty("btn.ok")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		};
		
		AbstractAction actClose = new AbstractAction(strings.getProperty("btn.cancel")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};

		AbstractAction actBrowse = new AbstractAction(strings.getProperty("btn.chooseDir")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onBrowse();
			}
		};

		AbstractAction actRemoveRecent = new AbstractAction(strings.getProperty("btn.clearRecentList")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onRemoveRecent();
			}
		};

		AbstractAction actRemoveWorkingSets = new AbstractAction(
				strings.getProperty("btn.clearWorkingSets")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				onRemoveWorkingSets();
			}
		};

		final JButton btnRemoveWorkingSets = new JButton(actRemoveWorkingSets);
		final JButton btnRemoveRecent = new JButton(actRemoveRecent);
		final JButton btnOK = new JButton(actOk);
		final JButton btnCancel = new JButton(actClose);
		final JButton btnBroseForDir = new JButton(actBrowse);

		Toolkit tk = Toolkit.getDefaultToolkit();
		final JRootPane rootPane = getRootPane();
		FocusAdapter focusAdapter = new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				JButton btn = (JButton) e.getSource();
				rootPane.setDefaultButton(btn);
			}
			@Override
			public void focusLost(FocusEvent e) {
				rootPane.setDefaultButton(btnOK);
			}
		};
		rootPane.setDefaultButton(btnOK);
		InputMap im = rootPane.getInputMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "close");
		rootPane.getActionMap().put("close", actClose);
		
		btnRemoveWorkingSets.addFocusListener(focusAdapter);
		btnRemoveRecent.addFocusListener(focusAdapter);
		btnOK.addFocusListener(focusAdapter);
		btnCancel.addFocusListener(focusAdapter);
		btnBroseForDir.addFocusListener(focusAdapter);


		JPanel dirPanel = new JPanel(new BorderLayout(3, 3));
		dirPanel.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 3));
		
		JLabel lbl = new JLabel(strings.getProperty("caption"), JLabel.CENTER);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
		lbl.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		Dimension dim = lbl.getPreferredSize();
		dim.width = Integer.parseInt(strings.getProperty("width"));
		lbl.setPreferredSize(dim);
		dirPanel.add(lbl, BorderLayout.NORTH);

		combDir = new JComboBox();
		combDir.setEditable(true);
		
		dirPanel.add(combDir, BorderLayout.CENTER);
		
		dirPanel.add(new JLabel(strings.getProperty("lbl.dir")), BorderLayout.WEST);
		dirPanel.add(btnBroseForDir, BorderLayout.EAST);
		
		contentPane.add(dirPanel, BorderLayout.NORTH);
		
		JPanel btnPanel = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		btnPanel.setLayout(gbl);
		
		chkDoNotAsk = new JCheckBox(strings.getProperty("chk.doNotAskAgein"));
		chkDoNotAsk.setSelected(recentCharactersDir.isDoNotAskAgain());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 5;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		
		btnPanel.add(chkDoNotAsk, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		btnPanel.add(btnRemoveRecent, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		btnPanel.add(btnRemoveWorkingSets, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		
		btnPanel.add(Box.createGlue(), gbc);

		gbc.gridx = Main.isLinuxOrMacOSX() ? 4 : 3;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;

		btnPanel.add(btnOK, gbc);


		gbc.gridx = Main.isLinuxOrMacOSX() ? 3 : 4;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		btnPanel.add(btnCancel, gbc);
		
		gbc.gridx = 5;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.ipadx = 32;
		gbc.ipady = 0;
		
		btnPanel.add(Box.createGlue(), gbc);

		setTitle(strings.getProperty("title"));
		setResizable(false);
		
		contentPane.add(btnPanel, BorderLayout.SOUTH);

		// フォルダのドロップによる入力を許可
		new DropTarget(this, new FileDropTarget() {
			@Override
			protected void onDropFiles(List<File> dropFiles) {
				setSelectFile(dropFiles);
			}

			@Override
			protected void onException(Exception ex) {
				ErrorMessageHelper.showErrorDialog(SelectCharatersDirDialog.this, ex);
			}
		});

		pack();
		setLocationRelativeTo(null);
	}
	
	/**
	 * ドロップによるファイル名の設定.<br>
	 * 最初の1つだけを使用する.<br>
	 * リストが空であるか、最初のファイルが、フォルダでなければ何もしない.<br>
	 * 
	 * @param dropFiles
	 *            ドロップされたファイルリスト
	 */
	protected void setSelectFile(List<File> dropFiles) {
		if (dropFiles.isEmpty()) {
			return;
		}
		File dropFile = dropFiles.get(0);
		if ( !dropFile.exists() || !dropFile.isDirectory()) {
			return;
		}
		combDir.setSelectedItem(dropFile);
	}

	protected void onClose() {
		selectedCharacterDir = null;
		dispose();
	}
	
	protected void onOK() {
		try {
			Object value = combDir.getSelectedItem();
			if (value != null && value instanceof String) {
				value = new File((String) value);
			}
			
			if (value != null && value instanceof File) {
				File file = (File) value;
				if (!file.exists()) {
					boolean result = file.mkdirs();
					logger.log(Level.INFO, "mkdirs(" + file+ ") succeeded=" + result);
				}
				if (file.isDirectory()) {
					logger.log(Level.CONFIG, "selectedCharactersDir=" + file);
					selectedCharacterDir = file;
					doNotAskAgain = chkDoNotAsk.isSelected();
					dispose();
					return;
				}
			}

			// 選択されていないかファイルでない場合
			Toolkit tk = Toolkit.getDefaultToolkit();
			tk.beep();

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void onBrowse() {
		try {
			Object selectedItem = combDir.getSelectedItem();
			String directoryTxt = null;
			if (selectedItem != null) {
				directoryTxt = selectedItem.toString();
			}
			JFileChooser dirChooser = new JFileChooser(directoryTxt);
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (dirChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			File dir = dirChooser.getSelectedFile();
			if (dir != null) {
				combDir.setSelectedItem(dir);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}

	protected void onRemoveWorkingSets() {
		try {
			Properties strings = LocalizedResourcePropertyLoader
					.getCachedInstance().getLocalizedProperties(
							"languages/selectCharatersDirDialog");

			// 削除の確認ダイアログ
			if (JOptionPane.showConfirmDialog(this,
					strings.getProperty("confirm.clearWorkingSets"),
					strings.getProperty("confirm.clearWorkingSets.title"),
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
				return;
			}

			// 全てのワーキングセットをクリアする.
			WorkingSetPersist workingSetPersist = WorkingSetPersist
					.getInstance();
			workingSetPersist.removeAllWorkingSet();

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void onRemoveRecent() {
		try {
			Object current = combDir.getSelectedItem();

			recentCharactersDir.clrar();
			setRecents();

			if (current != null) {
				combDir.setSelectedItem(current);
			}

		} catch (Exception ex) {
			ErrorMessageHelper.showErrorDialog(this, ex);
		}
	}
	
	protected void setRecents() {
		// 現在の候補をクリア.
		while (combDir.getItemCount() > 0) {
			combDir.removeItemAt(0);
		}

		// 前回使用したディレクトリを最優先候補
		ArrayList<File> priorityDirs = new ArrayList<File>();
		File lastUseCatacterDir = recentCharactersDir.getLastUseCharacterDir();
		if (lastUseCatacterDir != null) {
			if (defaultCharactersDir != null && !lastUseCatacterDir.equals(defaultCharactersDir)) {
				combDir.addItem(lastUseCatacterDir);
				priorityDirs.add(lastUseCatacterDir);
			}
		}
		// デフォルトのキャラクターデータを第２位に設定
		if (defaultCharactersDir != null) {
			combDir.addItem(defaultCharactersDir);
			priorityDirs.add(defaultCharactersDir);
		}
		// それ以外の履歴を設定
		for (File charactersDir : recentCharactersDir
				.getRecentCharacterDirsOrderByNewly()) {
			if (charactersDir == null) {
				continue;
			}
			if (!priorityDirs.contains(charactersDir)) {
				combDir.addItem(charactersDir);
			}
		}
		// 第一候補を選択状態とする.
		if (combDir.getItemCount() > 0) {
			combDir.setSelectedIndex(0);
		}
	}
	

	/**
	 * キャラクターデータディレクトリを履歴および既定のディレクトリから、任意の使用するディレクトリを選択する.<br>
	 * 既定のディレクトリは常に選択候補とする.<br>
	 * 新しいディレクトリを指定した場合は、履歴に追加される.<br>
	 * 「再度問い合わせなし」を選択している場合で、そのディレクトリが実在すれば、選択ダイアログを表示せず、それを返す.<br>
	 * 
	 * @param defaultCharacterDir
	 *            既定のディレクトリ
	 * @return 選択したディレクトリ、キャンセルした場合はnull
	 */
	public static File getCharacterDir(File defaultCharacterDir) {
		RecentCharactersDir recentChars;
		try {
			recentChars = RecentCharactersDir.load();

		} catch (Exception ex) {
			logger.log(Level.WARNING, "最後に使用したキャラクターディレクトリ情報の読み込みに失敗しました。", ex);
			recentChars = null;
		}
		if (recentChars == null) {
			recentChars = new RecentCharactersDir();
		}

		logger.log(Level.CONFIG, "RecentCharacterDirs.doNotAskAgain=" + recentChars.isDoNotAskAgain());
		if (recentChars.isDoNotAskAgain()) {
			// 「再度問い合わせ無し」の場合で、過去のディレクトリが有効であれば、それを返す.
			File recentCharDir = recentChars.getLastUseCharacterDir();
			if (recentCharDir != null && recentCharDir.exists() && recentCharDir.isDirectory()) {
				return recentCharDir;
			}
			recentChars.setDoNotAskAgain(false); // 不正である場合は「再度問い合わせ無し」をリセットする.
		}

		File selectedCharacterDir;
		SelectCharatersDirDialog dlg = new SelectCharatersDirDialog(null, recentChars);
		dlg.setDefaultCharactersDir(defaultCharacterDir);
		dlg.setRecents();
		dlg.setVisible(true);
		
		selectedCharacterDir = dlg.getSelectedCharacterDir();
		if (selectedCharacterDir != null) {
			recentChars.setLastUseCharacterDir(selectedCharacterDir);
			try {
				recentChars.setDoNotAskAgain(dlg.isDoNotAskAgain());
				recentChars.saveRecents();

			} catch (Exception ex) {
				logger.log(Level.WARNING, "最後に使用したキャラクターディレクトリ情報の保存に失敗しました。",
						ex);
			}
		}
		return selectedCharacterDir;
	}
}
