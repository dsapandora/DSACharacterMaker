package charactermanaj.ui;

import static java.lang.Math.ceil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.AppConfig;

/**
 * 壁紙
 * @author seraphy
 */
public class Wallpaper {
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(Wallpaper.class.getName());

	/**
	 * 壁紙の推奨されるブロックサイズ(幅).<br>
	 * このサイズに満たない壁紙用画像は、このサイズに近い値まで敷き詰めて保持しておく.<br>
	 */
	private static final int wallpaperPreferredWidth = 128;

	/**
	 * 壁紙の推奨されるブロックサイズ(高さ).<br>
	 * このサイズに満たない壁紙用画像は、このサイズに近い値まで敷き詰めて保持しておく.<br>
	 */
	private static final int wallpaperPreferredHeight = 128;
	
	/**
	 * プロパティ変更イベントのキー名
	 */
	public static final String KEY_WALLPAPER_IMAGE = "wallpaperImage";
	
	/**
	 * プロパティ変更通知サポート.
	 */
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	/**
	 * 背景色.<br>
	 */
	private Color backgroundColor = Color.WHITE;
	
	/**
	 * 壁紙画像.<br>
	 */
	private BufferedImage wallpaperImg;
	
	/**
	 * 壁紙画像のアルファ値.<br>
	 */
	private float wallpaperAlpha = 1.f;
	
	/**
	 * 壁紙用オフスクリーンサーフェイス.<br>
	 * なければnull.<br>
	 */
	private VolatileImage wallpaperVolatileImg;
	
	/**
	 * 壁紙用オフスクリーンを生成したときに使用した背景色.<br>
	 * なければnull
	 */
	private Color wallpaperVolatileBgColor;
	
	
	/**
	 * 壁紙が表示されない状態で壁紙イメージを構築する.<br>
	 */
	public Wallpaper() {
		this(null);
	}

	/**
	 * 壁紙の元画像を指定して壁紙イメージを構築する.<br>
	 * nullを指定した場合は壁紙は表示されない.<br>
	 * @param wallpaperImg 壁紙イメージ、もしくはnull
	 */
	public Wallpaper(BufferedImage wallpaperImg) {
		this.wallpaperImg = makeExpandedWallpaper(makeExpandedWallpaper(wallpaperImg));
	}

	/**
	 * 壁紙画像を設定する.<br>
	 * nullの場合は解除される.<br>
	 * 壁紙が小さい場合は推奨されるブロックサイズまで敷き詰めなおした状態で
	 * 保持する.(描画不可軽減のため.)
	 * したがって、{@link #getWallpaperImage()}を呼び出したときには
	 * 拡張されたサイズとなっている.<br>
	 * @param wallpaperImg
	 */
	public void setWallpaperImage(BufferedImage wallpaperImg) {
		// 現在のオフスクリーンを破棄する.
		disposeOffscreen();

		// 新しいイメージ
		BufferedImage wallpaperImgOld = makeExpandedWallpaper(this.wallpaperImg);
		this.wallpaperImg = makeExpandedWallpaper(wallpaperImg);
		propertyChangeSupport.firePropertyChange("wallpaperImage", wallpaperImgOld, this.wallpaperImg);
	}

	/**
	 * 壁紙画像を取得する.<br>
	 * 壁紙画像はブロックサイズまで拡張されたものとなっている.<br>
	 * @return 壁紙画像、なければnull
	 */
	public BufferedImage getWallpaperImage() {
		return wallpaperImg;
	}
	
	public float getWallpaperAlpha() {
		return wallpaperAlpha;
	}
	
