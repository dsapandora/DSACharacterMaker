package charactermanaj.ui.model;

import javax.swing.event.EventListenerList;

import charactermanaj.model.CharacterData;


/**
 * お気に入りが変更されたことを通知するためのメカニズム.<br>
 * 
 * @author seraphy
 * 
 */
public abstract class FavoritesChangeObserver {

	private static FavoritesChangeObserver defobj = new FavoritesChangeObserverImpl();

	public static FavoritesChangeObserver getDefault() {
		return defobj;
	}

	public abstract void addFavoritesChangeListener(FavoritesChangeListener l);

	public abstract void removeFavoritesChangeListener(FavoritesChangeListener l);

	public abstract void notifyFavoritesChange(FavoritesChangeEvent e);

	public void notifyFavoritesChange(Object wnd, CharacterData cd) {
		if (cd == null) {
			throw new IllegalArgumentException();
		}
		notifyFavoritesChange(new FavoritesChangeEvent(wnd, cd));
	}
}

class FavoritesChangeObserverImpl extends FavoritesChangeObserver {

	private EventListenerList listeners = new EventListenerList();

	@Override
	public void addFavoritesChangeListener(FavoritesChangeListener l) {
		listeners.add(FavoritesChangeListener.class, l);
	}

	@Override
	public void removeFavoritesChangeListener(FavoritesChangeListener l) {
		listeners.remove(FavoritesChangeListener.class, l);
	}

	@Override
	public void notifyFavoritesChange(FavoritesChangeEvent e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		FavoritesChangeListener[] lst = listeners
				.getListeners(FavoritesChangeListener.class);
		for (FavoritesChangeListener l : lst) {
			l.notifyChangeFavorites(e);
		}
	}
}
