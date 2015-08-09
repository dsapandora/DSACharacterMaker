package charactermanaj.ui.model;

import java.util.EventListener;

public interface FavoritesChangeListener extends EventListener {

	void notifyChangeFavorites(FavoritesChangeEvent e);

}
