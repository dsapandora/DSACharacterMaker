package charactermanaj.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.EventListenerList;

import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 色表示ボックス.<br>
 * 色が変更された場合はプロパティ「colorKey」に対するプロパティ変更リスナへの通知が行われます.<br>
 * ダブルクリックまたはボタンが押下され色を指定したことによるアクションリスナへの通知が行われます.<br>
 * 既定のコマンドは「colorKey」です.<br>
 * アクションは色選択ダイアログがOKされたことによるアクションであり、色が前後で変更されなくても通知されます.<br>
 * @author seraphy
 */
public class ColorBox extends JPanel {

	private static final long serialVersionUID = -8745278154296281466L;

	/**
	 * リソース
	 */
	protected static final String STRINGS_RESOURCE = "languages/colorbox";

	/**
	 * コマンド
	 */
    private String actionCommand = "colorKey";

    /**
     * 初期カラー
     */
    private Color colorKey;
	
    
    /**
     * 色の表示パネル 
     */
	private JPanel colorDisplayPanel;
	
	/**
	 * 色選択アクション
	 */
	private AbstractAction actChooseColor;
	
	
	/**
	 * 色ボックスを構築します.<br>
	 * 色選択ボタンが付与されます.<br>
	 */
	public ColorBox() {
		this(null, true);
	}
	
	/**
	 * 初期カラーと、色選択ボックスの表示有無を指定して構築します.
	 * @param colorKey 初期カラー
	 * @param colorPicker 色選択ボタンの表示有無
	 */
	public ColorBox(Color colorKey, boolean colorPicker) {
		if (colorKey == null) {
			colorKey = Color.WHITE;
		}
		this.colorKey = colorKey;

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(STRINGS_RESOURCE);

		colorDisplayPanel = createColorDiaplyPanel();
		colorDisplayPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (actChooseColor.isEnabled() && e.getClickCount() == 2) {
					onChooseColor(new ActionEvent(this, 1, getActionCommand(),
							e.getWhen(), e.getModifiers()));
				}
			}
		});
		
		colorDisplayPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 0, 0, 3),
				BorderFactory.createBevelBorder(BevelBorder.LOWERED)));
		colorDisplayPanel.setPreferredSize(new Dimension(32, 24));
		
		actChooseColor = new AbstractAction(strings.getProperty("btn.chooseColorKey")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onChooseColor(e);
			}
		};
		
		JButton btnChooseColor = new JButton(actChooseColor);
		btnChooseColor.setVisible(colorPicker);
		actChooseColor.setEnabled(colorPicker);
		
		setLayout(new BorderLayout());
		add(colorDisplayPanel, BorderLayout.CENTER);
		add(btnChooseColor, BorderLayout.EAST);
	}
	
	protected JPanel createColorDiaplyPanel() {
		return new JPanel() {
			private static final long serialVersionUID = -8554046012311330274L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				Rectangle rct = getBounds();
				Insets insets = getInsets();
				int x = insets.left;
				int y = insets.top;
				int w = rct.width - insets.left - insets.right;
				int h = rct.height - insets.top - insets.bottom;

				g.setColor(getColorKey());
				g.fillRect(x, y, w, h);
			}
		};
	}
	
	protected JPanel getColorDisplayPanel() {
		return colorDisplayPanel;
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		actChooseColor.setEnabled(enabled);
	}
	
	public void setColorKey(Color colorKey) {
		if (colorKey == null) {
			colorKey = Color.WHITE;
		}
		if ( !this.colorKey.equals(colorKey)) {
			Color oldc = this.colorKey;
			this.colorKey = colorKey;
			repaint();
			firePropertyChange("colorKey", oldc, colorKey);
		}
	}
	
    /**
     * Adds an <code>ActionListener</code> to the button.
     * @param l the <code>ActionListener</code> to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }
    
    /**
     * Removes an <code>ActionListener</code> from the button.
     * If the listener is the currently set <code>Action</code>
     * for the button, then the <code>Action</code>
     * is set to <code>null</code>.
     *
     * @param l the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
	    listenerList.remove(ActionListener.class, l);
    }	
	
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the <code>event</code> 
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see EventListenerList
     */
    protected void fireActionPerformed(ActionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                      String actionCommand = event.getActionCommand();
                      if(actionCommand == null) {
                         actionCommand = getActionCommand();
                      }
                      e = new ActionEvent(this,
                                          ActionEvent.ACTION_PERFORMED,
                                          actionCommand,
                                          event.getWhen(),
                                          event.getModifiers());
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }          
        }
    }
    
    public void setActionCommand(String actionCommand) {
		this.actionCommand = actionCommand;
	}
    
    public String getActionCommand() {
		return actionCommand;
	}

    public Color getColorKey() {
		return colorKey;
	}
    
    protected String getColorDialogTitle() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(STRINGS_RESOURCE);
		return strings.getProperty("caption.chooseColorKey");
	}
	
	protected void onChooseColor(ActionEvent e) {

		Color colorKey = getColorKey();
		colorKey = JColorChooser.showDialog(
				this,
				getColorDialogTitle(),
				colorKey);
		if (colorKey != null) {
			setColorKey(colorKey);
			fireActionPerformed(e);
		}
	}
}