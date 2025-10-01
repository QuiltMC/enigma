package org.quiltmc.enigma.util;

import com.github.javaparser.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineIndexer {
	private static final Pattern LINE_END = Pattern.compile("\\r\\n?|\\n");

	private final List<Integer> indexesByLine = new ArrayList<>();
	private final Matcher lineEndMatcher;
	private final String string;

	public LineIndexer(String string) {
		// the first line always starts at 0
		this.indexesByLine.add(0);
		this.string = string;
		this.lineEndMatcher = LINE_END.matcher(this.string);
	}

	public String getString() {
		return this.string;
	}

	public int getStartIndex(int line) {
		while (line >= this.indexesByLine.size() && this.lineEndMatcher.find()) {
			this.indexesByLine.add(this.lineEndMatcher.end());
		}

		return line < this.indexesByLine.size() ? this.indexesByLine.get(line) : -1;
	}

	public int getIndex(Position position) {
		final int lineIndex = this.getStartIndex(position.line - Position.FIRST_LINE);
		return lineIndex < 0 ? lineIndex : lineIndex + position.column - Position.FIRST_COLUMN;
	}
}
