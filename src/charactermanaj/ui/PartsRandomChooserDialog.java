package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;

import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * パーツのランダム選択ダイアログ.<br>
 *
 * @author seraphy
 */
public class PartsRandomChooserDialog extends JDialog {

	private static final long serialVersionUID = -8427874726724107481L;

	protected static final String STRINGS_RESOURCE = "languages/partsrandomchooserdialog";

	/**
	 * メインフレームとの間でパーツの選択状態の取得・設定を行うためのインターフェイス.<br>
	 */
	public interface PartsSetSynchronizer {

		/**
		 * 現在フレームで設定されているパーツセットを取得する.
		 *
		 * @return
		 */
		PartsSet getCurrentPartsSet();

		/**
		 * ランダム選択パネルのパーツセットでフレームを設定する.
		 *
		 * @param partsSet
		 */
		void setPartsSet(PartsSet partsSet);

		/**
		 * 指定されたパーツがランダム選択対象外であるか？
		 *
		 * @param partsIdentifier
		 *            パーツ
		 * @return 対象外であればtrue
		 */
		boolean isExcludePartsIdentifier(PartsIdentifier partsIdentifier);

		/**
		 * 指定したパーツがランダム選択対象外であるか設定する.
		 *
		 * @param partsIdentifier
		 *            パーツ
		 * @param exclude
		 *            対象外であればtrue
		 */
		void setExcludePartsIdentifier(PartsIdentifier partsIdentifier,
				boolean exclude);
	}

	/**
	 * ランダム選択パネルを縦に並べるボックス
	 */
	private Box centerPnl;

	/**
	 * キャラクターデータ
	 */
	private CharacterData characterData;

	/**
	 * メインフレームとの同期用
	 */
	private PartsSetSynchronizer partsSync;

	/**
	 * 一括ランダムアクション
	 */
	private Action actRandomAll;

	/**
	 * 選択を戻すアクション
	 */
	private Action actBack;

	/**
	 * 閉じるアクション
	 */
	private Action actCancel;

	/**
	 * 履歴
	 */
	private LinkedList<Map<RandomChooserPanel, PartsIdentifier>> history = new LinkedList<Map<RandomChooserPanel, PartsIdentifier>>();

	/**
	 * 最大の履歴保持数
	 */
	private int maxHistory;

