package org.quiltmc.enigma.gui.util;

import java.util.List;

public final class ListUtil {
	private ListUtil() { }

	public static <T, L extends List<T>> L prepend(T first, L list) {
		list.add(0, first);

		return list;
	}
}
