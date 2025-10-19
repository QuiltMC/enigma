package org.quiltmc.enigma.util;

import com.github.javaparser.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineIndexer {
	public static final Pattern LINE_END = Pattern.compile("\\r\\n?|\\n");
	public static final Comparator<Integer> INT_COMPARATOR = Comparator.comparingInt(Integer::intValue);

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
		if (lineIndex < 0) {
			return lineIndex;
		} else {
			final int index = lineIndex + position.column - Position.FIRST_COLUMN;

			return index < this.string.length() ? index : -1;
		}
	}

	public int getLine(int index) {
		if (this.indexesByLine.get(this.indexesByLine.size() - 1) >= index) {
			final int found = Collections.binarySearch(this.indexesByLine, index, INT_COMPARATOR);

			// -found - 2 because binarySearch returns -(insertion point) - 1 when not found;
			// subtract 1 to undo their 1 and subtract another 1 to get the preceding line
			return found >= 0 ? found : -found - 2;
		} else {
			while (this.lineEndMatcher.find()) {
				final int lineStart = this.lineEndMatcher.end();
				this.indexesByLine.add(lineStart);

				if (lineStart >= index) {
					// -2 to get the preceding line
					return this.indexesByLine.size() - 2;
				}
			}
		}

		return -1;
	}
}
