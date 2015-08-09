package charactermanaj.ui;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import charactermanaj.Main;
import charactermanaj.graphics.filters.BackgroundColorFilter;
import charactermanaj.graphics.filters.BackgroundColorFilter.BackgroundColorMode;
import charactermanaj.model.AppConfig;
import charactermanaj.ui.util.ScrollPaneDragScrollSupport;
import charactermanaj.util.LocalizedResourcePropertyLoader;
import charactermanaj.util.UIHelper;

/**
 * プレビューパネル
 * 
 * @author seraphy
 */
public class PreviewPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected static final String STRINGS_RESOURCE = "languages/previewpanel";


	/**
	 * プレビューパネルの上部ツールバーの通知を受けるリスナ
	 * 
	 * @author seraphy
	 */
	public interface PreviewPanelListener {

		/**
		 * 保存
		 * 
		 * @param e
		 */
		void savePicture(PreviewPanelEvent e);
		
		/**
		 * コピー
		 * 
		 * @param e
		 */
		void copyPicture(PreviewPanelEvent e);

		/**
		 * 背景色変更
		 * 
		 * @param e
		 */
		void changeBackgroundColor(PreviewPanelEvent e);

		/**
		 * 情報
		 * 
		 * @param e
		 */
		void showInformation(PreviewPanelEvent e);

		/**
		 * お気に入りに追加
		 * 
		 * @param e
		 */
		void addFavorite(PreviewPanelEvent e);
		
		/**
		 * 左右反転
		 * 
		 * @param e
		 */
		void flipHorizontal(PreviewPanelEvent e);
	}

	/**
	 * ロード中を示すインジケータ
	 */
	private final String indicatorText;
	
	/**
	 * ロード中であるか判定するタイマー
	 */
	private final Timer timer;
	
	/**
	 * インジケータを表示するまでのディレイ
	 */
	private long indicatorDelay;
	
	@Override
	public void addNotify() {
		super.addNotify();
		if (!timer.isRunning()) {
			timer.start();
		}
	}
	
	@Override
	public void removeNotify() {
		if (timer.isRunning()) {
			timer.stop();
		}
		super.removeNotify();
	}
	
	public static class PreviewPanelEvent extends EventObject {
		
		private static final long serialVersionUID = 1L;
		
		private int modifiers;

		public PreviewPanelEvent(Object src, ActionEvent e) {
			this(src, (e == null) ? 0 : e.getModifiers());
		}

		public PreviewPanelEvent(Object src, int modifiers) {
			super(src);
			this.modifiers = modifiers;
		}
		
		public int getModifiers() {
			return modifiers;
		}
		
		public boolean isShiftKeyPressed() {
			return (modifiers & ActionEvent.SHIFT_MASK) != 0;
		}
	}
	
	private final Object lock = new Object();
	
	private long loadingTicket;
	
	private long loadedTicket;
	
	private long firstWaitingTimestamp;
	
	private boolean indicatorShown;
	
	private String title;
	
	private JLabel lblTitle;
	
	private JLayeredPane layeredPane;
	
	private CheckInfoLayerPanel checkInfoLayerPanel;
	
	private PreviewImagePanel previewImgPanel;
	
	private JScrollPane previewImgScrollPane;
	
	private ScrollPaneDragScrollSupport scrollSupport;
	
	private PreviewControlPanel previewControlPanel;
	
	private double latestToggleZoom = 2.;
	
	private LinkedList<PreviewPanelListener> listeners = new LinkedList<PreviewPanelListener>();

	
	public PreviewPanel() {
		setLayout(new BorderLayout());

		final AppConfig appConfig = AppConfig.getInstance();
		final Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(STRINGS_RESOURCE);
		
		// 画像をロード中であることを示すインジケータの確認サイクル.
		timer = new Timer(100, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onTimer();
				}
			});
		 
		indicatorText = strings.getProperty("indicatorText");
		indicatorDelay = appConfig.getPreviewIndicatorDelay();

		UIHelper uiUtl = UIHelper.getInstance();
		JButton saveBtn = uiUtl.createIconButton("icons/save.png");
		JButton copyBtn = uiUtl.createIconButton("icons/copy.png");
		JButton colorBtn = uiUtl.createIconButton("icons/color.png");
		JButton informationBtn = uiUtl.createIconButton("icons/information.png");
		JButton favoriteBtn = uiUtl.createIconButton("icons/favorite.png");
		JButton flipHolizontalBtn = uiUtl.createIconButton("icons/flip.png");

		saveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				savePicture(new PreviewPanelEvent(PreviewPanel.this, e));
			}
		});
		copyBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				copyPicture(new PreviewPanelEvent(PreviewPanel.this, e));
			}
		});
		colorBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeBackgroundColor(new PreviewPanelEvent(PreviewPanel.this, e));
			}
		});
		informationBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showInformation(new PreviewPanelEvent(PreviewPanel.this, e));
			}
		});
		favoriteBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addFavorite(new PreviewPanelEvent(PreviewPanel.this, e));
			}
		});
		flipHolizontalBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				flipHolizontal(new PreviewPanelEvent(PreviewPanel.this, e));
			}
		});
		
		saveBtn.setToolTipText(strings.getProperty("tooltip.save"));
		copyBtn.setToolTipText(strings.getProperty("tooltip.copy"));
		colorBtn.setToolTipText(strings.getProperty("tooltip.changeBgColor"));
		informationBtn.setToolTipText(strings.getProperty("tooltip.showInformation"));
		favoriteBtn.setToolTipText(strings.getProperty("tooltip.registerFavorites"));
		flipHolizontalBtn.setToolTipText(strings.getProperty("tooltip.flipHorizontal"));

		final JToolBar toolBar = new JToolBar(); 
		toolBar.setFloatable(false);
		toolBar.add(flipHolizontalBtn);
		toolBar.add(copyBtn);
		toolBar.add(saveBtn);
		toolBar.add(Box.createHorizontalStrut(8));
		toolBar.add(colorBtn);
		toolBar.add(Box.createHorizontalStrut(4));
		toolBar.add(favoriteBtn);
		toolBar.add(informationBtn);

		lblTitle = new JLabel() {
			private static final long serialVersionUID = 1L;

			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				int maxWidth = getParent().getWidth() - toolBar.getWidth();
				if (dim.width > maxWidth) {
					dim.width = maxWidth;
				}
				return dim;
			};
			
			public Dimension getMaximumSize() {
				return getPreferredSize();
			};
			
			public Dimension getMinimumSize() {
				Dimension dim = getPreferredSize();
				dim.width = 50; 
				return dim;
			};
		};

		lblTitle.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 3));

		JPanel previewPaneHeader = new JPanel();
		previewPaneHeader.setLayout(new BorderLayout());
		previewPaneHeader.add(lblTitle, BorderLayout.WEST);
		previewPaneHeader.add(toolBar, BorderLayout.EAST);

		previewImgPanel = new PreviewImagePanel();

		
		previewImgScrollPane = new JScrollPane(previewImgPanel);
		previewImgScrollPane.setAutoscrolls(false);
		previewImgScrollPane.setWheelScrollingEnabled(false);
		previewImgScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		previewImgScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		scrollSupport = new ScrollPaneDragScrollSupport(previewImgScrollPane) {
			@Override
			protected void setCursor(Cursor cursor) {
				PreviewPanel.this.setCursor(cursor);
			}
		};
		
		add(previewPaneHeader, BorderLayout.NORTH);
		
		layeredPane = new JLayeredPane();
		layeredPane.setLayout(new OverlayLayout(layeredPane));

		layeredPane.add(previewImgScrollPane, JLayeredPane.DEFAULT_LAYER);
		
		checkInfoLayerPanel = new CheckInfoLayerPanel();
		layeredPane.add(checkInfoLayerPanel, JLayeredPane.POPUP_LAYER);
		checkInfoLayerPanel.setVisible(false);
		
		add(layeredPane, BorderLayout.CENTER);

		previewControlPanel = new PreviewControlPanel();
		Dimension dim = previewControlPanel.getPreferredSize();
		Dimension prevDim = previewImgScrollPane.getPreferredSize();
		dim.width = prevDim.width;
		previewControlPanel.setPreferredSize(dim);
		
		add(previewControlPanel, BorderLayout.SOUTH);
		previewControlPanel.setPinned(appConfig.isEnableZoomPanel());

		// 倍率が変更された場合
		previewControlPanel.addPropertyChangeListener("zoomFactorInt", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Integer newValue = (Integer) evt.getNewValue();
				zoomWithCenterPosition(newValue.doubleValue() / 100., null);
			}
		});
		// 背景モードが切り替えられた場合
		previewControlPanel.addPropertyChangeListener("backgroundColorMode", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				BackgroundColorMode bgColorMode = (BackgroundColorMode) evt.getNewValue();
				previewImgPanel.setBackgroundColorMode(bgColorMode);
				if (bgColorMode != BackgroundColorMode.ALPHABREND
						&& appConfig.isEnableCheckInfoTooltip() ) {
							// チェック情報ツールチップの表示
					checkInfoLayerPanel.setMessage(null);
					checkInfoLayerPanel.setVisible(true);

				} else {
							// チェック情報ツールチップの非表示
					checkInfoLayerPanel.setVisible(false);
				}
			}
		});
		
		previewImgScrollPane.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Rectangle rct = previewImgScrollPane.getBounds();
				int y = e.getY();
				if (y > rct.height - appConfig.getZoomPanelActivationArea()) {
					previewControlPanel.setVisible(true);
 				} else {
 					if ( !previewControlPanel.isPinned()) {
 						previewControlPanel.setVisible(false);
 					}
 				}
			}
		});

		// 標準のホイールリスナは削除する.
		for (final MouseWheelListener listener : previewImgScrollPane.getMouseWheelListeners()) {
			previewImgScrollPane.removeMouseWheelListener(listener);
		}
		
		previewImgScrollPane.addMouseWheelListener(new MouseWheelListener() { 
			public void mouseWheelMoved(MouseWheelEvent e) {
				if ((Main.isMacOSX() && e.isAltDown()) ||
						( !Main.isMacOSX() && e.isControlDown())) {
					// Mac OS XならOptionキー、それ以外はコントロールキーとともにホイールスクロールの場合
					zoomByWheel(e);
				} else {
					// ズーム以外のホイール操作はスクロールとする.
					scrollByWheel(e);
				}
				// 現在画像位置の情報の更新
				updateCheckInfoMessage(e.getPoint());
			}
		});
		
		previewImgScrollPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// ダブルクリック
					// (正確に2回目。3回目以降はダブルクリック + シングルクリック)
					toggleZoom(e.getPoint());
				} else {
					scrollSupport.drag(true, e.getPoint());
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				scrollSupport.drag(false, e.getPoint());
			}
		});
		
		previewImgScrollPane.addMouseMotionListener(new MouseMotionListener() {
			
			public void mouseMoved(MouseEvent e) {
				updateCheckInfoMessage(e.getPoint());
			}
			
			public void mouseDragged(MouseEvent e) {
				scrollSupport.dragging(e.getPoint());

				// 現在画像位置の情報の更新
				updateCheckInfoMessage(e.getPoint());
			}
		});
	}
	
	/**
	 * 倍率を切り替える.
	 */
	protected void toggleZoom(Point mousePos) {
		if (previewImgPanel.isDefaultZoom()) {
			// 等倍であれば以前の倍率を適用する.
			zoomWithCenterPosition(latestToggleZoom, mousePos);

		} else {
			// 等倍でなければ現在の倍率を記憶して等倍にする.
			double currentZoomFactor = previewImgPanel.getZoomFactor();
			latestToggleZoom = currentZoomFactor;
			zoomWithCenterPosition(1., mousePos);
		}
	}

	/**
	 * マウス位置に対して画像情報のツールチップを表示する
	 * 
	 * @param mousePosition
	 *            マウス位置
	 */
	protected void updateCheckInfoMessage(Point mousePosition) {
		if ( !checkInfoLayerPanel.isVisible()) {
			return;
		}
		// マウス位置から画像位置を割り出す
		Point imgPos = null;
		if (mousePosition != null) {
			Point panelPt = SwingUtilities.convertPoint(previewImgScrollPane,
					mousePosition, previewImgPanel);
			imgPos = previewImgPanel.getImagePosition(panelPt);
		}
		if (imgPos != null) {
			// 画像位置があれば、その位置の情報を取得する.
			int argb = previewImgPanel.getImageARGB(imgPos);
			int a = (argb >> 24) & 0xff;
			int r = (argb >> 16) & 0xff;
			int g = (argb >> 8) & 0xff;
			int b = argb & 0xff;
			int y = (int) (0.298912f * r + 0.586611f * g + 0.114478f * b);
			String text = String.format(
					"(%3d,%3d)¥nA:%3d, Y:%3d¥nR:%3d, G:%3d, B:%3d", imgPos.x,
					imgPos.y, a, y, r, g, b);
			checkInfoLayerPanel.setMessage(text);
			checkInfoLayerPanel.setPotision(mousePosition);

		} else {
			// 画像位置がなければツールチップは空にする.
			checkInfoLayerPanel.setMessage(null);
		}
	}
	
	/**
	 * マウス座標単位で指定したオフセット分スクロールする.
	 * 
	 * @param diff_x
	 *            水平方向スクロール数
	 * @param diff_y
	 *            垂直方向スクロール数
	 */
	protected void scroll(int diff_x, int diff_y) {
		scrollSupport.scroll(diff_x, diff_y);
	}
	
	/**
	 * マウスホイールによる水平・垂直スクロール.<br>
	 * シフトキーで水平、それ以外は垂直とする.<br>
	 * 
	 * @param e
	 *            ホイールイベント
	 */
	protected void scrollByWheel(final MouseWheelEvent e) {
		scrollSupport.scrollByWheel(e);

		// イベントは処理済みとする.
		e.consume();
	}

	/**
	 * ホイールによる拡大縮小.<br>
	 * ホイールの量は関係なく、方向だけで判定する.<br>
	 * プラットフォームごとに修飾キーの判定が異なるので、 呼び出しもとであらかじめ切り分けて呼び出すこと.<br>
	 * 
	 * @param e
	 *            ホイールイベント
	 */
	protected void zoomByWheel(final MouseWheelEvent e) {
		int wheelRotation = e.getWheelRotation();
		double currentZoom = previewImgPanel.getZoomFactor();
		double zoomFactor;
		if (wheelRotation < 0) {
			// ホイール上で拡大
			zoomFactor = currentZoom * 1.1;

		} else if (wheelRotation > 0){
			// ホイール下で縮小
			zoomFactor = currentZoom * 0.9;
		
		} else {
			return;
		}
		
		// 倍率変更する
		zoomWithCenterPosition(zoomFactor, e.getPoint());
		
		// イベント処理済み
		e.consume();
	}
	
	/**
	 * ズームスライダまたはコンボのいずれかの値を更新すると、他方からも更新通知があがるため 二重処理を防ぐためのセマフォ.<br>
	 */
	private Semaphore zoomLock = new Semaphore(1);
	
	/**
	 * プレビューに表示する画像の倍率を更新する.<br>
	 * 指定した座標が拡大縮小の中心点になるようにスクロールを試みる.<br>
	 * 座標がnullの場合は現在表示されている中央を中心とするようにスクロールを試みる.<br>
	 * (スクロールバーが表示されていない、もしくは十分にスクロールできない場合は必ずしも中心とはならない.)<br>
	 * コントロールパネルの表示値も更新する.<br>
	 * コントロールパネルからの更新通知をうけて再入しないように、 同時に一つしか実行されないようにしている.<br>
	 * 
	 * @param zoomFactor
	 *            倍率、範囲外のものは範囲内に補正される.
	 * @param mousePos
	 *            スクロールペイン上のマウス座標、もしくはnull(nullの場合は表示中央)
	 */
	protected void zoomWithCenterPosition(double zoomFactor, Point mousePos) {
		if ( !zoomLock.tryAcquire()) {
			return;
		}
		try {
			// 範囲制限.
			if (zoomFactor < 0.2) {
				zoomFactor = 0.2;
			} else if (zoomFactor > 8.) {
				zoomFactor = 8.;
			}

			JViewport vp = previewImgScrollPane.getViewport();

			Point viewCenter;
			if (mousePos != null) {
				// スクロールペインのマウス座標を表示パネルの位置に換算する.
				viewCenter = SwingUtilities.convertPoint(this, mousePos, previewImgPanel);

			} else {
				// 表示パネル上の現在表示しているビューポートの中央の座標を求める
				Rectangle viewRect = vp.getViewRect();
				viewCenter = new Point(
						(viewRect.x + viewRect.width / 2),
						(viewRect.y + viewRect.height / 2)
						);
			}

			// 現在のビューサイズ(余白があれば余白も含む)
			Dimension viewSize = previewImgPanel.getScaledSize(true);
			
			// 倍率変更
			previewControlPanel.setZoomFactor(zoomFactor);
			previewImgPanel.setZoomFactor(zoomFactor);
			
			// 新しいのビューサイズ(余白があれば余白も含む)
			Dimension viewSizeAfter = previewImgPanel.getScaledSize(true);
			Dimension visibleSize = vp.getExtentSize();

			if (viewSize != null && viewSizeAfter != null &&
				viewSizeAfter.width > 0 && viewSizeAfter.height > 0 &&
				viewSizeAfter.width > visibleSize.width &&
				viewSizeAfter.height > visibleSize.height) {
				// 新しいビューの大きさよりも表示可能領域が小さい場合のみ
				vp.setViewSize(viewSizeAfter);

				// スクロールペインに表示されている画面サイズを求める.
				// スクロールバーがある方向は、コンテンツの最大と等しいが
				// スクロールバーがない場合は画面サイズのほうが大きいため、
				// 倍率変更による縦横の移動比は、それぞれ異なる.
				int visible_width = max(visibleSize.width, viewSize.width);
				int visible_height = max(visibleSize.height, viewSize.height);
				int visible_width_after = max(visibleSize.width, viewSizeAfter.width);
				int visible_height_after = max(visibleSize.height, viewSizeAfter.height);

				// 前回の倍率から今回の倍率の倍率.
				// オリジナルに対する倍率ではない.
				// また、画像は縦横同率であるが表示ウィンドウはスクロールバー有無により同率とは限らない.
				double zoomDiffX = (double) visible_width_after / (double) visible_width;
				double zoomDiffY = (double) visible_height_after / (double) visible_height;
				
				// 拡大後の座標の補正
				Point viewCenterAfter = new Point();
				viewCenterAfter.x = (int) round(viewCenter.x * zoomDiffX);
				viewCenterAfter.y = (int) round(viewCenter.y * zoomDiffY);
				
				// 倍率適用前後の座標の差分
				int diff_x = viewCenterAfter.x - viewCenter.x;
				int diff_y = viewCenterAfter.y - viewCenter.y;
				
				// スクロール
				scroll(diff_x, diff_y);
			}

			// スクロールの単位を画像1ドットあたりの表示サイズに変更する.
			// (ただし1を下回らない)
			JScrollBar vsb = previewImgScrollPane.getVerticalScrollBar();
			JScrollBar hsb = previewImgScrollPane.getHorizontalScrollBar();
			vsb.setUnitIncrement(max(1, (int) ceil(zoomFactor)));
			hsb.setUnitIncrement(max(1, (int) ceil(zoomFactor)));

		} finally {
			zoomLock.release();
		}
	}
	
	/**
	 * プレビューに表示するタイトル.<br>
	 * 
	 * @param title
	 *            タイトル
	 */
	public void setTitle(String title) {
		if (title == null) {
			title = "";
		}
		if (!title.equals(this.title)) {
			this.title = title;
			lblTitle.setText(title + (indicatorShown ? indicatorText : ""));
			lblTitle.setToolTipText(title);
		}
	}
	
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * ロードに時間がかかっているか判定し、 インジケータを表示するためのタイマーイベントハンドラ.<br>
	 */
	protected void onTimer() {
		boolean waiting;
		long firstRequest;
		synchronized (lock) {
			waiting = isWaiting();
			firstRequest = firstWaitingTimestamp;
		}
		boolean indicatorShown = (waiting && ((System.currentTimeMillis() - firstRequest) > indicatorDelay));
		if (this.indicatorShown != indicatorShown) {
			this.indicatorShown = indicatorShown;
			lblTitle.setText(title + (indicatorShown ? indicatorText : ""));
		}
	}
	
	/**
	 * チケットの状態が、ロード完了待ち状態であるか?<br>
	 * ロード中のチケットが、ロード完了のチケットより新しければロード中と見なす.<br>
	 * 
	 * @return 完了待ちであればtrue、そうでなければfalse
	 */
	protected boolean isWaiting() {
		synchronized (lock) {
			return loadingTicket > loadedTicket;
		}
	}

	/**
	 * ロード要求が出されるたびに、そのロード要求チケットを登録する.<br>
	 * チケットは要求されるたびに増加するシーケンスとする.<br>
	 * 
	 * @param ticket
	 *            ロード要求チケット
	 */
	public void setLoadingRequest(long ticket) {
		synchronized (lock) {
			if ( !isWaiting() && this.loadedTicket < ticket) {
				// 現在認識しているチケットの状態がロード完了であり、
				// それよりも新しいチケットが要求されたならば、
				// 今回のチケットから待ち時間の計測を開始する.
				this.firstWaitingTimestamp = System.currentTimeMillis();
			}
			this.loadingTicket = ticket;
		}
	}
	
	/**
	 * ロード完了するたびに呼び出される.<br>
	 * 
	 * @param ticket
	 *            ロード要求チケット.
	 */
	public void setLoadingComplete(long ticket) {
		synchronized (lock) {
			this.loadedTicket = ticket;
		}
	}

	/**
	 * 表示画像を設定する.<br>
	 * 
	 * @param previewImg
	 *            表示画像、もしくはnull
	 */
	public void setPreviewImage(BufferedImage previewImg) {
		previewImgPanel.setPreviewImage(previewImg);
	}

	/**
	 * 表示されている画像を取得する.<br>
	 * 表示画像が設定されていなければnull.<br>
	 * 
	 * @return 表示画像、もしくはnull
	 */
	public BufferedImage getPreviewImage() {
		return previewImgPanel.getPreviewImage();
	}
	
	/**
	 * 表示している画面イメージそのままを取得する.
	 * 
	 * @return 表示画像
	 */
	public BufferedImage getScreenImage() {
		JViewport vp = previewImgScrollPane.getViewport();
		Dimension dim = vp.getExtentSize();
		BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			vp.paint(g);
			
		} finally {
			g.dispose();
		}
		return img;
	}
	
	/**
	 * 壁紙を設定する.<br>
	 * 
	 * @param wallpaperImg
	 *            壁紙、null不可
	 */
	public void setWallpaper(Wallpaper wallpaper) {
		previewImgPanel.setWallpaper(wallpaper);
	}
	
	/**
	 * 壁紙を取得する.<br>
	 * 壁紙が未設定の場合は空の壁紙インスタンスが返される.<br>
	 * 
	 * @return 壁紙
	 */
	public Wallpaper getWallpaper() {
		return previewImgPanel.getWallpaper();
	}
	
	/**
	 * 表示倍率を取得する.
	 * 
	 * @return 表示倍率
	 */
	public double getZoomFactor() {
		return previewControlPanel.getZoomFactor();
	}
	
	/**
	 * 表示倍率を設定する
	 * 
	 * @param zoomFactor
	 *            表示倍率
	 */
	public void setZoomFactor(double zoomFactor) {
		previewControlPanel.setZoomFactor(zoomFactor);
	}
	
	/**
	 * ズームパネルのピン留め制御
	 * 
	 * @param visible
	 *            表示する場合はtrue
	 */
	public void setVisibleZoomBox(boolean visible) {
		previewControlPanel.setPinned(visible);
	}
	
	/**
	 * ズームパネルがピン留めされているか?
	 * 
	 * @return ピン留めされていればtrue
	 */
	public boolean isVisibleZoomBox() {
		return previewControlPanel.isPinned();
	}

	public void addPreviewPanelListener(PreviewPanelListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		listeners.add(listener);
	}
	
	public void removePreviewPanelListener(PreviewPanelListener listener) {
		listeners.remove(listener);
	}
	
	protected void savePicture(PreviewPanelEvent e) {
		for (PreviewPanelListener listener : listeners) {
			listener.savePicture(e);
		}
	}
	
	protected void flipHolizontal(PreviewPanelEvent e) {
		for (PreviewPanelListener listener : listeners) {
			listener.flipHorizontal(e);
		}
	}
	
	protected void copyPicture(PreviewPanelEvent e) {
		for (PreviewPanelListener listener : listeners) {
			listener.copyPicture(e);
		}
	}

	protected void changeBackgroundColor(PreviewPanelEvent e) {
		for (PreviewPanelListener listener : listeners) {
			listener.changeBackgroundColor(e);
		}
	}

	protected void showInformation(PreviewPanelEvent e) {
		for (PreviewPanelListener listener : listeners) {
			listener.showInformation(e);
		}
	}

	protected void addFavorite(PreviewPanelEvent e) {
		for (PreviewPanelListener listener : listeners) {
			listener.addFavorite(e);
		}
	}
}