	/**
	 * コンストラクタ
	 *
	 * @param parent
	 *            メインフレーム(親)
	 * @param characterData
	 *            キャラクターデータ
	 * @param partsSync
	 *            メインフレームとの同期用
	 */
	public PartsRandomChooserDialog(JFrame parent, CharacterData characterData,
			PartsSetSynchronizer partsSync) {
		super(parent, false);
		try {
			if (characterData == null || partsSync == null) {
				throw new IllegalArgumentException();
			}

			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					onClose();
				}
			});

			this.characterData = characterData;
			this.partsSync = partsSync;

			AppConfig appConfig = AppConfig.getInstance();
			this.maxHistory = appConfig.getRandomChooserMaxHistory();
			if (this.maxHistory < 0) {
				this.maxHistory = 0;
			}

			initLayout();

			pack();
			setLocationRelativeTo(parent);

		} catch (RuntimeException ex) {
			dispose();
			throw ex;
		}
	}

	/**
	 * レイアウトを行う.
	 */
	private void initLayout() {
		Properties strings = LocalizedResourcePropertyLoader
				.getCachedInstance().getLocalizedProperties(STRINGS_RESOURCE);

		setTitle(strings.getProperty("partsRandomChooser"));

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		this.centerPnl = Box.createVerticalBox();

		ActionListener changePartsIdentifierListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (eventLock.get() == 0) {
					onChangePartsIdentifiers();
				}
			}
		};

		PartsSet partsSet = partsSync.getCurrentPartsSet();
		eventLock.incrementAndGet();
		try {
			for (PartsCategory category : characterData.getPartsCategories()) {
				List<PartsIdentifier> partsIdentifiers = partsSet.get(category);
				int partsLen = (partsIdentifiers != null) ? partsIdentifiers
						.size() : 0;
				boolean enable = true;
				if (partsLen < 1) {
					partsLen = 1; // 未選択の場合でも1つは作成する.
					enable = false; // 未選択の場合はディセーブルとする.
				}

				for (int partsIdx = 0; partsIdx < partsLen; partsIdx++) {
					PartsIdentifier partsIdentifier = null;
					if (partsIdentifiers != null
							&& partsIdx < partsIdentifiers.size()) {
						partsIdentifier = partsIdentifiers.get(partsIdx);
					}
					boolean lastInCategory = (partsIdx == partsLen - 1);

					int idx = centerPnl.getComponentCount();
					RandomChooserPanel pnl = addPartsChooserPanel(centerPnl,
							idx, category, lastInCategory,
							changePartsIdentifierListener);

					// 未選択の場合、もしくは複数選択カテゴリの場合はランダムはディセーブルとする
					pnl.setEnableRandom(enable
							&& !category.isMultipleSelectable());

					if (partsIdentifier != null) {
						pnl.setSelectedPartsIdentifier(partsIdentifier);
					}
				}
			}

		} finally {
			eventLock.decrementAndGet();
		}

		JScrollPane scr = new JScrollPane(centerPnl) {
			private static final long serialVersionUID = 1L;

			@Override
			public JScrollBar createVerticalScrollBar() {
				JScrollBar sb = super.createVerticalScrollBar();
				sb.setUnitIncrement(12);
				return sb;
			}
		};
		scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		contentPane.add(scr, BorderLayout.CENTER);

		this.actRandomAll = new AbstractAction(strings.getProperty("randomAll")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onRandomAll();
			}
		};

		this.actBack = new AbstractAction(strings.getProperty("back")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onBack();
			}
		};

		this.actCancel = new AbstractAction(strings.getProperty("close")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onClose();
			}
		};

		JButton btnClose = new JButton(actCancel);
		JButton btnRandomAll = new JButton(actRandomAll);
		JButton btnBack = new JButton(actBack);

		Box btnPanel = Box.createHorizontalBox();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 42));

		btnPanel.add(btnRandomAll);
		btnPanel.add(btnBack);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.add(btnClose);

		contentPane.add(btnPanel, BorderLayout.SOUTH);

		JRootPane rootPane = getRootPane();
		rootPane.setDefaultButton(btnRandomAll);

		Toolkit tk = Toolkit.getDefaultToolkit();
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,
				tk.getMenuShortcutKeyMask()), "closeDialog");
		am.put("closeDialog", actCancel);

		addHistory(getSelection());
		updateUIState();
	}

	/**
	 * ボタンの状態を設定する.
	 */
	protected void updateUIState() {
		actBack.setEnabled(history.size() > 1);
	}

	/**
	 * ダイアログを破棄して閉じる.
	 */
	protected void onClose() {
		dispose();
	}

	/**
	 * パネル構築時、および一括ランダム選択時などでパーツのコンボボックスの選択が複数変更される場合に
	 * イベントを一度だけ処理するようにグループ化するためのロック.
	 */
	private final AtomicInteger eventLock = new AtomicInteger(0);

	/**
	 * センターパネル上に配置したランダム選択パネルのリストを取得する.<br>
	 * (ランダム選択パネルの個数は実行時に自由に可変できるため.)
	 *
	 * @return ランダム選択パネルのリスト
	 */
	protected List<RandomChooserPanel> getRandomChooserPanels() {
		ArrayList<RandomChooserPanel> panels = new ArrayList<RandomChooserPanel>();
		int mx = centerPnl.getComponentCount();
		for (int idx = 0; idx < mx; idx++) {
			Component comp = centerPnl.getComponent(idx);
			if (comp instanceof RandomChooserPanel) {
				RandomChooserPanel pnl = (RandomChooserPanel) comp;
				panels.add(pnl);
			}
		}
		return panels;
	}

	/**
	 * 現在選択中の状態を取得する.
	 *
	 * @return
	 */
	protected Map<RandomChooserPanel, PartsIdentifier> getSelection() {
		HashMap<RandomChooserPanel, PartsIdentifier> selection = new HashMap<RandomChooserPanel, PartsIdentifier>();

		for (RandomChooserPanel pnl : getRandomChooserPanels()) {
			PartsIdentifier partsIdentifier = pnl.getSelectedPartsIdentifier();
			selection.put(pnl, partsIdentifier);
		}

		return selection;
	}

	/**
	 * 履歴に追加する.
	 *
	 * @param selection
	 */
	protected void
			addHistory(Map<RandomChooserPanel, PartsIdentifier> selection) {
		if (selection == null || selection.isEmpty()) {
			return;
		}

		// 履歴に追加する.
		history.addLast(selection);

		// 最大数を越えた場合は除去する
		while (history.size() > maxHistory) {
			history.removeFirst();
		}
		updateUIState();
	}

	/**
	 * 前回の選択状態に戻す
	 */
	protected void onBack() {
		if (history.size() <= 1) {
			return;
		}

		// ヒストリーの直前のものを取り出す
		// 先頭のものは現在表示中のものなので、2つ取り出す必要がある.
		history.removeLast();
		Map<RandomChooserPanel, PartsIdentifier> selection = history.getLast();

		// すべてのランダム選択パネルに再適用する.
		eventLock.incrementAndGet();
		try {
			for (Map.Entry<RandomChooserPanel, PartsIdentifier> entry : selection
					.entrySet()) {
				RandomChooserPanel pnl = entry.getKey();
				PartsIdentifier partsIdentifier = entry.getValue();
				pnl.setSelectedPartsIdentifier(partsIdentifier);
			}

			PartsSet partsSet = makePartsSet(selection.values());
			if (!partsSet.isEmpty()) {
				partsSync.setPartsSet(partsSet);
			}

		} finally {
			eventLock.decrementAndGet();
		}

		updateUIState();
	}

	/**
	 * 一括ランダム選択
	 */
	protected void onRandomAll() {
		eventLock.incrementAndGet();
		try {
			for (RandomChooserPanel pnl : getRandomChooserPanels()) {
				if (pnl.isEnableRandom()) {
					// ランダム選択を有効としているものだけを対象とする.
					pnl.selectRandom();
				}
			}
			onChangePartsIdentifiers();

		} finally {
			eventLock.decrementAndGet();
		}
	}

	/**
	 * パーツの選択からパーツセットを生成して返す.
	 *
	 * @param selection
	 * @return
	 */
	protected PartsSet makePartsSet(Collection<PartsIdentifier> selection) {
		PartsSet partsSet = new PartsSet();
		for (PartsIdentifier partsIdentifier : selection) {
			if (partsIdentifier != null) {
				PartsCategory category = partsIdentifier.getPartsCategory();
				partsSet.appendParts(category, partsIdentifier, null); // 色は不問とする
			}
		}
		return partsSet;
	}

	/**
	 * パーツの選択が変更されたことを通知される.<br>
	 * 現在のランダム選択状態を、プレビューの状態に反映させる.<brr>
	 */
	protected void onChangePartsIdentifiers() {

		Map<RandomChooserPanel, PartsIdentifier> selection = getSelection();

		PartsSet partsSet = makePartsSet(selection.values());
		if (!partsSet.isEmpty()) {
			partsSync.setPartsSet(partsSet);
			addHistory(selection);
		}
	}

	/**
	 * アイテムごとのランダム選択パネル
	 *
	 * @author seraphy
	 */
	protected class RandomChooserPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		private EventListenerList listeners = new EventListenerList();

		private JCheckBox label;

		private JComboBox partsCombo;

		private JToggleButton btnReject;

		public RandomChooserPanel(final PartsCategory category,
				final boolean lastInCategory) {
			Properties strings = LocalizedResourcePropertyLoader
					.getCachedInstance().getLocalizedProperties(
							STRINGS_RESOURCE);

			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(3, 3, 3, 3),
					BorderFactory.createCompoundBorder(
							BorderFactory.createEtchedBorder(),
							BorderFactory.createEmptyBorder(3, 3, 3, 3))));
			setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.anchor = GridBagConstraints.EAST;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 1.;
			gbc.weighty = 0.;

			String categoryName = category.getLocalizedCategoryName();
			this.label = new JCheckBox(categoryName, true);
			add(label, gbc);

			JButton btnRandom = new JButton(new AbstractAction(
					strings.getProperty("random")) {
				private static final long serialVersionUID = -1;

				public void actionPerformed(ActionEvent e) {
					onClickRandom(e);
				}
			});
			gbc.gridx = 1;
			gbc.weightx = 0;
			add(btnRandom, gbc);

			ArrayList<PartsIdentifier> partsList = new ArrayList<PartsIdentifier>();
			partsList.addAll(characterData.getPartsSpecMap(category).keySet());
			Collections.sort(partsList);
			if (category.isMultipleSelectable()) {
				// 複数選択カテゴリは未選択状態が可能なため先頭に空行を入れる.
				partsList.add(0, null);
			}

			this.partsCombo = new JComboBox(
					partsList.toArray(new PartsIdentifier[partsList.size()]));

			partsCombo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onSelectChangePartsIdentifier(e);
				}
			});

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.weightx = 1.;
			add(partsCombo, gbc);

			this.btnReject = new JToggleButton(new AbstractAction(
					strings.getProperty("reject")) {
				private static final long serialVersionUID = -1;

				public void actionPerformed(ActionEvent e) {
					onClickReject(e);
				}
			});
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.weightx = 0;
			add(btnReject, gbc);

			if (category.isMultipleSelectable() && lastInCategory) {
				JButton btnAdd = new JButton(new AbstractAction(
						strings.getProperty("add")) {
					private static final long serialVersionUID = -1;

					public void actionPerformed(ActionEvent e) {
						onClickAdd(e);
					}
				});
				gbc.gridx = 1;
				gbc.gridy = 2;
				gbc.weightx = 0;
				add(btnAdd, gbc);
			}

			updateButtonState();
		}

		public void addActionListener(ActionListener l) {
			listeners.add(ActionListener.class, l);
		}

		public void removeActionListener(ActionListener l) {
			listeners.remove(ActionListener.class, l);
		}

		public boolean isEnableRandom() {
			return label.isSelected();
		}

		public void setEnableRandom(boolean selected) {
			label.setSelected(selected);
		}

		public PartsIdentifier getSelectedPartsIdentifier() {
			return (PartsIdentifier) partsCombo.getSelectedItem();
		}

		public void setSelectedPartsIdentifier(PartsIdentifier partsIdentifier) {
			partsCombo.setSelectedItem(partsIdentifier);
		}

		protected void updateButtonState() {
			PartsIdentifier partsIdentifier = getSelectedPartsIdentifier();
			if (partsIdentifier == null) {
				btnReject.setEnabled(false);
				return;
			}
			boolean exclude = partsSync
					.isExcludePartsIdentifier(partsIdentifier);
			btnReject.setSelected(exclude);
			btnReject.setEnabled(true);
		}

		protected void onSelectChangePartsIdentifier(ActionEvent e) {
			updateButtonState();

			ActionEvent evt = new ActionEvent(this,
					ActionEvent.ACTION_PERFORMED, "selectChangePartsIdentifier");
			for (ActionListener l : listeners
					.getListeners(ActionListener.class)) {
				l.actionPerformed(evt);
			}
		}

		protected void onClickReject(ActionEvent e) {
			PartsIdentifier partsIdentifier = getSelectedPartsIdentifier();
			if (partsIdentifier == null) {
				return;
			}
			boolean exclude = partsSync
					.isExcludePartsIdentifier(partsIdentifier);
			partsSync.setExcludePartsIdentifier(partsIdentifier, !exclude);
			updateButtonState();
		}

		protected void onClickRandom(ActionEvent e) {
			selectRandom();
		}

		public void selectRandom() {
			ArrayList<PartsIdentifier> partsIdentifiers = new ArrayList<PartsIdentifier>();
			int mx = partsCombo.getItemCount();
			for (int idx = 0; idx < mx; idx++) {
				PartsIdentifier partsIdentifier = (PartsIdentifier) partsCombo
						.getItemAt(idx);
				if (partsIdentifier != null) {
					if (!partsSync.isExcludePartsIdentifier(partsIdentifier)) {
						partsIdentifiers.add(partsIdentifier);
					}
				}
			}

			int len = partsIdentifiers.size();
			if (len == 0) {
				// 選択しようがないので何もしない.
				return;
			}

			Random rng = new Random();
			int selidx = rng.nextInt(len);

			setSelectedPartsIdentifier(partsIdentifiers.get(selidx));
		}

		protected void onClickAdd(ActionEvent e) {
			// 何もしない.
		}
	}

	/**
	 * カテゴリのパーツのランダム選択パネルを作成する.<br>
	 * パネルが追加ボタンをもつときには、作成されたパネルにもパーツ変更リスナは適用される.<br>
	 *
	 * @param centerPnl
	 *            追加されるパネル
	 * @param addPos
	 *            追加する位置
	 * @param category
	 *            カテゴリ
	 * @param lastInCategory
	 *            作成するパネルに、追加ボタンをつけるか？
	 * @param changePartsIdentifierListener
	 *            パーツ選択が変わった場合のリスナ
	 * @return 作成されたランダム選択パネル
	 */
	protected RandomChooserPanel addPartsChooserPanel(final Box centerPnl,
			final int addPos,
			final PartsCategory category,
			final boolean lastInCategory,
			final ActionListener changePartsIdentifierListener) {
		RandomChooserPanel pnl = new RandomChooserPanel(category,
				lastInCategory) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClickAdd(ActionEvent e) {
				int mx = centerPnl.getComponentCount();
				for (int idx = 0; idx < mx; idx++) {
					Component comp = centerPnl.getComponent(idx);
					if (comp.equals(this)) {
						// 同じカテゴリのものを追加する
						addPartsChooserPanel(centerPnl, idx + 1, category,
								lastInCategory, changePartsIdentifierListener);
						centerPnl.validate();
						// Addボタンを非表示にする.
						((JButton) e.getSource()).setVisible(false);
						break;
					}
				}
			}
		};

		// パーツ選択変更を通知するリスナを設定する.
		pnl.addActionListener(changePartsIdentifierListener);

		centerPnl.add(pnl, addPos);
		return pnl;
	}
}
