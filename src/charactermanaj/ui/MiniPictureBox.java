package charactermanaj.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

/**
 * サンプルイメージを表示する小さなピクチャボックス.<br>
 * 非同期イメージに対応している.<br>
 * @author seraphy
 */
public class MiniPictureBox extends JPanel {
	
	private static final long serialVersionUID = 3210952907784110605L;

	/**
	 * 表示するイメージ.<br>
	 * 非同期読み込みのイメージにも対応.<br>
	 */
	private Image image;
	
	/**
	 * 読み込みエラーが発生しているか?
	 */
	private boolean errorOccured;

	/**
	 * コンストラクタ
	 */
	public MiniPictureBox() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		setPreferredSize(new Dimension(150, 300));
	}

	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0;
		super.paintComponent(g);

		Image img = getImage();  
		if (img != null) {
			if (errorOccured) {
				FontMetrics fm = g.getFontMetrics();
				String message = "ERROR";
				Rectangle2D rct = fm.getStringBounds(message, g);
				Insets insets = getInsets();
				g.drawString(message, insets.left, insets.top + (int) rct.getHeight());

			} else {
				// 画像の読み込みに失敗しているか?
				checkImage(img, -1, -1, new ImageObserver() {
					public boolean imageUpdate(Image img, int infoflags, int x, int y,
							int width, int height) {
						if ((infoflags & (ImageObserver.ERROR)) != 0) {
							errorOccured = true;
							repaint();
						}
				        return true;
					}
				});

				if ( !prepareImage(img, this)) {
					// まだロードできていない場合は
					// ロードできるまで表示しない.
					return;
				}

				// イメージサイズの取得
				int imgW = img.getWidth(this);
				int imgH = img.getHeight(this);

				// 表示エリア
				int x = 0;
				int y = 0;
				int w = getWidth();
				int h = getHeight();
				Insets insets = getInsets();
				if (insets != null) {
					x = insets.left;
					y = insets.top;
					w -= insets.left + insets.right;
					h -= insets.top + insets.bottom;
				}
				
				// 倍率算定
				double vx = (double) w / (double) imgW;
				double vy = (double) h / (double) imgH;
				
				double factor = Math.min(vx, vy);
				
				int scaledW = (int)(imgW * factor);
				int scaledH = (int)(imgH * factor);
				int offset_x = (w - scaledW) / 2;
				int offset_y = (h - scaledH) / 2;

				// 描画
				g.setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);

				g.drawImage(img, x + offset_x, y + offset_y, x
						+ offset_x + scaledW, y + offset_y + scaledH,
						0, 0, imgW, imgH,
						this);
			}
		}
	}
	
	public Image getImage() {
		return image;
	}
	
	public void setImage(Image image) {
		Image oldimg = this.image;
		if ((oldimg != null && image == null)
				|| (image != null && (oldimg == null || !oldimg.equals(image)))) {
			this.image = image;
			this.errorOccured = false;
			repaint();
			firePropertyChange("image", oldimg, image);
		}
	}
}