/**
 * チェック情報の表示用レイヤーパネル.<br>
 * 
 * @author seraphy
 */
class CheckInfoLayerPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(CheckInfoLayerPanel.class.getName());

	/**
	 * ボックスの余白
	 */
	private Insets padding = new Insets(3, 3, 3, 3);
	
	/**
	 * 表示位置プロパティ
	 */
	private Point pos = new Point();
	
	/**
	 * 表示メッセージプロパティ.<br>
	 * ¥nで改行となる.<br>
	 * 空文字ならば非表示.<br>
	 */
	private String message = "";
	
	/**
	 * 解析済みメッセージ.<br>
	 * 業に分割される.<br>
	 * 空文字は空のリストとなる.<br>
	 */
	private String[] messageLines;
	
	/**
	 * 解析済みフォントの高さ.<br>
	 */
	private int fontHeight;
	
	/**
	 * 描画済みエリア.<br>
	 * 次回描画前に消去する必要のある領域.<br>
	 * まだ一度も描画してなければnull.<br>
	 */
	private Rectangle eraseRect;
	
	/**
	 * 現在、描画すべきエリア.<br>
	 * なければnull.<br>
	 */
	private Rectangle requestRect;
	
	/**
	 * 画面に関連づけられていない状態でのテキスト表示サイズは 計算できないため、画面追加時に再計算させるための 予約フラグ.<br>
	 */
	private boolean requestRecalcOnAdd;
	
	/**
	 * フォントのためのデスクトップヒント.(あれば)
	 */
	@SuppressWarnings("rawtypes")
	private Map desktopHintsForFont;
	
	/**
	 * 透明コンポーネントとして構築する.<br>
	 */
	@SuppressWarnings("rawtypes")
	public CheckInfoLayerPanel() {
		setOpaque(false);
		
		Toolkit tk = Toolkit.getDefaultToolkit();
		desktopHintsForFont = (Map) tk.getDesktopProperty("awt.font.desktophints");
		logger.log(Level.CONFIG, "awt.font.desktophints=" + desktopHintsForFont);
	}
	
	/**
	 * 指定エリアに情報を描画する.<br>
	 */
	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0;
		super.paintComponent(g);

		// クリップ領域
		Rectangle clip = g.getClipBounds();
		// System.out.println("clip:" + clip + " /eraseRect:" + eraseRect + " /drawRect:" + requestRect);

		// 削除すべき領域が描画範囲に含まれているか?
		// (含まれていれば、その領域は消去済みである.)
		if (clip == null || (eraseRect != null && clip.contains(eraseRect))) {
			eraseRect = null;
		}
		
		// 表示領域の判定
		if (requestRect == null || requestRect.isEmpty()
				|| !(clip != null && clip.intersects(requestRect))) {
			// 表示すべき領域が存在しないか、描画要求範囲にない.
			return;
		}
		if (messageLines == null || messageLines.length == 0) {
			// 表示するものがなければ何もしない.
			return;
		}

		// フォントのレンダリングヒント
		if (desktopHintsForFont != null) {
			g.addRenderingHints(desktopHintsForFont);
		}

		// 箱の描画
		g.setColor(new Color(255, 255, 255, 192));
		g.fillRect(requestRect.x, requestRect.y, requestRect.width, requestRect.height);
		g.setColor(Color.GRAY);
		g.drawRect(requestRect.x, requestRect.y, requestRect.width - 1, requestRect.height - 1);
		
		// 情報の描画
		g.setColor(Color.BLACK);
		int oy = fontHeight;
		for (String messageLine : messageLines) {
			g.drawString(messageLine, requestRect.x + padding.left, requestRect.y + padding.top - 1 + oy);
			oy += fontHeight;
		}
		
		// 描画された領域を次回消去領域として記憶する.
		if (eraseRect == null || eraseRect.isEmpty()) {
			// 消去済みであれば、今回分のみを次回消去領域とする.
			eraseRect = (Rectangle) requestRect.clone();

		} else {
			// 消去済みエリアが未消去で残っている場合は
			// 今回領域を結合する.
			eraseRect.add(requestRect);
		}
	}

	/**
	 * 画面にアタッチされた場合、描画領域の再計算が 必要であれば計算する.<br>
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		if (requestRecalcOnAdd) {
			requestRecalcOnAdd = false;
			calcRepaint();
		}
	}
	
	/**
	 * 要求されたプロパティから、フォント高さによる表示領域を計算し、 その領域の再描画を要求する.(描画する内容がなれば、描画要求しない.)<br>
	 * 前回表示領域があれば、消去するために、そのエリアも再描画を要求する.<br>
	 * それ以外のエリアは描画要求しない.(描画の最適化による負荷軽減策)<br>
	 * フォントサイズを求めるためにグラフィクスへのアクセスが必要となるが、 まだ取得できない場合は{@link #addNotify()}の呼び出し時に
	 * 再計算するようにフラグを立てておく.<br>
	 */
	protected void calcRepaint() {
		Graphics2D g = (Graphics2D) getGraphics();
		if (g == null) {
			requestRecalcOnAdd = true;
			return;
		}
		try {
			// 前回描画領域のクリアのために呼び出す.
			if (eraseRect != null && !eraseRect.isEmpty()) {
				repaint(eraseRect);
			}
			
			// 空であれば新たな描画なし.
			if (message.length() == 0) {
				requestRect = null;
				return;
			}

			FontMetrics fm = g.getFontMetrics();
			String[] messageLines = message.split("¥n");
			
			Rectangle2D rct = null;
			for (String messageLine : messageLines) {
				Rectangle2D tmp = fm.getStringBounds(messageLine, g);
				if (rct != null) {
					rct.add(tmp);

				} else {
					rct = tmp;
				}
			}
			
			int fw = (int) rct.getWidth();
			int fh = (int) rct.getHeight();

			int w = fw + padding.left + padding.right;
			int h = fh * messageLines.length + padding.top + padding.bottom;
			
			// 指定した位置の右上あたりにする
			int x = pos.x + 16;
			int y = pos.y - h;
			
			// サイズ
			int client_w = getWidth();
			int client_h = getHeight();
			
			if (x + w > client_w) {
				// 画面右の場合はカーソルの左に移動
				x = pos.x - w - 10;
			}
			if (y < 0) {
				// 画面上の場合はカーソルの下に移動
				y = pos.y + 10;
			}
			if (y + h > client_h) {
				y -= (y + h - client_h);
			}
			
			// 結果の格納
			this.requestRect = new Rectangle(x, y, w, h);
			this.messageLines = messageLines;
			this.fontHeight = fh;
			
			// 再描画の要求
			Rectangle paintRect = (Rectangle) requestRect.clone();
			repaint(paintRect);
			
		} finally {
			g.dispose();
		}
	}
	
	public void setPotision(Point requestPt) {
		if (requestPt == null) {
			throw new IllegalArgumentException();
		}
		if ( !requestPt.equals(pos)) {
			Point oldpos = pos;
			pos = (Point) requestPt.clone();
			calcRepaint();
			firePropertyChange("position", oldpos, pos); 
		}
	}
	
	public Point getPosition() {
		return (Point) pos.clone();
	}
	
	public void setMessage(String message) {
		if (message == null) {
			message = "";
		}
		message = message.replace("¥r¥n", "¥n");
		if ( !message.equals(this.message)) {
			String oldmes = this.message;
			this.message = message;
			calcRepaint();
			firePropertyChange("message", oldmes, message);
		}
	}

	public String getMessage() {
		return message;
	}
}

