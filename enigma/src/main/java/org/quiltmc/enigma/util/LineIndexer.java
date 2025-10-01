package org.quiltmc.enigma.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineIndexer {
	private static final Pattern LINE_END = Pattern.compile("\\r\\n?|\\n");

	private final List<Integer> indexesByLine = new ArrayList<>();
	private final Matcher lineEndMatcher;

	public LineIndexer(String string) {
		// the first line always starts at 0
		this.indexesByLine.add(0);
		this.lineEndMatcher = LINE_END.matcher(string);
	}

	public int getIndex(int line) {
		while (line >= this.indexesByLine.size() && this.lineEndMatcher.find()) {
			this.indexesByLine.add(this.lineEndMatcher.end());
		}

		return line < this.indexesByLine.size() ? this.indexesByLine.get(line) : -1;
	}
}