	/**
	 * 壁紙画像を描画する場合のアルファ値を設定する.<br>
	 * 負の値は0に、1以上は1に制限される.<br>
	 * @param wallpaperAlpha アルファ値(0から1の範囲)
	 */
	public void setWallpaperAlpha(float wallpaperAlpha) {
		// 現在のオフスクリーンを破棄する.
		disposeOffscreen();

		// 範囲無いに制限する.
		if (wallpaperAlpha < 0) {
			wallpaperAlpha = 0;
		} else if (wallpaperAlpha > 1.f) {
			wallpaperAlpha = 1.f;
		}

		// アルファ値を設定する.
		float oldalpha = this.wallpaperAlpha;
		if (oldalpha != wallpaperAlpha) {
			this.wallpaperAlpha = wallpaperAlpha;
			propertyChangeSupport.firePropertyChange("wallpaperAlpha", oldalpha, wallpaperAlpha);
		}
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	public void setBackgroundColor(Color backgroundColor) {
		// 現在のオフスクリーンを破棄する.
		disposeOffscreen();

		// 色が省略された場合の補正.
		if (backgroundColor == null) {
			backgroundColor = Color.WHITE;
		}
		
		// 背景色を設定する.
		Color colorOld = this.backgroundColor;
		if ( !colorOld.equals(backgroundColor)) {
			this.backgroundColor = backgroundColor;
			propertyChangeSupport.firePropertyChange("backgroundColor", colorOld, backgroundColor);
		}
	}

	/**
	 * 壁紙を左上(0,0)を原点に指定した幅・高さでタイル状に敷き詰めて描画します.<br>
	 * 壁紙が設定されていなければ何もしません.<br>
	 * アプリケーション設定でオフスクリーンの使用が有効である場合、グラフィクスコンテキストに
	 * あわせてオフスクリーンイメージをあらかじめキャッシュとして作成して転送する.<br>
	 * オフスクリーンは初回描画時に構築され、以降、必要に応じて再作成される.<br>
	 * オフスクリーンを即座に破棄する場合には{@link #disposeOffscreen()}を呼び出す.<br>
	 * @param g 描画先
	 * @param bgColor 背景色
	 * @param w 幅 (画面幅)
	 * @param h 高さ (画面高)
	 */
	public void drawWallpaper(Graphics2D g, int w, int h) {
		drawWallpaper(g, w, h, false);
	}
	
	/**
	 * 壁紙を左上(0,0)を原点に指定した幅・高さでタイル状に敷き詰めて描画します.<br>
	 * 壁紙が設定されていなければ何もしません.<br>
	 * アプリケーション設定でオフスクリーンの使用が有効である場合、グラフィクスコンテキストに
	 * あわせてオフスクリーンイメージをあらかじめキャッシュとして作成して転送する.<br>
	 * ただし、引数でオフスクリーンを使用しないと指定した場合はオフスクリーンには一切関知せず、
	 * 通常の画像による背景描画を行う.<br>
	 * (この場合、オフスクリーンは作成されず、現在あるものを再作成も破棄もしない.)
	 * @param g 描画先
	 * @param bgColor 背景色
	 * @param w 幅 (画面幅)
	 * @param h 高さ (画面高)
	 * @param noUseOffscreen オフスクリーンを使用しない.(たとえ利用可能であっても)
	 */
	public void drawWallpaper(Graphics2D g, int w, int h, boolean noUseOffscreen) {
		// 背景色
		Color bgColor = getBackgroundColor();
		
		// 背景色で塗りつぶす
		// (壁紙がある場合は不要)
		if (wallpaperImg == null) {
			fillBackgroundColor(g, bgColor, w, h);
		}
		
		// 壁紙を敷き詰める
		if (wallpaperImg != null) {
			// オフスクリーンによる背景描画が行われたか?
			boolean drawOffscreen = false;

			if ( !noUseOffscreen) {
				// オフクリーンサーフェイスのチェックまたは生成
				AppConfig appConfig = AppConfig.getInstance();
				if (appConfig.isEnableOffscreenWallpaper()) {
					checkOrCreateOffscreen(g, bgColor, w, h);
				} else {
					disposeOffscreen();
				}

				// オフスクリーンフェイスが有効であれば、オフスクリーンで描画する.
				if (wallpaperVolatileImg != null) {
					int src_w = wallpaperVolatileImg.getWidth();
					int src_h = wallpaperVolatileImg.getHeight();
					drawWallpaper(g, w, h, wallpaperVolatileImg, src_w, src_h);

					// オフスクリーンがロストしていなければ
					// オフスクリーンで描画されたと判定する.
					drawOffscreen = !wallpaperVolatileImg.contentsLost();
				}
			}

			// オフスクリーンサーフェイスで描画されていなければ通常の画像で描画する.
			if ( !drawOffscreen) {
				if (wallpaperVolatileImg != null && logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "offscreen is lost.");
				}

				fillBackgroundColor(g, bgColor, w, h);

				Composite oldcomp = g.getComposite();
				try {
					float alpha = getWallpaperAlpha();
					if (alpha < 1.f) {
						// アルファが100%の場合は、あえて設定しない.
						AlphaComposite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
						g.setComposite(comp);
					}
					
					int src_w = wallpaperImg.getWidth();
					int src_h = wallpaperImg.getHeight();
					drawWallpaper(g, w, h, wallpaperImg, src_w, src_h);

				} finally {
					g.setComposite(oldcomp);
				}
			}
		}
	}