/**
 * 画像表示パネル
 * 
 * @author seraphy
 */
class PreviewImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * 背景モード.<br>
	 */
	private BackgroundColorMode bgColorMode;
	
	/**
	 * 壁紙.<br>
	 */
	private Wallpaper wallpaper;
	
	/**
	 * 壁紙変更イベントのリスナ
	 */
	private PropertyChangeListener wallpaperListener;
	
	/**
	 * 透過オリジナル画像.<br>
	 */
	private BufferedImage previewImg;
	
	/**
	 * 表示用画像(背景モードによる調整あり).<br>
	 * 事前に拡大縮小を適用済みの場合は、{@link #scaledZoomFactor}に 適用している倍率が設定される.<br>
	 * 表示用に改めてイメージを生成する必要がない場合は、 透過オリジナルと同じインスタンスとなりえる.<br>
	 */
	private BufferedImage previewImgForDraw;
	
	/**
	 * 表示用画像がスケール済みである場合、そのスケールが設定される.<br>
	 * スケール済みでない場合はnullとなる.<br>
	 */
	private Double scaledZoomFactor;
	
	
	/**
	 * 倍率
	 */
	private double zoomFactor = 1.;
	
	/**
	 * 許容誤差
	 */
	private static final double TOLERANT = 0.001;

	
	/**
	 * コンストラクタ
	 */
	public PreviewImagePanel() {
		super();

		// 通常モード
		bgColorMode = BackgroundColorMode.ALPHABREND;

		// 壁紙変更通知リスナ
		wallpaperListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				onChangeWallpaper();
			}
		};
		
		// 壁紙
		wallpaper = new Wallpaper();
		wallpaper.addPropertyChangeListener(wallpaperListener);
	}
	
	/**
	 * 画像を表示する.
	 */
	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0;
		super.paintComponent(g);

		if (previewImgForDraw == null) {
			return;
		}

		// 倍率を適用した画像を画面の中央に配置できるように計算する.
		// (画像が倍率適用済みであれば1倍とする)
		Rectangle imgRct = adjustImageRectangle();
		
		// 表示用画像がスケール済みでない場合はレンダリングオプションを適用する.
		if (scaledZoomFactor == null) {
			Object renderingOption = getRenderingOption();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderingOption);
		}

		// 背景処理
		if (bgColorMode == BackgroundColorMode.ALPHABREND) {
			// 表示の最大範囲 (可視領域外も含む)
			int w = getWidth();
			int h = getHeight();
			wallpaper.drawWallpaper(g, w, h);
		}
		
		// レンダリング
		g.drawImage(previewImgForDraw,
				imgRct.x, imgRct.y,
				imgRct.x + imgRct.width, imgRct.y + imgRct.height,
				0, 0,
				previewImgForDraw.getWidth(), previewImgForDraw.getHeight(),
				null);

		// 通常モード以外のグリッド描画に該当するモードはグリッドを前景に描く
		AppConfig appConfig = AppConfig.getInstance();
		int drawGridMask = appConfig.getDrawGridMask();
		if ((drawGridMask & bgColorMode.mask()) != 0) {
			Color oldc = g.getColor();
			try {
				g.setColor(new Color(appConfig.getPreviewGridColor(), true));
				drawGrid(g, imgRct.x, imgRct.y, appConfig.getPreviewGridSize());
				
			} finally {
				g.setColor(oldc);
			}
		}
	}


	/**
	 * グリッドを描画する.<br>
	 * 開始位置の-1単位位置から画像サイズの+1単位までがグリッド範囲となる。
	 * 
	 * @param g
	 * @param offset_x
	 *            開始位置
	 * @param offset_y
	 *            開始位置
	 * @param unit
	 *            グリッド単位(pixel)
	 */
	protected void drawGrid(Graphics2D g, int offset_x, int offset_y, int unit) {
		Rectangle clip = g.getClipBounds();
		
		int src_w = previewImg.getWidth();
		int src_h = previewImg.getHeight();
		int my = src_h / unit;
		int mx = src_w / unit;
		
		int st_x = offset_x + (int)(-1 * unit * zoomFactor);
		int en_x = offset_x + (int)((mx + 1) * unit * zoomFactor);
		int w = en_x - st_x + 1;
		
		for (int y = -1; y <= my + 1; y++) {
			int y1 = y * unit;
			Rectangle rct = new Rectangle(
					st_x, offset_y + (int)(y1 * zoomFactor),
					w, 1);
			if (clip == null || clip.intersects(rct)) {
				g.drawLine(rct.x, rct.y, rct.x + rct.width, rct.y);
			}
		}
		
		int st_y = offset_y + (int)(-1 * unit * zoomFactor);
		int en_y = offset_y + (int)((my + 1) * unit * zoomFactor);
		int h = en_y - st_y + 1;
		
		for (int x = -1; x <= mx + 1; x++) {
			int x1 = x * unit;
			Rectangle rct = new Rectangle(
					offset_x + (int)(x1 * zoomFactor), st_y,
					1, h);
			g.drawLine(rct.x, rct.y, rct.x, rct.y + rct.height);
		}
	}
	
	/**
	 * 現在の倍率に応じたレンダリングオプションを取得する.<br>
	 * 
	 * @return レンダリングオプション
	 */
	protected Object getRenderingOption() {
		AppConfig appConfig = AppConfig.getInstance();
		double rendringOptimizeThreshold;
		if (bgColorMode == BackgroundColorMode.ALPHABREND) {
			rendringOptimizeThreshold = appConfig.getRenderingOptimizeThresholdForNormal();
		} else {
			rendringOptimizeThreshold = appConfig.getRenderingOptimizeThresholdForCheck();
		}
		Object renderingHint;
		if (zoomFactor < rendringOptimizeThreshold) {
			// 補正を適用する最大倍率以内である場合
			if (zoomFactor <= 1. || !appConfig.isEnableInterpolationBicubic()) {
				// 縮小する場合、もしくはバイキュービックをサポートしない場合
				renderingHint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
			} else {
				// 拡大する場合でバイキュービックをサポートしている場合
				renderingHint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
			}
			
		} else {
			// 補正を適用する最大倍率を超えている場合は補正なし.
			renderingHint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		}
		return renderingHint;
	}
	
	/**
	 * 倍率と、画面のサイズと、表示するオリジナルの画像サイズをもとに、 倍率を適用した画像サイズを、画面に収まる位置に補正して返す.<br>
	 * 返される矩形の幅と高さ(width, height)は拡大後の画像サイズに等しい.<br>
	 * 拡大後の画像が画面よりも小さければセンタリングするように矩形の開始位置(x, y)がオフセットされる.<br>
	 * そうでなければ矩形の開始位置(x, y)は0となる.<br>
	 * 画像が設定されていなければ幅と高さがゼロの矩形が返される.<br>
	 * 
	 * @return 画像を表示するオフセットと大きさ、もしくは空の矩形
	 */
	public Rectangle adjustImageRectangle() {
		if (previewImg == null) {
			return new Rectangle(0, 0, 0, 0); // 幅・高さともにゼロ
		}
		int client_w = getWidth();
		int client_h = getHeight();
		
		int src_w = previewImg.getWidth();
		int src_h = previewImg.getHeight();
		
		int w = (int) round(src_w * zoomFactor);
		int h = (int) round(src_h * zoomFactor);
		
		int offset_x = 0;
		if (w < client_w) {
			offset_x = (client_w - w) / 2;
		}
		int offset_y = 0;
		if (h < client_h) {
			offset_y = (client_h - h) / 2;
		}
		
		return new Rectangle(offset_x, offset_y, w, h);
	}

	/**
	 * パネルのマウス座標から、実寸の画像のピクセル位置を返す.<br>
	 * 画像が表示されていないか、範囲外であればnullを返す.<br>
	 * 
	 * @param pt
	 *            パネルの座標
	 * @return 画像の位置、もしくはnull
	 */
	public Point getImagePosition(Point pt) {
		if (pt == null || previewImg == null) {
			// プレビュー画像が設定されていなければnull
			return null;
		}

		Rectangle imgRct = adjustImageRectangle();

		if ( !imgRct.contains(pt.x, pt.y)) {
			// 範囲外であればnull
			return null;
		}
		
		// オフセットを除去する.
		Point ret = (Point) pt.clone();
		ret.x -= imgRct.x;
		ret.y -= imgRct.y;
		
		// 倍率を解除する.
		ret.x = (int) floor(ret.x / zoomFactor);
		ret.y = (int) floor(ret.y / zoomFactor);
		
		return ret;
	}

	/**
	 * 画像の位置から画面の位置を割り出す.<br>
	 * 
	 * @param pt
	 *            画像の位置
	 * @return 画面の位置
	 */
	public Point getMousePosition(Point pt) {
		if (pt == null || previewImg == null) {
			// プレビュー画像が設定されていなければnull
			return null;
		}
		
		Rectangle imgRct = adjustImageRectangle();

		// 表示倍率を加える
		Point ret = (Point) pt.clone();
		ret.x = (int) ceil(ret.x * zoomFactor);
		ret.y = (int) ceil(ret.y * zoomFactor);
		
		// オフセットを加える
		ret.x += imgRct.x;
		ret.y += imgRct.y;
		
		return ret;
	}
	
	/**
	 * 指定した位置のRGB値を取得する.<br>
	 * 範囲外の場合は0が返される.<br>
	 * 
	 * @param pt
	 *            イメージの位置
	 * @return イメージのARGB値 (ビット順序は、A:24, R:16, G:8, B:0)
	 */
	public int getImageARGB(Point pt) {
		if (pt == null) {
			throw new IllegalArgumentException();
		}
		try {
			return previewImg.getRGB(pt.x, pt.y);

		} catch (RuntimeException ex) {
			return 0; // 範囲外
		}
	}
	
	/**
	 * 倍率を適用した画像パネルのサイズを計算し適用する.<br>
	 * モードにより余白が加えられる.<br>
	 */
	protected void recalcScaledSize() {
		Dimension scaledSize = getScaledSize(true);
		if (scaledSize != null) {
			setPreferredSize(scaledSize);
			revalidate();
		}
	}

	/**
	 * 元画像の倍率適用後のサイズを返す.<br>
	 * 元画像が設定されていなければnull.<br>
	 * needOffsetがfalseであれば表示モードに関わらず、画像の拡大・縮小後の純粋なサイズ、
	 * trueであれば余白が必要な表示モードの場合の余白が付与された場合のサイズが返される.<br>
	 * 
	 * @param needOffset
	 *            余白を加味したサイズが必要な場合はtrue
	 * @return 倍率適用後のサイズ、もしくはnull
	 */
	protected Dimension getScaledSize(boolean needOffset) {
		if (previewImg == null) {
			return null;
		}
		int src_w = previewImg.getWidth();
		int src_h = previewImg.getHeight();

		int w = (int) round(src_w * zoomFactor);
		int h = (int) round(src_h * zoomFactor);
		
		Dimension scaledSize = new Dimension(w, h);

		if (bgColorMode != BackgroundColorMode.ALPHABREND) {
			// 通常モード以外は画像よりも少し大きめにすることで
			// キャンバスに余白をつける
			AppConfig appConfig = AppConfig.getInstance();
			int unfilledSpace = appConfig.getPreviewUnfilledSpaceForCheckMode();
			scaledSize.width += max(0, unfilledSpace * 2);
			scaledSize.height += max(0, unfilledSpace * 2);
		}
		
		return scaledSize;
	}

	/**
	 * プレビュー画像を設定する.
	 * 
	 * @param previewImg
	 */
	public void setPreviewImage(BufferedImage previewImg) {
		BufferedImage oldimg = this.previewImg;
		this.previewImg = previewImg;
		
		recalcScaledSize();
		makeDrawImage(true);
		repaint();
		
		firePropertyChange("previewImage", oldimg, previewImg);
	}
	
	public BufferedImage getPreviewImage() {
		return previewImg;
	}

	/**
	 * 壁紙を設定する.
	 * 
	 * @param wallpaper
	 */
	public void setWallpaper(Wallpaper wallpaper) {
		if (wallpaper == null) {
			throw new IllegalArgumentException();
		}
		if ( !this.wallpaper.equals(wallpaper)) {
			Wallpaper wallpaperOld = this.wallpaper;
			if (wallpaperOld != null) {
				wallpaperOld.removePropertyChangeListener(wallpaperListener);
			}
			this.wallpaper = wallpaper;
			if (this.wallpaper != null) {
				this.wallpaper.addPropertyChangeListener(wallpaperListener);
			}
			firePropertyChange("wallpaper", wallpaperOld, this.wallpaper);
			onChangeWallpaper();
		}
	}
	
	public Wallpaper getWallpaper() {
		return wallpaper;
	}
	
	protected void onChangeWallpaper() {
		repaint();
	}
	
	/**
	 * 背景モード調整済みの表示用画像を作成する.
	 * 
	 * @param changeImage
	 *            画像の変更あり
	 */
	protected void makeDrawImage(boolean changeImage) {
		if (previewImg == null) {
			// 画像が設定されていなければ空
			this.previewImgForDraw = null;
			scaledZoomFactor = null;
			return;
		}

		BufferedImage img;
		if (changeImage || scaledZoomFactor != null) {
			// 画像が変更されているか、スケール済みであれば
			// 背景モードの再適用が必要.
			if (bgColorMode == BackgroundColorMode.ALPHABREND) {
				// アルファブレンド通常モードは背景用にあえて作成する必要はない.
				img = previewImg;

			} else {
				// アルファブレンド通常モード以外は背景に作成する
				Color bgColor = wallpaper.getBackgroundColor();
				BackgroundColorFilter bgColorFilter = new BackgroundColorFilter(bgColorMode, bgColor);
				img = bgColorFilter.filter(previewImg, null);
			}

		} else {
			// 画像が変更されておらず、スケール済みでもなければ
			// すでに作成済みの画像が使用できる.
			img = previewImgForDraw;
		}
		
		// レンダリングオプション
		Object renderingOption = getRenderingOption();

		// バイキュービックでなければ、事前の拡大縮小は行わずに、表示時に行う.
		if ( !renderingOption.equals(RenderingHints.VALUE_INTERPOLATION_BICUBIC)) {
			previewImgForDraw = img;
			scaledZoomFactor = null;
			return;
		}

		// バイキュービックの場合、倍率を適用したサイズに予め加工しておく.
		Dimension scaledSize = getScaledSize(false);
		BufferedImage offscreen = new BufferedImage(
				scaledSize.width, scaledSize.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = offscreen.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			
			g.drawImage(img,
					0, 0, scaledSize.width, scaledSize.height,
					0, 0, img.getWidth(), img.getHeight(),
					null);
			
		} finally {
			g.dispose();
		}
		previewImgForDraw = offscreen;
		scaledZoomFactor = Double.valueOf(zoomFactor);
	}
	
	public void setBackgroundColorMode(BackgroundColorMode bgColorMode) {
		if (bgColorMode == null) {
			throw new IllegalArgumentException();
		}
		if (this.bgColorMode != bgColorMode) {
			BackgroundColorMode oldcm = bgColorMode;
			this.bgColorMode = bgColorMode;
			
			makeDrawImage(true);
			recalcScaledSize();
			repaint();
			
			firePropertyChange("backgroundColorMode", oldcm, bgColorMode);
		}
	}

	public BackgroundColorMode setBackgroundColorMode() {
		return bgColorMode;
	}

	public void setZoomFactor(double zoomFactor) {
		if (abs(zoomFactor - this.zoomFactor) > TOLERANT) {
			// 0.001未満の差異は誤差とみなして反映しない.
			double oldzoom = this.zoomFactor;
			this.zoomFactor = zoomFactor;

			recalcScaledSize();
			makeDrawImage(false);
			repaint();

			firePropertyChange("zoomFactor", oldzoom, zoomFactor);
		}
	}
	
	public double getZoomFactor() {
		return zoomFactor;
	}
	
	/**
	 * 倍率が100%であるか?
	 * 
	 * @return 100%であればtrue
	 */
	public boolean isDefaultZoom() {
		return zoomFactor - 1 < TOLERANT;
	}
}

