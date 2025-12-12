package org.quiltmc.enigma.util;

import com.github.javaparser.Position;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LineIndexerTest {
	private static final String SUBJECT =
			"""
			I
			II
			III
			IV
			V\
			""";

	private static final ImmutableList<Integer> START_INDEX_EXPECTATIONS = ImmutableList.of(0, 2, 5, 9, 12, -1);

	private static LineIndexer createIndexer() {
		return new LineIndexer(SUBJECT);
	}

	@Test
	void testGetStartIndex() {
		final LineIndexer indexer = createIndexer();

		for (int line = 0; line < START_INDEX_EXPECTATIONS.size(); line++) {
			assertEquals(START_INDEX_EXPECTATIONS.get(line), indexer.getStartIndex(line));
		}
	}

	// test backwards to make it find all start indexes first, then ensure it correctly uses cached results
	@Test
	void testGetStartIndexCaching() {
		final LineIndexer indexer = createIndexer();

		for (int line = START_INDEX_EXPECTATIONS.size() - 1; line >= 0; line--) {
			assertEquals(START_INDEX_EXPECTATIONS.get(line), indexer.getStartIndex(line));
		}
	}

	@Test
	void testGetIndex() {
		final LineIndexer indexer = createIndexer();

		final Position firstLine = new Position(Position.FIRST_LINE, Position.FIRST_COLUMN);
		final Position secondLine = firstLine.nextLine();
		final Position thirdLine = secondLine.nextLine();
		final Position fourthLine = thirdLine.nextLine();
		final Position fifthLine = fourthLine.nextLine();

		final List<Position> positionsByExpectedIndex = List.of(
				// I
				firstLine,
				firstLine.right(1),
				// II
				secondLine,
				secondLine.right(1),
				secondLine.right(2),
				// III
				thirdLine,
				thirdLine.right(1),
				thirdLine.right(2),
				thirdLine.right(3),
				// IV
				fourthLine,
				fourthLine.right(1),
				fourthLine.right(2),
				// V
				fifthLine
				// no character after V, so no right(1) expected
		);

		for (int expectedIndex = 0; expectedIndex < positionsByExpectedIndex.size(); expectedIndex++) {
			final Position pos = positionsByExpectedIndex.get(expectedIndex);
			final int index = indexer.getIndex(pos);

			final int finalExpectedIndex = expectedIndex;
			assertEquals(expectedIndex, index, () ->
					"expected pos [%s, %s] to have index %s, but had index %s!"
						.formatted(pos.line, pos.column, finalExpectedIndex, index)
			);
		}

		assertEquals(-1, indexer.getIndex(fifthLine.right(1)));
		assertEquals(-1, indexer.getIndex(fifthLine.nextLine()));
	}

	@Test
	void testGetLine() {
		final LineIndexer indexer = createIndexer();

		for (int expectedLine = 0; ; expectedLine++) {
			final int lineStartIndex = START_INDEX_EXPECTATIONS.get(expectedLine);
			final int nextLineStartIndex = START_INDEX_EXPECTATIONS.get(expectedLine + 1);

			final boolean lastLine = nextLineStartIndex < 0;

			final int lineEndIndex = lastLine ? SUBJECT.length() : nextLineStartIndex;
			for (int index = lineStartIndex; index < lineEndIndex; index++) {
				final int line = indexer.getLine(index);

				assertEquals(expectedLine, line, unexpectedLineMessageFactoryOf(index, expectedLine, line));
			}

			if (lastLine) {
				break;
			}
		}

		assertEquals(-1, indexer.getLine(SUBJECT.length()));
	}

	@Test
	void testGetLineAtStartOfLine() {
		final LineIndexer indexer = createIndexer();

		final int index = START_INDEX_EXPECTATIONS.get(1);
		final int line = indexer.getLine(index);
		final int expectedLine = 1;
		assertEquals(expectedLine, line, unexpectedLineMessageFactoryOf(index, expectedLine, line));
	}

	private static Supplier<String> unexpectedLineMessageFactoryOf(int index, int expectedLine, int line) {
		return () -> "expected index %s to have line %s, but had line %s!"
			.formatted(index, expectedLine, line);
	}
}
