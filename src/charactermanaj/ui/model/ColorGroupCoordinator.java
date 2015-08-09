package charactermanaj.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.ColorInfo;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsColorManager;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.ui.ColorDialog;

/**
 * 同一カラーグループでの連動をサポートするためのコーディネータオブジェクト.<br>
 * @author seraphy
 */
public class ColorGroupCoordinator {
	
	/**
	 * パーツの選択状態を管理するマネージャ.<br>
	 */
	private final PartsSelectionManager partsSelectionMrg;
	
	/**
	 * 色ダイアログのコレクション
	 */
	private final LinkedList<ColorDialog> colorDialogs = new LinkedList<ColorDialog>();
	
	/**
	 * カラーグループの変更通知を受けるリスナーのコレクション.<br>
	 */
	private final LinkedList<ColorChangeListener> listeners = new LinkedList<ColorChangeListener>();
	
	/**
	 * パーツ識別子ごとの色情報を管理するパーツカラーマネージャ
	 */
	private final PartsColorManager partsColorMrg;

	/**
	 * 色ダイアログからの通知を受け取るリスナ.(自クラス内オブジェクト)
	 */
	protected final ColorChangeListener listener;
	
	/**
	 * コンストラクタ
	 * @param partsSelectionMrg パーツの選択を管理するマネージャ
	 * @param partsColorMrg パーツ識別子ごとの色を管理するマネージャ
	 */
	public ColorGroupCoordinator(PartsSelectionManager partsSelectionMrg, PartsColorManager partsColorMrg) {
		if (partsSelectionMrg == null || partsColorMrg == null) {
			throw new IllegalArgumentException();
		}
		this.partsSelectionMrg = partsSelectionMrg;
		this.partsColorMrg = partsColorMrg;
		
		listener = new ColorChangeListener() {
			public void onColorChange(ColorChangeEvent event) {
				Layer layer = event.getLayer();
				ColorDialog colorDialog = (ColorDialog) event.getSource();
				PartsCategory partsCategory = colorDialog.getPartsCategory();
				ColorGroupCoordinator.this.syncColorGroup(partsCategory, layer, colorDialog);
				ColorGroupCoordinator.this.fireColorChangeEvent(event);
			}
			public void onColorGroupChange(ColorChangeEvent event) {
				Layer layer = event.getLayer();
				ColorDialog colorDialog = (ColorDialog) event.getSource();
				ColorGroup colorGroup = colorDialog.getColorGroup(layer);
				ColorGroupCoordinator.this.onChangeColorGroup(colorDialog, layer, colorGroup);
				ColorGroupCoordinator.this.fireColorGroupChangeEvent(event);
			}
		};
	}
	
	/**
	 * カラーダイアログを登録する.<br>
	 * @param colorDialog カラーダイアログ
	 */
	public void registerColorDialog(ColorDialog colorDialog) {
		if (colorDialog == null) {
			throw new IllegalArgumentException();
		}
		this.colorDialogs.add(colorDialog);
		colorDialog.addColorChangeListener(listener);
	}
	
	/**
	 * カラーダイアログの登録を解除する.<br>
	 * @param colorDialog カラーダイアログ
	 */
	public void unregisterColorDialog(ColorDialog colorDialog) {
		Iterator<ColorDialog> ite = colorDialogs.iterator();
		while (ite.hasNext()) {
			ColorDialog dlg = ite.next();
			if (dlg == colorDialog) {
				dlg.removeColorChangeListener(listener);
				ite.remove();
			}
		}
	}
	
	/**
	 * カラーグループが変更されたことを通知するリスナを登録する.<br>
	 * @param listener リスナー
	 */
	public void addColorChangeListener(ColorChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		listeners.add(listener);
	}

	/**
	 * カラーグループが変更されたことを通知するリスナを登録解除する.<br>
	 * @param listener リスナー
	 */
	public void removeColorChangeListener(ColorChangeListener listener) {
		listeners.remove(listener);
	}
	
	protected void fireColorChangeEvent(ColorChangeEvent e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		for (ColorChangeListener listener : listeners) {
			listener.onColorChange(e);
		}
	}
	
