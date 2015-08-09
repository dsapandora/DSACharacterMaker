package charactermanaj.ui.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsColorManager;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.ui.ImageSelectPanel;

/**
 * パーツ選択ペインの管理クラス.
 * @author seraphy
 */
public class PartsSelectionManager {
	
	/**
	 * 背景色を管理するオブジェクトが実装するインターフェイス.
	 * @author seraphy
	 */
	public interface ImageBgColorProvider {
		
		/**
		 * 背景色を取得する.
		 * @return 背景色
		 */
		Color getImageBgColor();

		/**
		 * 背景色を設定する
		 * @param imageBgColor 背景色
		 */
		void setImageBgColor(Color imageBgColor);
	}

	/**
	 * カテゴリ別パーツ選択パネル.<br>
	 */
	private HashMap<PartsCategory, ImageSelectPanel> imageSelectPanels = new HashMap<PartsCategory, ImageSelectPanel>();
	
	/**
	 * パーツカラーマネージャ.<br>
	 */
	private PartsColorManager partsColorMrg;
	
	/**
	 * 背景色プロバイダ.
	 */
	private ImageBgColorProvider imageBgColorProvider;
	
	/**
	 * アフィン変換用のパラメータ.<br>
	 * 変換しない場合はnull.<br>
	 */
	private double[] affineTransformParameter;
	
	/**
	 * 単一選択カテゴリの選択解除の有効／無効.<br>
	 * 有効にする場合はtrue.<br>
	 */
	private boolean deselectableAllCategory;
	
	
	/**
	 * コンストラクタ
	 * @param partsColorMrg パーツカラーマネージャ
	 * @param imageBgColorProvider 背景色プロバイダ
	 */
	public PartsSelectionManager(
			PartsColorManager partsColorMrg,
			ImageBgColorProvider imageBgColorProvider
			) {
		if (partsColorMrg == null || imageBgColorProvider == null) {
			throw new IllegalArgumentException();
		}
		this.partsColorMrg = partsColorMrg;
		this.imageBgColorProvider = imageBgColorProvider;
	}
	

	/**
	 * パーツをロードする.
	 */
	public void loadParts() {
		for (ImageSelectPanel panel : imageSelectPanels.values()) {
			panel.loadParts();
		}
	}
	
	public void register(ImageSelectPanel imageSelectPanel) {
		if (imageSelectPanel == null) {
			throw new IllegalArgumentException();
		}
		imageSelectPanels.put(imageSelectPanel.getPartsCategory(), imageSelectPanel);
	}

	public List<PartsIdentifier> getSelectedPartsIdentifiers(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		ImageSelectPanel panel = imageSelectPanels.get(partsCategory);
		if (panel != null) {
			return Collections.unmodifiableList(panel.getSelectedPartsIdentifiers());
		}
		return Collections.emptyList();
	}

	public PartsIdentifier getSelectedPartsIdentifier(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		ImageSelectPanel panel = imageSelectPanels.get(partsCategory);
		if (panel != null) {
			return panel.getSelectedPartsIdentifier();
		}
		return null;
	}
	
	public Collection<PartsCategory> getAllCategories() {
		ArrayList<PartsCategory> partsCategories = new ArrayList<PartsCategory>();
		partsCategories.addAll(imageSelectPanels.keySet());
		return partsCategories;
	}
	
	/**
	 * 各カテゴリの選択状態と背景をパーツセットで指定されたものに設定します.<br>
	 * @param partsSet パーツセット
	 */
	public void selectPartsSet(PartsSet partsSet) {
		if (partsSet == null) {
			throw new IllegalArgumentException();
		}

		for (ImageSelectPanel panel : imageSelectPanels.values()) {
			PartsCategory partsCategory = panel.getPartsCategory();
			List<PartsIdentifier> partsIdentifiers = partsSet.get(partsCategory);
			panel.selectParts(partsIdentifiers);
			if (partsIdentifiers != null) {
				for (PartsIdentifier partsIdentifier : partsIdentifiers) {
					PartsColorInfo partsColorInfo = partsSet.getColorInfo(partsIdentifier);
					if (partsColorInfo != null) {
						partsColorMrg.setPartsColorInfo(partsIdentifier, partsColorInfo, false);
					}
				}
			}
		}

		Color bgColor = partsSet.getBgColor();
		if (bgColor != null) {
			setImageBgColor(bgColor); 
		}
		
		affineTransformParameter = partsSet.getAffineTransformParameter(); // clone済み
	}
	
