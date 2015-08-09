package charactermanaj.model;

import javax.swing.event.EventListenerList;


/**
 * キャラクターデータが変更されたことを通知するためのメカニズム
 * 
 * @author seraphy
 * 
 */
public abstract class CharacterDataChangeObserver {

	private static CharacterDataChangeObserver inst = new CharacterDataChangeObserverImpl();

	public static CharacterDataChangeObserver getDefault() {
		return inst;
	}

	public abstract void addCharacterDataChangeListener(
			CharacterDataChangeListener l);

	public abstract void removeCharacterDataChangeListener(
			CharacterDataChangeListener l);

	public abstract void notifyCharacterDataChange(CharacterDataChangeEvent e);

	public void notifyCharacterDataChange(Object wnd, CharacterData cd,
			boolean changeStructure, boolean reloadPartsAndFavorites) {
		if (cd == null) {
			throw new IllegalArgumentException();
		}
		notifyCharacterDataChange(new CharacterDataChangeEvent(wnd, cd,
				changeStructure, reloadPartsAndFavorites));
	}
}

class CharacterDataChangeObserverImpl extends CharacterDataChangeObserver {

	private EventListenerList listeners = new EventListenerList();

	@Override
	public void addCharacterDataChangeListener(CharacterDataChangeListener l) {
		listeners.add(CharacterDataChangeListener.class, l);
	}

	@Override
	public void removeCharacterDataChangeListener(CharacterDataChangeListener l) {
		listeners.remove(CharacterDataChangeListener.class, l);
	}

	@Override
	public void notifyCharacterDataChange(CharacterDataChangeEvent e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		CharacterDataChangeListener[] lst = listeners
				.getListeners(CharacterDataChangeListener.class);
		for (CharacterDataChangeListener l : lst) {
			l.notifyChangeCharacterData(e);
		}
	}
}