/**
 * 倍率・背景モードを操作するための下部パネル用
 * 
 * @author seraphy
 */
class PreviewControlPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(PreviewControlPanel.class.getName());

	protected static final String STRINGS_RESOURCE = "languages/previewpanel";

	/**
	 * ピン留めチェックボックス
	 */
	private JCheckBox chkPinning;
	
	/**
	 * アルファ確認チェックボックス
	 */
	private JCheckBox chkNoAlpha;

	/**
	 * グレースケール確認チェックボックス
	 */
	private JCheckBox chkGrayscale;

	/**
	 * 倍率用スライダ
	 */
	private JSlider zoomSlider;
	
	/**
	 * 倍率入力用コンボボックス
	 */
	private JComboBox zoomCombo;
	

	/**
	 * スライダの最小値
	 */
	private static final int MIN_INDEX = -170;
	
	/**
	 * スライダの最大値
	 */
	private static final int MAX_INDEX = 219;
	
	/**
	 * 最小倍率
	 */
	private double minimumZoomFactor;
	
	/**
	 * 最大倍率
	 */
	private double maximumZoomFactor;
	
	
	/**
	 * 現在の倍率(100倍済み)
	 */
	private int currentZoomFactorInt;
	
	/**
	 * 現在の背景色モード
	 */
	private BackgroundColorMode backgroundColorMode;

	
	/**
	 * 任意の底Aをもつ対数 logA(N)を計算して返す.
	 * 
	 * @param a
	 *            底
	 * @param x
	 *            引数
	 * @return logA(N)
	 */
	private static double logN(double a, double x) {
		return log(x) / log(a);
	}
	
	/**
	 * 倍率(等倍を1とする)に対するスライダのインデックス値を返す.<br>
	 * スライダは10ステップごとに前のステップの10%づつ増減する.(複利式)<br>
	 * 
	 * @param zoomFactor
	 *            倍率(1を等倍とする)
	 * @return インデックス
	 */
	private static int zoomFactorToIndex(double zoomFactor) {
		return (int) round(logN((1. + 0.1), zoomFactor) * 10);
	}
	
	/**
	 * スライダのインデックス値から倍率(等倍を1とする)を返す.<br>
	 * 10ステップごとに10%づつ増減する.(複利式)<br>
	 * 
	 * @param index
	 *            インデックス
	 * @return 倍率(1を等倍とする)
	 */
	private static double zoomFactorFromIndex(int index) {
		return pow(1. + 0.1, index / 10.);
	}
	
	
	/**
	 * コンストラクタ
	 */
	public PreviewControlPanel() {
		final Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);
		
		UIHelper uiHelper = UIHelper.getInstance();

		// ピンアイコン
		Icon pinIcon = uiHelper.createTwoStateIcon(
				"icons/pin-icon1.png", "icons/pin-icon2.png");
		
		// ピンチェックボックス
		chkPinning = new JCheckBox(pinIcon);
		chkPinning.setToolTipText(strings.getProperty("tooltip.zoompanel.pinning"));

		// 円ボタン型チェックボックス用アイコンの実装
		final Icon stateIcon = new Icon() {
			public int getIconHeight() {
				return 12;
			}
			public int getIconWidth() {
				return 6;
			};
			public void paintIcon(Component c, Graphics g, int x, int y) {
				boolean sw = false;
				if (c instanceof AbstractButton) {
					AbstractButton btn = (AbstractButton) c;
					sw = btn.isSelected();
				}
				
				int w = getIconWidth();
				int h = getIconHeight();
				
				int s = min(w, h);
				
				int ox = 0;
				int oy = 0;
				if (w > s) {
					ox = (w - s) / 2;
				}
				if (h > s) {
					oy = (h - s) / 2;
				}
				
				if (sw) {
					AppConfig appConfig = AppConfig.getInstance();
					Color fillColor = appConfig.getSelectedItemBgColor();
					g.setColor(fillColor);
					g.fillOval(x + ox, y + oy, s, w);
				}
				g.setColor(Color.GRAY);
				g.drawOval(x + ox, y + oy, s, s);
			}
		}; 
		
		// アルファ確認とグレースケール確認用のチェックボックス
		chkNoAlpha = new JCheckBox(stateIcon);
		chkGrayscale = new JCheckBox(stateIcon);

		chkNoAlpha.setToolTipText(strings.getProperty("tooltip.zoompanel.checkalpha"));
		chkGrayscale.setToolTipText(strings.getProperty("tooltip.zoompanel.checkgrayscale"));
		
		backgroundColorMode = BackgroundColorMode.ALPHABREND;
		
		final ChangeListener chkAlphaGrayChangeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				onChangeCheckAlphaGray();
			}
		};
		chkNoAlpha.addChangeListener(chkAlphaGrayChangeListener);
		chkGrayscale.addChangeListener(chkAlphaGrayChangeListener);

		// 倍率スライダ
		zoomSlider = new JSlider(JSlider.HORIZONTAL, MIN_INDEX, MAX_INDEX, 0);
		zoomSlider.setToolTipText(strings.getProperty("tooltip.zoompanel.zoomfactor_slider"));

		// 倍率コンボ
		zoomCombo = new JComboBox();
		zoomCombo.setToolTipText(strings.getProperty("tooltip.zoompanel.zoomfactor_combo"));

		// 倍率の既定リストの設定と、最大・最小値の算定
		minimumZoomFactor = zoomFactorFromIndex(zoomSlider.getMinimum());
		maximumZoomFactor = zoomFactorFromIndex(zoomSlider.getMaximum());

		int minZoomRange = (int) round(minimumZoomFactor * 100.);
		int maxZoomRange = (int) round(maximumZoomFactor * 100.);
		
		List<Integer> predefinedZoomRanges = getPredefinedZoomRanges();
		for (int zoomRange : predefinedZoomRanges) {
			if (zoomRange < minZoomRange) {
				minZoomRange = zoomRange;
			}
			if (zoomRange > maxZoomRange) {
				maxZoomRange = zoomRange;
			}
			zoomCombo.addItem(Integer.toString(zoomRange));
		}
		final int[] zoomRanges = {minZoomRange, maxZoomRange};
		
		currentZoomFactorInt = 100;
		zoomCombo.setSelectedItem(Integer.toString(currentZoomFactorInt));
		zoomCombo.setEditable(true);
		if ( !Main.isMacOSX()) {
			// Windows環境だとデフォルトで9桁分のテキストフィールドが作成され
			// それがレイアウトの推奨サイズとして実際に使われてしまうため、
			// 明示的に3桁にとどめておくようにオーバーライドする.
			// Mac OS Xならば問題ない.
			zoomCombo.setEditor(new BasicComboBoxEditor() {
				{
					editor = new JTextField(3) {
						private static final long serialVersionUID = 1L;
						@Override
						public void setBorder(Border border) {
							// 何もしない.
						}
						public void setText(String s) {
							if (getText().equals(s)) {
								return;
							}
							super.setText(s);
						}
					};
				}
			});
		}
		
		// スライダを変更することによりコンボボックスを変更する、
		// もしくはコンボボックスを変更することでスライダを変更するが、
		// 互いに通知を呼び合うことになるため他方を無視するためのセマフォ
		final Semaphore changeLock = new Semaphore(1);

		zoomCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean adjusted = false;
				String value = (String) zoomCombo.getSelectedItem();
				int zoomFactorInt;
				try {
					zoomFactorInt = Integer.parseInt(value);
					if (zoomFactorInt < zoomRanges[0]) {
						zoomFactorInt = zoomRanges[0];
						adjusted = true;

					} else if (zoomFactorInt > zoomRanges[1]) {
						zoomFactorInt = zoomRanges[1];
						adjusted = true;
					}
					
				} catch (RuntimeException ex) {
					zoomFactorInt = 100;
					adjusted = true;
				}
				if (adjusted) {
					zoomCombo.setSelectedItem(Integer.toString(zoomFactorInt));
					Toolkit tk = Toolkit.getDefaultToolkit();
					tk.beep();
				}
				if (changeLock.tryAcquire()) {
					try {
						zoomSlider.setValue(zoomFactorToIndex(zoomFactorInt / 100.));
						
					} finally {
						changeLock.release();
					}
				}
				fireZoomFactorChange(zoomFactorInt);
			}
		});
		
		zoomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int index = zoomSlider.getValue();
				double zoomFactor = zoomFactorFromIndex(index);
				int zoomFactorInt = (int) round(zoomFactor * 100);

				if (changeLock.tryAcquire()) {
					try {
						zoomCombo.setSelectedItem(Integer.toString(zoomFactorInt));
						
					} finally {
						changeLock.release();
					}
					fireZoomFactorChange(zoomFactorInt);
				}
			}
		});

		// パーツの配備
		
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(0, 0, 0, 5);
		gbc.weightx = 0.;
		gbc.weighty = 0.;
		
		add(chkPinning, gbc);
		
		gbc.gridx = 1;
		gbc.weightx = 0.;
		gbc.insets = new Insets(0, 0, 0, 0);
		add(chkGrayscale, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0.;
		gbc.insets = new Insets(0, 0, 0, 5);
		add(chkNoAlpha, gbc);

		gbc.gridx = 3;
		gbc.weightx = 1.;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(zoomSlider, gbc);
		
		gbc.gridx = 4;
		gbc.weightx = 0.;
		gbc.insets = new Insets(3, 0, 3, 0);
		gbc.fill = GridBagConstraints.VERTICAL;
		add(zoomCombo, gbc);

		Integer scrollbarWidth = (Integer) UIManager.get("ScrollBar.width");
		logger.log(Level.CONFIG, "ScrollBar.width=" + scrollbarWidth);
		if (scrollbarWidth == null) {
			scrollbarWidth = Integer.parseInt(
					strings.getProperty("uiconstraint.scrollbar.width"));
		}
		
		gbc.gridx = 5;
		gbc.weightx = 0.;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 0, scrollbarWidth);
		add(new JLabel("%"), gbc);
	}
	
	/**
	 * アプリケーション設定より事前定義済みの倍率候補を取得する
	 * 
	 * @return 倍率候補のリスト(順序あり)
	 */
	protected List<Integer> getPredefinedZoomRanges() {
		AppConfig appConfig = AppConfig.getInstance();
		String strs = appConfig.getPredefinedZoomRanges();
		TreeSet<Integer> ranges = new TreeSet<Integer>();
		for (String str : strs.split(",")) {
			str = str.trim();
			if (str.length() > 0) {
				try {
					int zoomFactor = Integer.parseInt(str);
					ranges.add(Integer.valueOf(zoomFactor));

				} catch (RuntimeException ex) {
					// 無視する.
				}
			}
		}
		ranges.add(Integer.valueOf(100)); // 等倍は常に設定する.
		return new ArrayList<Integer>(ranges);
	}
	
	/**
	 * 倍率が変更されたことを通知する.
	 */
	protected void fireZoomFactorChange(int newZoomFactor) {
		if (currentZoomFactorInt != newZoomFactor) {
			int oldValue = currentZoomFactorInt;
			currentZoomFactorInt = newZoomFactor;
			firePropertyChange("zoomFactorInt", oldValue, newZoomFactor);
		}
	}
	
	private Semaphore changeChkLock = new Semaphore(1);
	
	protected void onChangeCheckAlphaGray() {
		changeChkLock.tryAcquire();
		try {
			BackgroundColorMode backgroundColorMode = BackgroundColorMode.valueOf(
					chkNoAlpha.isSelected(),
					chkGrayscale.isSelected()
					);
			setBackgroundColorMode(backgroundColorMode);

		} finally {
			changeChkLock.release();
		}
	}
	
	public BackgroundColorMode getBackgroundColorMode() {
		return this.backgroundColorMode;
	}
	
	public void setBackgroundColorMode(BackgroundColorMode backgroundColorMode) {
		if (backgroundColorMode == null) {
			throw new IllegalArgumentException();
		}

		BackgroundColorMode oldcm = this.backgroundColorMode;
		if (oldcm != backgroundColorMode) {
			this.backgroundColorMode = backgroundColorMode;
			changeChkLock.tryAcquire();
			try {
				chkNoAlpha.setSelected(backgroundColorMode.isNoAlphaChannel());
				chkGrayscale.setSelected(backgroundColorMode.isGrayscale());

			} finally {
				changeChkLock.release();
			}
			firePropertyChange("backgroundColorMode", oldcm, backgroundColorMode);
		}
	}
	
	public boolean isPinned() {
		return chkPinning.isSelected();
	}
	
	public void setPinned(boolean pinned) {
		chkPinning.setSelected(pinned);
		if (isDisplayable()) {
			setVisible(pinned);
		}
	}
	
	public double getZoomFactor() {
		return (double) currentZoomFactorInt / 100.;
	}

	public void setZoomFactor(double zoomFactor) {
		if (zoomFactor < minimumZoomFactor) {
			zoomFactor = minimumZoomFactor;
		}
		if (zoomFactor > maximumZoomFactor) {
			zoomFactor = maximumZoomFactor;
		}
		int zoomFactorInt = (int) round(zoomFactor * 100.);
		if (zoomFactorInt != currentZoomFactorInt) {
			zoomCombo.setSelectedItem(Integer.toString(zoomFactorInt));
		}
	}
}