	/**
	 * 現在の選択中のパーツと色設定からパーツセットを構築します.
	 * 選択がなにもない場合は空のパーツセットとなります.<br>
	 * @return パーツセット
	 */
	public PartsSet createPartsSet() {
		PartsSet presetParts = new PartsSet();
		for (ImageSelectPanel imageSelectPanel : imageSelectPanels.values()) {
			PartsCategory category = imageSelectPanel.getPartsCategory();
			for (PartsIdentifier partsIdentifier : imageSelectPanel.getSelectedPartsIdentifiers()) {
				PartsColorInfo partsColorInfo = partsColorMrg.getPartsColorInfo(partsIdentifier, false);
				presetParts.appendParts(category, partsIdentifier, partsColorInfo);
			}
		}
		presetParts.setBgColor(getImageBgColor());
		presetParts.setAffineTransformParameter(affineTransformParameter); // 相手側でcloneする
		return presetParts;
	}

	/**
	 * すべてのカテゴリのリストで選択中のアイテムが見えるようにスクロールする.
	 */
	public void scrollToSelectedParts() {
		for (ImageSelectPanel imageSelectPanel : imageSelectPanels.values()) {
			imageSelectPanel.scrollToSelectedRow();
		}
	}
	
	/**
	 * 指定したパーツ識別子にフォーカスを当てて、必要に応じてスクロールします.<br>
	 * 該当するパーツ識別子がどこにもなければ何もしません.<br>
	 * @param partsIdentifier パーツ識別子
	 */
	public void setSelection(PartsIdentifier partsIdentifier) {
		if (partsIdentifier == null) {
			return;
		}
		PartsCategory partsCategory = partsIdentifier.getPartsCategory();
		if (isMinimizeMode(partsCategory)) {
			setMinimizeModeIfOther(partsCategory, true);
		}
		ImageSelectPanel imageSelectPanel = imageSelectPanels.get(partsCategory);
		if (imageSelectPanel != null) {
			imageSelectPanel.setSelection(partsIdentifier);
		}
	}
	
	/**
	 * 背景色を取得する.
	 * @return 背景色
	 */
	protected Color getImageBgColor() {
		return imageBgColorProvider.getImageBgColor();
	}

	/**
	 * 背景色を設定する
	 * @param imageBgColor 背景色
	 */
	protected void setImageBgColor(Color imageBgColor) {
		imageBgColorProvider.setImageBgColor(imageBgColor);
	}

	/**
	 * アフィン変換用のパラメータを取得する.<br>
	 * 変換しない場合はnull.<br>
	 * @return アフィン変換用のパラメータ、もしくはnull
	 */
	public double[] getAffineTransformParameter() {
		return affineTransformParameter == null ? null : affineTransformParameter.clone();
	}
	
	/**
	 * アフィン変換用のパラメータを設定する.<br>
	 * 変換しない場合はnull.<br>
	 * 要素数は4または6でなければならない.<br>
	 * @param affineTransformParameter アフィン変換用のパラメータ、もしくはnull
	 */
	public void setAffineTransformParameter(double[] affineTransformParameter) {
		if (affineTransformParameter != null && !(affineTransformParameter.length == 4 || affineTransformParameter.length == 6)) {
			throw new IllegalArgumentException("affineTransformParameter invalid length.");
		}
		this.affineTransformParameter = affineTransformParameter == null ? null : affineTransformParameter.clone();
	}

