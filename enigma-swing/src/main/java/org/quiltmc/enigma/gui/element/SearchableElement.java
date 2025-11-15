package org.quiltmc.enigma.gui.element;

import javax.swing.MenuElement;
import java.util.stream.Stream;

public interface SearchableElement extends MenuElement {
	Stream<String> streamSearchAliases();

	void onSearchClicked();
}