	/**
	 * 現在使用しているオフスクリーン用ネイティブリソースを破棄する.<br>
	 */
	protected void disposeOffscreen() {
		if (wallpaperVolatileImg != null) {
			wallpaperVolatileImg.flush();
			wallpaperVolatileImg = null;

			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "オフスクリーンを破棄しました。");
			}
		}
	}
	
	/**
	 * オフスクリーンイメージが有効であるかチェックし、
	 * 有効でなければオフスクリーンを再作成、もしくは再描画する.<br>
	 * オフスクリーンは背景画像と同じサイズで作成される.<br>
	 * 背景画像が設定されていなければオフスクリーンも無効とする.<br>
	 * @param g 実際のスクリーンデバイス(互換性あるオフスクリーンを作成するため)
	 * @param bgColor 背景色
	 * @param offscreen_max_w オフスクリーンの最大サイズ(幅)
	 * @param offscreen_max_h オフスクリーンの最大サイズ(高さ)
	 */
	protected void checkOrCreateOffscreen(Graphics2D g, Color bgColor, int offscreen_max_w, int offscreen_max_h) {
		if (wallpaperImg == null) {
			// 壁紙もと画像がなければ何もしない.
			disposeOffscreen();
			return;
		}
		try {
			GraphicsConfiguration gConf = g.getDeviceConfiguration();
	
			// オフスクリーンの状態を確認する.
			int validate = VolatileImage.IMAGE_INCOMPATIBLE;
			if (wallpaperVolatileImg != null) {
				validate = wallpaperVolatileImg.validate(gConf);
				if (logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, "オフスクリーンの状態: "  + validate);
				}
			}
			
			// 構築時の背景色と変更があるか?
			if (validate == VolatileImage.IMAGE_OK
					&& (wallpaperVolatileBgColor != null && bgColor != null)) {
				if ( !wallpaperVolatileBgColor.equals(bgColor)) {
					validate = VolatileImage.IMAGE_RESTORED;
				}
			}
			
			// 壁紙元画像サイズ
			int src_w = wallpaperImg.getWidth();
			int src_h = wallpaperImg.getHeight();

			// オフスクリーンサイズの算定.
			// 要求された最大幅かアプリ設定の最大幅の小さいほうを最大幅とし、
			// それが壁紙もと画像よりも小さければ壁紙サイズと同じとする.
			AppConfig appConfig = AppConfig.getInstance();
			int offscreen_w = appConfig.getOffscreenWallpaperSize();
			int offscreen_h = appConfig.getOffscreenWallpaperSize();
			
			offscreen_w = Math.max(src_w, Math.min(offscreen_max_w, offscreen_w));
			offscreen_h = Math.max(src_h, Math.min(offscreen_max_h, offscreen_h));
			
			// ブロックサイズを満たすために必要な元サイズの繰り返し数
			int nx = (int) ceil((double) offscreen_w / src_w);
			int ny = (int) ceil((double) offscreen_h / src_h);
			
			// 繰り返し数からブロックサイズに近い元サイズで割り切れるサイズを求める
			offscreen_w = src_w * nx;
			offscreen_h = src_h * ny;

			// オフスクリーンが有効、もしくは描画が必要である状態の場合にサイズのチェックを行う.
			if (validate == VolatileImage.IMAGE_OK || validate == VolatileImage.IMAGE_RESTORED) {
				int currentOffW = Math.max(1, wallpaperVolatileImg.getWidth());
				int currentOffH = Math.max(1, wallpaperVolatileImg.getHeight());
				
				double ratioW = ((double) offscreen_w / currentOffW);
				double ratioH = ((double) offscreen_h / currentOffH);
				
				// オフスクリーンの描画済みサイズが要求サイズの2割を超えるか割り込んだ場合は
				// 再作成が必要.
				if (ratioW < 0.8 || ratioW > 1.2 || ratioH < 0.8 || ratioH > 1.2) {
					validate = VolatileImage.IMAGE_INCOMPATIBLE;

					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, "オフスクリーンサイズの変更が必要です。: " + ratioW + "," + ratioH);
					}
				}
			}
			
			// オフスクリーンの状態が再構築または再描画が必要であるか?
			if (validate != VolatileImage.IMAGE_OK ) {

				// オフスクリーンがないか、コンパチでなくなっている場合はオフスクリーンを生成する.
				if (wallpaperVolatileImg == null || validate == VolatileImage.IMAGE_INCOMPATIBLE) {
					// 現在使用しているネイティブリソースを破棄する.
					disposeOffscreen();

					// 新しいオフスクリーンサーフェイスを作成する.
					wallpaperVolatileImg = gConf.createCompatibleVolatileImage(
							offscreen_w, offscreen_h, Transparency.OPAQUE);
					
					if (wallpaperVolatileImg == null) {
						logger.log(Level.INFO, "オフスクリーンイメージは生成できません。");

					} else {
						if (logger.isLoggable(Level.FINER)) {
							logger.log(Level.FINER, "オフスクリーンを構築しました。(サイズ:"
									+ offscreen_w + "," + offscreen_h + ")");
						}
					}
				}

				// 再描画する.
				if (wallpaperVolatileImg != null) {
					if (logger.isLoggable(Level.FINER)) {
						logger.log(Level.FINER, "オフスクリーンの描画 (サイズ:"
								+ offscreen_w + "," + offscreen_h + ")");
					}
					
					Graphics2D vg = wallpaperVolatileImg.createGraphics();
					try {
						int ow = wallpaperVolatileImg.getWidth();
						int oh = wallpaperVolatileImg.getHeight();

						fillBackgroundColor(vg, bgColor, ow, oh);

						Composite oldcomp = vg.getComposite();
						try {
							float alpha = getWallpaperAlpha();
							if (alpha < 1.f) {
								// アルファが100%の場合は、あえて設定しない.
								AlphaComposite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
								vg.setComposite(comp);
							}

							drawWallpaper(vg, ow, oh, wallpaperImg, src_w, src_h);
							
						} finally {
							vg.setComposite(oldcomp);
						}
						
					} finally {
						vg.dispose();
					}
					
					wallpaperVolatileBgColor = bgColor;
				}
			}
			
		} catch (RuntimeException ex) {
			logger.log(Level.SEVERE, "オフスクリーンイメージの生成中に例外が発生しました。", ex);
			// 現在使用しているネイティブリソースを破棄する.
			disposeOffscreen();
		}
	}

	/**
	 * 背景色で塗りつぶす
	 * @param g 対象
	 * @param bgColor 背景色、nullの場合は何もしない.
	 * @param w 幅
	 * @param h 高さ
	 */
	protected void fillBackgroundColor(Graphics2D g, Color bgColor, int w, int h) {
		if (bgColor == null) {
			return;
		}
		Color oldc = g.getColor();
		try {
			Rectangle clip = g.getClipBounds();
			if (clip == null) {
				clip = new Rectangle(0, 0, w, h);
			}
			g.setColor(bgColor);
			g.fill(clip);
			
		} finally {
			g.setColor(oldc);
		}
	}
	
	/**
	 * 壁紙を指定の領域まで敷き詰めて描画する.<br>
	 * @param g
	 * @param w 敷き詰めるサイズ(幅)
	 * @param h 敷き詰めるサイズ(高さ)
	 * @param wallpaperImg 敷き詰める画像
	 * @param src_w 壁紙画像のサイズ
	 * @param src_v 壁紙画像の高さ
	 */
	protected void drawWallpaper(Graphics2D g, int w, int h, Image wallpaperImg, int src_w, int src_h) {
		if (wallpaperImg == null) {
			return;
		}
		
		// 表示範囲で表示できる壁紙を表示できる個数
		int nx = (int) ceil((double) w / src_w);
		int ny = (int) ceil((double) h / src_h);
		
		// 描画対象領域
		Rectangle clip = g.getClipBounds();

		// 描画対象領域にかかる壁紙を描画する. 
		Rectangle rct = new Rectangle(0, 0, src_w, src_h);
		for (int iy = 0; iy <= ny; iy++) {
			for (int ix = 0; ix <= nx; ix++) {
				rct.x = ix * src_w;
				rct.y = iy * src_h;
				if (clip == null || clip.intersects(rct)) {
					g.drawImage(
							wallpaperImg,
							rct.x, rct.y,
							rct.x + rct.width, rct.y + rct.height,
							0, 0,
							src_w, src_h,
							null
							);
				}
			}
		}
	}
	
	/**
	 * 壁紙を一定の大きさに敷き詰める.<br>
	 * すでに十分大きい場合は何もしない.<br>
	 * @param wallpaper 対象のイメージ
	 * @return 拡張後のイメージ、もしくは同じイメージ
	 */
	protected BufferedImage makeExpandedWallpaper(BufferedImage wallpaper) {
		if (wallpaper == null) {
			return null;
		}

		// 敷き詰める画像の元サイズ
		int src_w = wallpaper.getWidth();
		int src_h = wallpaper.getHeight();
		
		// ブロックサイズよりも元サイズが大きければ何もしない.
		if (src_w > wallpaperPreferredWidth && src_h > wallpaperPreferredHeight) {
			return wallpaper;
		}

		// ブロックサイズを満たすために必要な元サイズの繰り返し数
		int nx = (int) ceil((double) wallpaperPreferredWidth / src_w);
		int ny = (int) ceil((double) wallpaperPreferredHeight / src_h);
		
		// 繰り返し数からブロックサイズに近い元サイズで割り切れるサイズを求める
		int w = src_w * nx;
		int h = src_h * ny;
		
		// ブロックサイズまで元画像を敷き詰める
		BufferedImage wallpaperNew = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = wallpaperNew.createGraphics();
		try {
			drawWallpaper(g, w, h, wallpaper, src_w, src_h);
			
		} finally {
			g.dispose();
		}
		
		return wallpaperNew;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}
}
