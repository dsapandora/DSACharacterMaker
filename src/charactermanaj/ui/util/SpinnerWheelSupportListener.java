package charactermanaj.ui.util;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

/**
 * スピナーをホイールによって上下できるようにするためのホイールリスナ.
 * @author seraphy
 */
public class SpinnerWheelSupportListener implements MouseWheelListener {

	/**
	 * 対象となるスピナーのモデル
	 */
	protected SpinnerModel model;
	
	/**
	 * スピナーのモデルを指定して構築します.
	 * @param model
	 */
	public SpinnerWheelSupportListener(SpinnerModel model) {
		if (model == null) {
			throw new IllegalArgumentException();
		}
		this.model = model;
	}
	
	/**
	 * ホイールによりスピナーモデルの現在値を上下させる.<br>
	 * モデルが数値型である場合は範囲チェックをし、その範囲で適用します.<br>
	 * 数値外であれば適用した結果エラーとなる場合は単に無視します.<br>
	 * @param e マウスホイールイベント
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		int rotate = e.getWheelRotation();
		Object nextval = null;
		if (rotate < 0) {
			// 上スクロール(up)
			nextval = model.getNextValue();

		} else if (rotate > 0) {
			// 下スクロール(down)
			nextval = model.getPreviousValue();
		}
		
		if (nextval != null) {
			if (model instanceof SpinnerNumberModel) {
				SpinnerNumberModel nmodel = (SpinnerNumberModel) model;
				@SuppressWarnings("unchecked")
				Comparable<Number> max = nmodel.getMaximum();
				@SuppressWarnings("unchecked")
				Comparable<Number> min = nmodel.getMinimum();
				if (max.compareTo((Number) nextval) < 0) {
					nextval = null;
				} else if (min.compareTo((Number) nextval) > 0) {
					nextval = null;
				}
			}
			try {
				if (nextval != null) {
					model.setValue(nextval);
				}
				
			} catch (IllegalArgumentException ex) {
				// 範囲外になった場合はIllegalArgumentExceptionが発生するが、
				// ユーザ操作によるものなので単に無視する
			}
		}
	}
	
}