	/**
	 * 選択選択パーツカテゴリの選択解除を許可するか?<br>
	 * @return 許可する場合はtrue
	 */
	public boolean isDeselectableSingleCategory() {
		return deselectableAllCategory;
	}
	
	/**
	 * 選択選択パーツカテゴリの選択解除を許可するか設定する.<br>
	 * @param deselectable 許可する場合はtrue
	 */
	public void setDeselectableSingleCategory(boolean deselectable) {
		this.deselectableAllCategory = deselectable;
		for (ImageSelectPanel imageSelectPanel : this.imageSelectPanels.values()) {
			imageSelectPanel.setDeselectableSingleCategory(deselectable);
		}
	}
	
	/**
	 * パーツ選択をすべて解除する.<br>
	 * 単一選択カテゴリが解除されるかどうかは、{@link #isDeselectableSingleCategory()}による.<br>
	 */
	public void deselectAll() {
		for (ImageSelectPanel imageSelectPanel : this.imageSelectPanels.values()) {
			if (imageSelectPanel.getPartsCategory().isMultipleSelectable()
					|| imageSelectPanel.isDeselectableSingleCategory()) {
				imageSelectPanel.deselectAll();
			}
		}
	}
	
	/**
	 * 指定したカテゴリ以外のパネルを最小化する.<br>
	 * (指定したカテゴリがnullでなければ、そのカテゴリの最小化は解除される.)<br>
	 * @param partsCategory 最小化の対象外のパネル、nullの場合は不問
	 * @param minimize 指定したカテゴリ以外を最小化する場合はtrue、falseの場合はすべてが最小化解除される.
	 */
	public void setMinimizeModeIfOther(PartsCategory partsCategory, boolean minimize) {
		for (Map.Entry<PartsCategory, ImageSelectPanel> entry : imageSelectPanels.entrySet()) {
			PartsCategory cat = entry.getKey();
			ImageSelectPanel imageSelectPanel = entry.getValue();
			if (partsCategory != null && cat.equals(partsCategory)) {
				imageSelectPanel.setMinimizeMode(false);
			} else {
				imageSelectPanel.setMinimizeMode(minimize);
			}
		}
	}
	
	/**
	 * 指定したカテゴリが最小化モードであるか?
	 * @param partsCategory カテゴリ
	 * @return 指定したカテゴリが最小化モードであればtrue、該当するカテゴリがない場合は常にfalse
	 */
	public boolean isMinimizeMode(PartsCategory partsCategory) {
		ImageSelectPanel panel = imageSelectPanels.get(partsCategory);
		if (panel == null) {
			return false;
		}
		return panel.isMinimizeMode();
	}
	
	/**
	 * 指定したカテゴリが最小化モードでなく、且つ、他がすべて最小化モードであるか?
	 * (指定カテゴリがない場合は全パネルが最小化モードである場合)<br>
	 * @param partsCategory カテゴリ、もしくはnull
	 * @return 指定したカテゴリが最小化モードでなく、且つ、他がすべて最小化モードである場合はtrue
	 */
	public boolean isNotMinimizeModeJust(PartsCategory partsCategory) {
		for (Map.Entry<PartsCategory, ImageSelectPanel> entry : imageSelectPanels.entrySet()) {
			PartsCategory cat = entry.getKey();
			ImageSelectPanel imageSelectPanel = entry.getValue();
			if (partsCategory != null) {
				if (cat.equals(partsCategory)) {
					// 指定したカテゴリが最小化モードであればfalse
					if (imageSelectPanel.isMinimizeMode()) {
						return false;
					}
				} else {
					// 指定したカテゴリ以外が最小化モードでなければfalse
					if ( !imageSelectPanel.isMinimizeMode()) {
						return false;
					}
				}
			}
		}
		// 指定カテゴリ以外がすべて最小化モードである場合
		// (指定カテゴリがない場合は全パネルが最小化モードである場合)
		return true;
	}
}