	protected void fireColorGroupChangeEvent(ColorChangeEvent e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		for (ColorChangeListener listener : listeners) {
			listener.onColorGroupChange(e);
		}
	}
	
	protected void onChangeColorGroup(ColorDialog destColorDialog, Layer layer, ColorGroup colorGroup) {
		if (destColorDialog == null || layer == null || colorGroup == null) {
			throw new IllegalArgumentException();
		}
		for (ColorDialog colorDlg : colorDialogs) {
			for (Layer srcLayer : colorDlg.getPartsCategory().getLayers()) {
				if (!srcLayer.equals(layer)) {
					if (ColorGroup.equals(colorGroup, colorDlg.getColorGroup(srcLayer))
							&& colorDlg.isSyncColorGroup(srcLayer)) {
						ColorConvertParameter param = colorDlg.getColorConvertParameter(srcLayer);
						destColorDialog.setColorConvertParameter(layer, param);
						break;
					}
				}
			}
		}
	}
	
	/**
	 * パーツの色ダイアログが変更されたことにより、同一の他のカラーグループのレイヤーのカラーダイアログの設定値をコピーする.<br>
	 * (色ダイアログのパラメータ変更により呼び出される.)<br>
	 * @param partsCategory パーツカテゴリ
	 * @param eventSourceLayer 変更もとのレイヤー
	 * @param sourceColorDialog 変更されたカラーダイアログ
	 */
	public void syncColorGroup(PartsCategory partsCategory, Layer eventSourceLayer, ColorDialog sourceColorDialog) {
		if (partsCategory == null || sourceColorDialog == null) {
			throw new IllegalArgumentException();
		}

		// 変更もと
		ArrayList<Layer> syncSources = new ArrayList<Layer>();
		if (eventSourceLayer != null) {
			if (sourceColorDialog.isSyncColorGroup(eventSourceLayer)) {
				syncSources.add(eventSourceLayer);
			}
		} else {
			for (Layer layer2 : partsCategory.getLayers()) {
				if (sourceColorDialog.isSyncColorGroup(layer2)) {
					syncSources.add(layer2);
				}
			}
		}
		
		// 変更もとのレイヤーのカラーグループを他のレイヤーにも適用する.
		for (Layer sourceLayer : syncSources) {
			ColorGroup sourceColorGroup = sourceColorDialog.getColorGroup(sourceLayer);
			if (sourceColorGroup != null && sourceColorGroup.isEnabled()) {
				ColorConvertParameter param = sourceColorDialog.getColorConvertParameter(sourceLayer);
				
				// 他のパネルに適用する
				for (ColorDialog targetColorDialog : colorDialogs) {
					for (Layer targetLayer : targetColorDialog.getPartsCategory().getLayers()) {
						if (!targetLayer.equals(sourceLayer)) {
							if (ColorGroup.equals(targetColorDialog.getColorGroup(targetLayer), sourceColorGroup)) {
								if (targetColorDialog.isSyncColorGroup(targetLayer)) {
									targetColorDialog.setColorConvertParameter(targetLayer, param);
								}
							}
						}
					}
				}
				// 色ダイアログで選択中でない有効なパーツも含めてパーツカラーを更新する.
				for (PartsCategory targetPartsCategory : partsSelectionMrg.getAllCategories()) {
					Collection<PartsIdentifier> selectedPartss = partsSelectionMrg.getSelectedPartsIdentifiers(targetPartsCategory);
					for (PartsIdentifier partsIdentifier : selectedPartss) {
						// カラーダイアログで選択されていない他のパーツも含めてパーツカラーを更新する.
						PartsColorInfo partsColorInfo = partsColorMrg.getPartsColorInfo(partsIdentifier, true);
						for (Map.Entry<Layer, ColorInfo> entry : partsColorInfo.entrySet()) {
							ColorInfo colorInfo = entry.getValue();
							if (ColorGroup.equals(sourceColorGroup, colorInfo.getColorGroup())) {
								if (colorInfo.isSyncColorGroup()) {
									colorInfo.setColorParameter(param);
								}
							}
						}
					}
				}
			}
		}
	}
	
}
