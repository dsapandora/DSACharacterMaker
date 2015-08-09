package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import charactermanaj.Main;
import charactermanaj.graphics.colormodel.ColorModel;
import charactermanaj.graphics.colormodel.ColorModels;
import charactermanaj.graphics.filters.ColorConv;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.model.AppConfig;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.ui.model.ColorChangeEvent;
import charactermanaj.ui.model.ColorChangeListener;
import charactermanaj.ui.util.SpinnerWheelSupportListener;
import charactermanaj.util.LocalizedResourcePropertyLoader;


/**
 * カラーダイアログ.<br>
 * カラーダイアログはカテゴリ別に関連づけられており、カテゴリ内の各レイヤーに対応するタブを持つ.<br>
 * 
 * @author seraphy
 */
public class ColorDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(ColorDialog.class.getName());

	/**
	 * このカラーダイアログが対応するカテゴリ
	 */
	private final PartsCategory partsCategory;

	/**
	 * レイヤーごとのタブのマップ
	 */
	private HashMap<Layer, ColorDialogTabPanel> tabs = new HashMap<Layer, ColorDialogTabPanel>();
	
	/**
	 * タブペイン
	 */
	private JTabbedPane tabbedPane;
	
	/**
	 * レイヤーに対するタブインデックスのマップ
	 */
	private HashMap<Layer, Integer> tabbedPaneIndexMap = new HashMap<Layer, Integer>();
	
	/**
	 * 色変更イベントのリスナのコレクション
	 */
	private LinkedList<ColorChangeListener> listeners = new LinkedList<ColorChangeListener>();

	/**
	 * キャプションのプレフィックス
	 */
	private String captionBase;
	
	/**
	 * 現在表示しているカラー情報の対象としているパーツ識別子
	 */
	private PartsIdentifier partsIdentifier;
	
	/**
	 * カテゴリ全体に適用するチェックボックス
	 */
	private JCheckBox chkApplyAll;
	
	/**
	 * リセットアクション
	 */
	private Action actReset;

	
	/**
	 * コンストラクタ
	 * 
	 * @param parent
	 *            親フレーム
	 * @param partsCategory
	 *            カテゴリ
	 * @param colorGroups
	 *            選択可能なカラーグループのコレクション
	 */
	public ColorDialog(JFrame parent, PartsCategory partsCategory, Collection<ColorGroup> colorGroups) {
		super(parent);
		this.partsCategory = partsCategory;

		final Properties strings = LocalizedResourcePropertyLoader.getCachedInstance().getLocalizedProperties("languages/colordialog");
		
		String caption = strings.getProperty("colordialog.caption");
		String name = partsCategory.getLocalizedCategoryName();
		captionBase = caption + name;
		setTitle(captionBase);

		// ダイアログを非表示にする.
		final AbstractAction actHide = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		};

		// 非表示アクション
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// ウィンドウの閉じるボタン押下により、ダイアログを「非表示」にする.
				actHide.actionPerformed(new ActionEvent(ColorDialog.this, 0, "closing"));
			}
		});
		
		Container container = getContentPane();
		
		this.tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
		for (final Layer layer : partsCategory.getLayers()) {
			
			final ColorDialogTabPanel tabContainer = new ColorDialogTabPanel(this, layer, colorGroups);
			final ColorChangeListener innerListener = new ColorChangeListener() {
				private Semaphore semaphore = new Semaphore(1); // イベントが循環発生することを防ぐ
				public void onColorChange(ColorChangeEvent event) {
					if (semaphore.tryAcquire()) {
						try {
							ColorDialog.this.fireColorChangeEvent(layer, false);
						} finally {
							semaphore.release();
						}
					}
				}
				public void onColorGroupChange(ColorChangeEvent event) {
					if (semaphore.tryAcquire()) {
						try {
							ColorDialog.this.fireColorGroupChangeEvent(layer);
							ColorDialog.this.fireColorChangeEvent(layer, false);
						} finally {
							semaphore.release();
						}
					}
				}
			};
			tabContainer.addColorChangeListener(innerListener);
			tabContainer.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					// レイヤータブが切り替えられるたびに、そのレイヤーの状況にあわせてリセットボタンの状態を更新する.
					updateResetButton(tabContainer);
				}
			});
			tabContainer.addPropertyChangeListener("colorConvertParameter", new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
							// レイヤーの情報が変るたびにリセットボタンの状態を更新する
					updateResetButton(tabContainer);
				}
			});

			tabbedPane.addTab(layer.getLocalizedName(), tabContainer);
			tabbedPaneIndexMap.put(layer, tabbedPane.getTabCount() - 1);
			tabs.put(layer, tabContainer);
		}

		// 適用アクション
		Action actApply = new AbstractAction(strings.getProperty("button.apply")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				apply();
			}
		};
		// リセットアクション
		actReset = new AbstractAction(strings.getProperty("button.reset")) {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				ColorDialogTabPanel tab = (ColorDialogTabPanel) tabbedPane
						.getSelectedComponent();
				if (tab != null) {
					resetColor(tab);
					apply();
				}
			}
		};

		JPanel btnPanel = new JPanel();
		btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		GridBagLayout gbl = new GridBagLayout();
		btnPanel.setLayout(gbl);

		int colIdx = 0;
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = colIdx++;
		gbc.gridy = 0;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		chkApplyAll = new JCheckBox(strings.getProperty("checkbox.applyAllItems"));
		chkApplyAll.setSelected(!partsCategory.isMultipleSelectable());
		chkApplyAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// すべてに適用のチェックが変更された場合は全レイヤーの色の変更通知を出す.
				apply();
			}
		});
		btnPanel.add(chkApplyAll, gbc);
		
		gbc.gridx = colIdx++;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		btnPanel.add(Box.createHorizontalGlue(), gbc);

		// 「適用」ボタン、アプリケーション設定により有無を選択できる.
		JButton btnApply = null;
		AppConfig appConfig = AppConfig.getInstance();
		if ( !appConfig.isEnableAutoColorChange()) {
			gbc.gridx = colIdx++;
			gbc.gridy = 0;
			gbc.weightx = 0.;
			btnApply = new JButton(actApply);
			btnPanel.add(btnApply, gbc);
		}

		gbc.gridx = colIdx++;
		gbc.gridy = 0;
		gbc.weightx = 0.;
		JButton btnReset = new JButton(actReset);
		btnPanel.add(btnReset, gbc);

		container.setLayout(new BorderLayout());
		container.add(tabbedPane, BorderLayout.CENTER);
		container.add(btnPanel, BorderLayout.SOUTH);
		

		Toolkit tk = Toolkit.getDefaultToolkit();
		JRootPane rootPane = getRootPane();

		// 「適用」ボタンがある場合は、デフォルトボタンに設定する.
		if (btnApply != null) {
			rootPane.setDefaultButton(btnApply);
		}
		
		// CTRL-Wでウィンドウを非表示にする. (ESCでは非表示にしない)
		InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = rootPane.getActionMap();
		
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, tk.getMenuShortcutKeyMask()), "hideColorDialog");
		am.put("hideColorDialog", actHide);
		
		pack();
	}
	
	/**
	 * 各レイヤーのカラー情報のタブが開かれた場合、もしくはカラーの設定値が変更されるたびに 呼び出されて、リセットボタンの状態を変更します.<br>
	 * 現在のタブが選択しているパネルと異なるパネルからの要求については無視されます.<br>
	 * 
	 * @param panel
	 *            色情報が変更された、もしくは開かれた対象パネル
	 */
	protected void updateResetButton(ColorDialogTabPanel panel) {
		ColorDialogTabPanel currentPanel = (ColorDialogTabPanel) tabbedPane.getSelectedComponent();
		if (currentPanel != null && currentPanel.equals(panel)) {
			actReset.setEnabled(panel.isColorConvertParameterModified());
		}
	}
	
	/**
	 * このカラーダイアログが対応するパーツカテゴリを取得する.<br>
	 * 
	 * @return パーツカテゴリ
	 */
	public PartsCategory getPartsCategory() {
		return partsCategory;
	}
	
	/**
	 * 指定したレイヤーのカラーグループが「連動」しているか?<br>
	 * カテゴリに属していないレイヤーを指定した場合は常にfalseを返す.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @return 連動している場合はtrue、そうでなければfalse
	 */
	public boolean isSyncColorGroup(Layer layer) {
		ColorDialogTabPanel tab = tabs.get(layer);
		if (tab == null) {
			return false;
		}
		return tab.isSyncColorGroup();
	}

	/**
	 * 指定したレイヤーのカラーグループの連動フラグを設定する.<br>
	 * カテゴリに属していないレイヤーを指定した場合は何もしない.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @param selected
	 *            連動フラグ
	 */
	public void setSyncColorGroup(Layer layer, boolean selected) {
		ColorDialogTabPanel tab = tabs.get(layer);
		if (tab != null) {
			tab.setSyncColorGroup(selected);
		}
	}

	/**
	 * レイヤーごとの色情報のマップを指定して、各レイヤーに色情報を設定する.<br>
	 * カテゴリに属していないレイヤーが含まれる場合は例外となる.<br>
	 * 
	 * @param params
	 *            レイヤーと色情報のマップ
	 */
	public void setColorConvertParameters(Map<Layer, ColorConvertParameter> params) {
		if (params == null) {
			throw new IllegalArgumentException();
		}
		for (Layer layer : partsCategory.getLayers()) {
			ColorConvertParameter param = params.get(layer);
			if (param == null) {
				param = new ColorConvertParameter();
			}
			setColorConvertParameter(layer, param);
		}
	}
	
	/**
	 * 対象となるパーツ識別子を指定する。<br>
	 * カラーダイアログのキャプションにパーツ名を設定される.<br>
	 * nullを指定した場合はキャプションからパーツ名が除去される.<br>
	 * 
	 * @param partsIdentifier
	 *            パーツ識別子、もしくはnull
	 */
	public void setPartsIdentifier(PartsIdentifier partsIdentifier) {
		this.partsIdentifier = partsIdentifier;
		if (partsIdentifier == null) {
			setTitle(captionBase);
		} else {
			setTitle(captionBase + "(" + partsIdentifier.getLocalizedPartsName() + ")");
		}
	}

	/**
	 * 対象となるパーツ識別子を取得する.<br>
	 * 設定されていなければnullが返される.<br>
	 * 
	 * @return パーツ識別子、もしくはnull
	 */
	public PartsIdentifier getPartsIdentifier() {
		return partsIdentifier;
	}

	/**
	 * 各レイヤーのタブの有効・無効状態を設定します.<br>
	 * カテゴリに属さないレイヤーは無視されます.<br>
	 * nullを指定した場合は、すべてのレイヤーが「有効」となります.<br>
	 * 
	 * @param layers
	 *            有効とするレイヤーのコレクション、もしくはnull
	 */
	public void setEnableLayers(Collection<Layer> layers) {
		for (Map.Entry<Layer, ColorDialogTabPanel> entry : tabs.entrySet()) {
			Layer layer = entry.getKey();
			boolean enabled = (layers == null) || layers.contains(layer);
			Integer tabIndex = tabbedPaneIndexMap.get(layer);
			if (tabIndex != null) {
				if (Main.isMacOSX()) {
					// OSXの場合、タブをディセーブルにしても表示が変化ないので
					// タブタイトルを変更することでディセーブルを示す.
					// (html3で表現しようとしたところ、かなりバギーだったため採用せず)
					tabbedPane.setTitleAt(tabIndex,
							enabled ? layer.getLocalizedName() : "-");
				}

				tabbedPane.setEnabledAt(tabIndex, enabled);

				if (logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, "setEnableLayers(" + layer + ")=" + enabled);
				}
			}
		}
	}

	/**
	 * 「すべてに適用」フラグを取得する.<br>
	 * 
	 * @return すべてに適用フラグ
	 */
	public boolean isApplyAll() {
		return chkApplyAll.isSelected();
	}
	
	/**
	 * 各レイヤーと、その色情報をマップとして取得する.<br>
	 * 
	 * @return 各レイヤーと、その色情報のマップ
	 */
	public Map<Layer, ColorConvertParameter> getColorConvertParameters() {
		HashMap<Layer, ColorConvertParameter> params = new HashMap<Layer, ColorConvertParameter>();
		for (Layer layer : partsCategory.getLayers()) {
			ColorDialogTabPanel tab = tabs.get(layer);
			ColorConvertParameter param = tab.getColorConvertParameter();
			params.put(layer, param);
		}
		return params;
	}

	/**
	 * レイヤーを指定して、色情報を設定する.<br>
	 * カテゴリに属していないレイヤーを指定した場合は例外となる.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @param param
	 *            色情報
	 */
	public void setColorConvertParameter(Layer layer, ColorConvertParameter param) {
		if (layer == null || param == null) {
			throw new IllegalArgumentException();
		}
		ColorDialogTabPanel tab = tabs.get(layer);
		if (tab == null) {
			throw new IllegalArgumentException("layer not found. " + layer + "/tabs=" + tabs);
		}
		tab.setColorConvertParameter(param);
	}

	/**
	 * 指定したレイヤーの色情報を取得する.<br>
	 * カテゴリに属していないレイヤーを指定した場合は例外となる.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @return 色情報
	 */
	public ColorConvertParameter getColorConvertParameter(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException();
		}
		ColorDialogTabPanel tab = tabs.get(layer);
		if (tab == null) {
			throw new IllegalArgumentException("layer not found. " + layer);
		}
		return tab.getColorConvertParameter();
	}
	
	/**
	 * 指定したレイヤーのカラーグループを取得する.<br>
	 * カテゴリに属さないレイヤーを指定した場合は例外となる.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @return カラーグループ
	 */
	public ColorGroup getColorGroup(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException();
		}
		ColorDialogTabPanel tab = tabs.get(layer);
		if (tab == null) {
			throw new IllegalArgumentException("layer not found. " + layer);
		}
		return tab.getColorGroup();
	}
	
	/**
	 * 指定したレイヤーのカラーグループを設定する.<br>
	 * カテゴリに属さないレイヤーを指定した場合は例外となる.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @param colorGroup
	 *            カラーグループ
	 */
	public void setColorGroup(Layer layer, ColorGroup colorGroup) {
		if (layer == null || colorGroup == null) {
			throw new IllegalArgumentException();
		}
		ColorDialogTabPanel tab = tabs.get(layer);
		if (tab != null) {
			tab.setColorGroup(colorGroup);
		}
	}
	
	/**
	 * 色ダイアログが変更された場合に通知を受けるリスナーを登録する.<br>
	 * 
	 * @param listener
	 *            リスナー
	 */
	public void addColorChangeListener(ColorChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		listeners.add(listener);
	}

	/**
	 * 色ダイアログが変更された場合に通知を受けるリスナーを登録解除する.<br>
	 * 
	 * @param listener
	 */
	public void removeColorChangeListener(ColorChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * 全レイヤーに対するカラー変更イベントを明示的に送信する.
	 */
	protected void apply() {
		for (Layer layer : getPartsCategory().getLayers()) {
			ColorDialog.this.fireColorChangeEvent(layer, true);
		}
	}
	
	/**
	 * カラーをリセットする.
	 */
	protected void resetColor(ColorDialogTabPanel tab) {
		tab.resetColor();
	}

	/**
	 * 指定したレイヤーに対するカラー変更イベントを通知する.<br>
	 * ただし、force引数がfalseである場合、アプリケーション設定で即時プレビューが指定されていない場合は通知しない.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 * @param force
	 *            アプリケーション設定に関わらず送信する場合はtrue
	 */
	protected void fireColorChangeEvent(Layer layer, boolean force) {
		if (layer == null) {
			throw new IllegalArgumentException();
		}
		if (!force) {
			AppConfig appConfig = AppConfig.getInstance();
			if (!appConfig.isEnableAutoColorChange()) {
				return;
			}
		}
		ColorChangeEvent event = new ColorChangeEvent(this, layer);
		for (ColorChangeListener listener : listeners) {
			listener.onColorChange(event);
		}
	}

	/**
	 * 色グループが変更されたことを通知する.<br>
	 * 
	 * @param layer
	 *            レイヤー
	 */
	protected void fireColorGroupChangeEvent(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException();
		}
		ColorChangeEvent event = new ColorChangeEvent(this, layer);
		for (ColorChangeListener listener : listeners) {
			listener.onColorGroupChange(event);
		}
	}
	
	@Override
	public String toString() {
		return "ColorDialog(partsCategory:" + partsCategory + ")";
	}
}


class ColorDialogTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JComboBox cmbColorReplace;
	
	private JSpinner spinGray;
	
	private JSpinner spinOffsetR;
	
	private JSpinner spinOffsetG;
	
	private JSpinner spinOffsetB;
	
	private JSpinner spinOffsetA;

	private JSpinner spinFactorR;
	
	private JSpinner spinFactorG;
	
	private JSpinner spinFactorB;
	
	private JSpinner spinFactorA;
	
	private JSpinner spinHue;
	
	private JSpinner spinSaturation;
	
	private JSpinner spinBrightness;
	
	private JSpinner spinContrast;

	private JSpinner spinGammaR;

	private JSpinner spinGammaG;
	
	private JSpinner spinGammaB;
	
	private JSpinner spinGammaA;
	
	private JComboBox cmbColorGroup;
	
	private JCheckBox chkColorGroupSync;

	private final ColorDialog parent;
	
	/**
	 * パラメータの明示的変更時に他のパラメータへの反映イベントを停止させるためのセマフォ
	 */
	private AtomicInteger changeEventDisableSemaphore = new AtomicInteger();
	
	/**
	 * 明示的に設定されたカラーパラメータを保存する.(リセットに使用するため)
	 */
	private ColorConvertParameter paramOrg = new ColorConvertParameter();
	
	private ColorConvertParameter chachedParam;
	
	private LinkedList<ColorChangeListener> listeners = new LinkedList<ColorChangeListener>();
	
	public void addColorChangeListener(ColorChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		listeners.add(listener);
	}
	
	public void removeColorChangeListener(ColorChangeListener listener) {
		listeners.remove(listener);
	}
	
	protected void fireColorChangeEvent(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException();
		}
		chachedParam = null;
		if (changeEventDisableSemaphore.get() <= 0) {
			ColorChangeEvent event = new ColorChangeEvent(parent, layer);
			for (ColorChangeListener listener : listeners) {
				listener.onColorChange(event);
			}
		}
	}
	
	protected void fireColorGroupChangeEvent(Layer layer) {
		if (layer == null) {
			throw new IllegalArgumentException();
		}
		chachedParam = null;
		if (changeEventDisableSemaphore.get() <= 0) {
			ColorChangeEvent event = new ColorChangeEvent(parent, layer);
			for (ColorChangeListener listener : listeners) {
				listener.onColorGroupChange(event);
			}
		}
	}

	public ColorDialogTabPanel(final ColorDialog parent, final Layer layer, Collection<ColorGroup> colorGroups) {
		if (parent == null || layer == null || colorGroups == null) {
			throw new IllegalArgumentException();
		}
		this.parent = parent;

		final Properties strings = LocalizedResourcePropertyLoader.getCachedInstance().getLocalizedProperties("languages/colordialog");

		setLayout(new BorderLayout());
		JPanel container = new JPanel();
		BoxLayout boxlayout = new BoxLayout(container, BoxLayout.PAGE_AXIS);
		container.setLayout(boxlayout);
		add(container, BorderLayout.NORTH);
		
		// 変更イベントハンドラ
		final ChangeListener changeEventHandler = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				fireColorChangeEvent(layer);
				firePropertyChange("colorConvertParameter", null, null);
			}
		};
		
		// 色置換パネル
		
		JPanel colorReplacePanel = new JPanel();
		colorReplacePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3),
				BorderFactory.createTitledBorder(strings.getProperty("group.replacergb.caption"))));
		GridBagLayout gbl = new GridBagLayout();
		colorReplacePanel.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel lblColorReplace = new JLabel(strings.getProperty("replacergb"), JLabel.RIGHT);
		cmbColorReplace = new JComboBox(ColorConv.values());
		JLabel lblGray = new JLabel(strings.getProperty("bright"), JLabel.RIGHT);
		SpinnerNumberModel grayModel = new SpinnerNumberModel(1., 0., 1., 0.01);
		grayModel.addChangeListener(changeEventHandler);
		cmbColorReplace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireColorChangeEvent(layer);
				firePropertyChange("colorConvertParameter", null, null);
			}
		});
		spinGray = new JSpinner(grayModel);
		spinGray.addMouseWheelListener(new SpinnerWheelSupportListener(grayModel));
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.insets = new Insets(3, 3, 3, 3);
		colorReplacePanel.add(lblColorReplace, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		colorReplacePanel.add(cmbColorReplace, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		colorReplacePanel.add(lblGray, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.weightx = 1.;
		gbc.weighty = 0.;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		colorReplacePanel.add(spinGray, gbc);

		// RGB変更パネル
		
		JPanel colorLevelPanel = new JPanel();
		colorLevelPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3),
				BorderFactory.createTitledBorder(strings.getProperty("group.rgb.caption"))));
		GridLayout gl = new GridLayout(4, 5);
		gl.setHgap(2);
		gl.setVgap(2);
		colorLevelPanel.setLayout(gl);
		colorLevelPanel.add(Box.createGlue());
		colorLevelPanel.add(new JLabel(strings.getProperty("red"), JLabel.CENTER));
		colorLevelPanel.add(new JLabel(strings.getProperty("green"), JLabel.CENTER));
		colorLevelPanel.add(new JLabel(strings.getProperty("blue"), JLabel.CENTER));
		colorLevelPanel.add(new JLabel(strings.getProperty("alpha"), JLabel.CENTER));
		colorLevelPanel.add(new JLabel(strings.getProperty("offset"), JLabel.RIGHT));
		SpinnerNumberModel offsetModelR = new SpinnerNumberModel(0, -255, 255, 1);
		SpinnerNumberModel offsetModelG = new SpinnerNumberModel(0, -255, 255, 1);
		SpinnerNumberModel offsetModelB = new SpinnerNumberModel(0, -255, 255, 1);
		SpinnerNumberModel offsetModelA = new SpinnerNumberModel(0, -255, 255, 1);
		offsetModelR.addChangeListener(changeEventHandler);
		offsetModelG.addChangeListener(changeEventHandler);
		offsetModelB.addChangeListener(changeEventHandler);
		offsetModelA.addChangeListener(changeEventHandler);
		spinOffsetR = new JSpinner(offsetModelR);
		spinOffsetG = new JSpinner(offsetModelG);
		spinOffsetB = new JSpinner(offsetModelB);
		spinOffsetA = new JSpinner(offsetModelA);
		spinOffsetR.addMouseWheelListener(new SpinnerWheelSupportListener(offsetModelR));
		spinOffsetG.addMouseWheelListener(new SpinnerWheelSupportListener(offsetModelG));
		spinOffsetB.addMouseWheelListener(new SpinnerWheelSupportListener(offsetModelB));
		spinOffsetA.addMouseWheelListener(new SpinnerWheelSupportListener(offsetModelA));
		colorLevelPanel.add(spinOffsetR);
		colorLevelPanel.add(spinOffsetG);
		colorLevelPanel.add(spinOffsetB);
		colorLevelPanel.add(spinOffsetA);
		colorLevelPanel.add(new JLabel(strings.getProperty("factor"), JLabel.RIGHT));
		SpinnerNumberModel factorModelR = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		SpinnerNumberModel factorModelG = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		SpinnerNumberModel factorModelB = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		SpinnerNumberModel factorModelA = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		factorModelR.addChangeListener(changeEventHandler);
		factorModelG.addChangeListener(changeEventHandler);
		factorModelB.addChangeListener(changeEventHandler);
		factorModelA.addChangeListener(changeEventHandler);
		spinFactorR = new JSpinner(factorModelR);
		spinFactorG = new JSpinner(factorModelG);
		spinFactorB = new JSpinner(factorModelB);
		spinFactorA = new JSpinner(factorModelA);
		spinFactorR.addMouseWheelListener(new SpinnerWheelSupportListener(factorModelR));
		spinFactorG.addMouseWheelListener(new SpinnerWheelSupportListener(factorModelG));
		spinFactorB.addMouseWheelListener(new SpinnerWheelSupportListener(factorModelB));
		spinFactorA.addMouseWheelListener(new SpinnerWheelSupportListener(factorModelA));
		colorLevelPanel.add(spinFactorR);
		colorLevelPanel.add(spinFactorG);
		colorLevelPanel.add(spinFactorB);
		colorLevelPanel.add(spinFactorA);
		colorLevelPanel.add(new JLabel(strings.getProperty("gamma"), JLabel.RIGHT));
		SpinnerNumberModel gammaModelR = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		SpinnerNumberModel gammaModelG = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		SpinnerNumberModel gammaModelB = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		SpinnerNumberModel gammaModelA = new SpinnerNumberModel(1., 0.01, 99, 0.01);
		gammaModelR.addChangeListener(changeEventHandler);
		gammaModelG.addChangeListener(changeEventHandler);
		gammaModelB.addChangeListener(changeEventHandler);
		gammaModelA.addChangeListener(changeEventHandler);
		spinGammaR = new JSpinner(gammaModelR);
		spinGammaG = new JSpinner(gammaModelG);
		spinGammaB = new JSpinner(gammaModelB);
		spinGammaA = new JSpinner(gammaModelA);
		spinGammaR.addMouseWheelListener(new SpinnerWheelSupportListener(gammaModelR));
		spinGammaG.addMouseWheelListener(new SpinnerWheelSupportListener(gammaModelG));
		spinGammaB.addMouseWheelListener(new SpinnerWheelSupportListener(gammaModelB));
		spinGammaA.addMouseWheelListener(new SpinnerWheelSupportListener(gammaModelA));
		colorLevelPanel.add(spinGammaR);
		colorLevelPanel.add(spinGammaG);
		colorLevelPanel.add(spinGammaB);
		colorLevelPanel.add(spinGammaA);

		// 色調パネル

		ColorModel colorModel = ColorModels.safeValueOf(layer
				.getColorModelName());

		JPanel colorTunePanel = new JPanel();
		colorTunePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3),
				BorderFactory.createTitledBorder(strings.getProperty(colorModel.getTitle()))));
		GridLayout gl2 = new GridLayout(3, 4);
		gl2.setHgap(3);
		gl2.setVgap(3);
		colorTunePanel.setLayout(gl2);

		colorTunePanel.add(new JLabel(strings.getProperty(colorModel
				.getItemTitle(0)), JLabel.CENTER)); // Hue 色相
		colorTunePanel.add(new JLabel(strings.getProperty(colorModel
				.getItemTitle(1)), JLabel.CENTER)); // Saturation 彩度
		colorTunePanel.add(new JLabel(strings.getProperty(colorModel
				.getItemTitle(2)), JLabel.CENTER)); // Brightness 明度
		colorTunePanel.add(new JLabel(strings.getProperty("contrast"),
				JLabel.CENTER)); // Contrast コントラスト

		SpinnerNumberModel hsbModelH = new SpinnerNumberModel(0., -1., 1., 0.001);
		SpinnerNumberModel hsbModelS = new SpinnerNumberModel(0., -1., 1., 0.001);
		SpinnerNumberModel hsbModelB = new SpinnerNumberModel(0., -1., 1., 0.001);
		SpinnerNumberModel hsbModelC = new SpinnerNumberModel(0., -1., 1., 0.001);
		
		hsbModelH.addChangeListener(changeEventHandler);
		hsbModelS.addChangeListener(changeEventHandler);
		hsbModelB.addChangeListener(changeEventHandler);
		hsbModelC.addChangeListener(changeEventHandler);
		
		spinHue = new JSpinner(hsbModelH);
		spinSaturation = new JSpinner(hsbModelS);
		spinBrightness = new JSpinner(hsbModelB);
		spinContrast = new JSpinner(hsbModelC);
		spinHue.addMouseWheelListener(new SpinnerWheelSupportListener(hsbModelH));
		spinSaturation.addMouseWheelListener(new SpinnerWheelSupportListener(hsbModelS));
		spinBrightness.addMouseWheelListener(new SpinnerWheelSupportListener(hsbModelB));
		spinContrast.addMouseWheelListener(new SpinnerWheelSupportListener(hsbModelC));
		
		colorTunePanel.add(spinHue);
		colorTunePanel.add(spinSaturation);
		colorTunePanel.add(spinBrightness);
		colorTunePanel.add(spinContrast);
		
		JSlider sliderHue = new JSlider();
		JSlider sliderSaturation = new JSlider();
		JSlider sliderBrightness = new JSlider();
		JSlider sliderContrast = new JSlider();
		
		sliderHue.setPreferredSize(spinHue.getPreferredSize());
		sliderSaturation.setPreferredSize(spinSaturation.getPreferredSize());
		sliderBrightness.setPreferredSize(spinBrightness.getPreferredSize());
		sliderContrast.setPreferredSize(spinContrast.getPreferredSize());

		colorTunePanel.add(sliderHue);
		colorTunePanel.add(sliderSaturation);
		colorTunePanel.add(sliderBrightness);
		colorTunePanel.add(sliderContrast);

		JSlider sliders[] = new JSlider[] {sliderHue, sliderSaturation, sliderBrightness, sliderContrast};
		JSpinner spinners[] = new JSpinner[] {spinHue, spinSaturation, spinBrightness, spinContrast};
		
		for (int idx = 0; idx < spinners.length; idx++) {
			final JSlider sl = sliders[idx];
			final JSpinner sp = spinners[idx];
			SpinnerNumberModel spModel = (SpinnerNumberModel) sp.getModel();
			sl.setMinimum((int)(((Number)spModel.getMinimum()).floatValue() * 100));
			sl.setMaximum((int)(((Number)spModel.getMaximum()).floatValue() * 100));
			sl.setValue((int)(((Number) sp.getValue()).doubleValue() * 100.));
			final Semaphore loopBlocker = new Semaphore(1); // イベントが循環発生することを防ぐ
			sl.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (loopBlocker.tryAcquire()) {
						try {
							double rate = sl.getValue() / 100.;
							sp.setValue(Double.valueOf(rate));
						} finally {
							loopBlocker.release();
						}
					}
				}
			});
			sp.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (loopBlocker.tryAcquire()) {
						try {
							int rate = (int)(((Number) sp.getValue()).doubleValue() * 100.);
							sl.setValue(rate);
						} finally {
							loopBlocker.release();
						}
					}
				}
			});
		}
		
		// カラーグループ
		ColorGroup colorGroup = layer.getColorGroup();
		JPanel colorGroupPanel = new JPanel();
		colorGroupPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3),
				BorderFactory.createTitledBorder(strings.getProperty("colorgroup"))));
		GridBagLayout gbl2 = new GridBagLayout();
		colorGroupPanel.setLayout(gbl2);
		GridBagConstraints gbc2 = new GridBagConstraints();

		JLabel lblColorGroup = new JLabel(strings.getProperty("group"), JLabel.RIGHT);
		cmbColorGroup = new JComboBox(colorGroups.toArray(new ColorGroup[colorGroups.size()]));
		cmbColorGroup.setSelectedItem(colorGroup);
		cmbColorGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ColorGroup selColorGroup = (ColorGroup) cmbColorGroup.getSelectedItem();
				chkColorGroupSync.setSelected(selColorGroup.isEnabled());
				fireColorGroupChangeEvent(layer);
			}
		});
		chkColorGroupSync = new JCheckBox(strings.getProperty("synchronized"));
		chkColorGroupSync.setSelected(layer.isInitSync());

		gbc2.gridx = 0;
		gbc2.gridy = 0;
		gbc2.gridwidth = 1;
		gbc2.gridheight = 1;
		gbc2.weightx = 0.;
		gbc2.weighty = 0.;
		gbc2.anchor = GridBagConstraints.WEST;
		gbc2.fill = GridBagConstraints.BOTH;
		gbc2.ipadx = 0;
		gbc2.ipady = 0;
		gbc2.insets = new Insets(3, 3, 3, 3);
		colorGroupPanel.add(lblColorGroup, gbc2);
		
		gbc2.gridx = 1;
		gbc2.gridy = 0;
		gbc2.gridwidth = 1;
		gbc2.gridheight = 1;
		gbc2.weightx = 1.;
		gbc2.weighty = 0.;
		colorGroupPanel.add(cmbColorGroup, gbc2);
		
		gbc2.gridx = 2;
		gbc2.gridy = 0;
		gbc2.gridwidth = GridBagConstraints.REMAINDER;
		gbc2.gridheight = 1;
		gbc2.weightx = 0.;
		gbc2.weighty = 0.;
		colorGroupPanel.add(chkColorGroupSync, gbc2);
		
		if (colorGroupPanel != null) {
			container.add(colorGroupPanel);
		}
		container.add(colorLevelPanel);
		container.add(colorReplacePanel);
		container.add(colorTunePanel);
	}
	
	/**
	 * このパネルで変更された色情報の状態をリセットする.<br>
	 * 最後に{@link #setColorConvertParameter(ColorConvertParameter)}された値で 設定し直す.<br>
	 */
	public void resetColor() {
		setColorConvertParameter(paramOrg);
	}
	
	public void setColorConvertParameter(ColorConvertParameter param) {
		if (param == null) {
			throw new IllegalArgumentException();
		}
		paramOrg = param.clone();
		ColorConv colorReplace = param.getColorReplace();
		if (colorReplace == null) {
			colorReplace = ColorConv.NONE;
		}
		changeEventDisableSemaphore.incrementAndGet();
		try {
			cmbColorReplace.setSelectedItem(colorReplace);
			spinGray.setValue(Double.valueOf(param.getGrayLevel()));
			spinOffsetR.setValue(Integer.valueOf(param.getOffsetR()));
			spinOffsetG.setValue(Integer.valueOf(param.getOffsetG()));
			spinOffsetB.setValue(Integer.valueOf(param.getOffsetB()));
			spinOffsetA.setValue(Integer.valueOf(param.getOffsetA()));
			spinFactorR.setValue(Double.valueOf(param.getFactorR()));
			spinFactorG.setValue(Double.valueOf(param.getFactorG()));
			spinFactorB.setValue(Double.valueOf(param.getFactorB()));
			spinFactorA.setValue(Double.valueOf(param.getFactorA()));
			spinGammaR.setValue(Double.valueOf(param.getGammaR()));
			spinGammaG.setValue(Double.valueOf(param.getGammaG()));
			spinGammaB.setValue(Double.valueOf(param.getGammaB()));
			spinGammaA.setValue(Double.valueOf(param.getGammaA()));
			spinHue.setValue(Double.valueOf(param.getHue()));
			spinSaturation.setValue(Double.valueOf(param.getSaturation()));
			spinBrightness.setValue(Double.valueOf(param.getBrightness()));
			spinContrast.setValue(Double.valueOf(param.getContrast()));

		} finally {
			changeEventDisableSemaphore.decrementAndGet();
		}
		
		chachedParam = param;
		
		firePropertyChange("colorConvertParameter", null, param);
	}
	
	public ColorConvertParameter getColorConvertParameter() {
		if (chachedParam != null) {
			return chachedParam;
		}
		ColorConvertParameter param = new ColorConvertParameter();
		param.setColorReplace((ColorConv) cmbColorReplace.getSelectedItem());
		param.setGrayLevel(((Number) spinGray.getValue()).floatValue());
		param.setOffsetR(((Number) spinOffsetR.getValue()).intValue());
		param.setOffsetG(((Number) spinOffsetG.getValue()).intValue());
		param.setOffsetB(((Number) spinOffsetB.getValue()).intValue());
		param.setOffsetA(((Number) spinOffsetA.getValue()).intValue());
		param.setFactorR(((Number) spinFactorR.getValue()).floatValue());
		param.setFactorG(((Number) spinFactorG.getValue()).floatValue());
		param.setFactorB(((Number) spinFactorB.getValue()).floatValue());
		param.setFactorA(((Number) spinFactorA.getValue()).floatValue());
		param.setGammaR(((Number) spinGammaR.getValue()).floatValue());
		param.setGammaG(((Number) spinGammaG.getValue()).floatValue());
		param.setGammaB(((Number) spinGammaB.getValue()).floatValue());
		param.setGammaA(((Number) spinGammaA.getValue()).floatValue());
		param.setHue(((Number) spinHue.getValue()).floatValue());
		param.setSaturation(((Number) spinSaturation.getValue()).floatValue());
		param.setBrightness(((Number) spinBrightness.getValue()).floatValue());
		param.setContrast(((Number) spinContrast.getValue()).floatValue());
		chachedParam = param;
		return param;
	}
	
	/**
	 * カラー設定が変更されているか?
	 * 
	 * @return 変更されている場合はtrue、そうでなければfalse
	 */
	public boolean isColorConvertParameterModified() {
		return !paramOrg.equals(getColorConvertParameter());
	}
	
	public ColorGroup getColorGroup() {
		return (ColorGroup) cmbColorGroup.getSelectedItem();
	}
	
	public void setColorGroup(ColorGroup colorGroup) {
		if (colorGroup == null) {
			colorGroup = ColorGroup.NA;
		}
		changeEventDisableSemaphore.incrementAndGet();
		try {
			cmbColorGroup.setSelectedItem(colorGroup);
		} finally {
			changeEventDisableSemaphore.decrementAndGet();
		}
	}
	
	public boolean isSyncColorGroup() {
		return chkColorGroupSync == null ? false : chkColorGroupSync.isSelected();
	}
	
	public void setSyncColorGroup(boolean selected) {
		if (chkColorGroupSync != null) {
			changeEventDisableSemaphore.incrementAndGet();
			try {
				chkColorGroupSync.setSelected(selected);
			} finally {
				changeEventDisableSemaphore.decrementAndGet();
			}
		}
	}
}
