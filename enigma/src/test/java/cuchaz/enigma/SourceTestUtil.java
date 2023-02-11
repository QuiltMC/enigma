package cuchaz.enigma;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceRemapper;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.representation.entry.Entry;

public class SourceTestUtil {
	/**
	 * Convert the provided {@code source} to HTML, which can be written to a file and/or viewed in a browser,
	 * for easier debugging of decompiling-related issues.
	 */
	public static String toHtml(Source source, String name) {
		return "<html><head><meta charset=\"UTF-8\"/>" + "<title>" + name + ".java</title>" +
				"<style>* { font-family: monospace; }.ref { background-color: khaki; border: 1px solid; }</style></meta><body>" +
				"<h3>" + name + ".java</h3>" +
				"<pre>" + insertTokenHtmlData(source) + "</pre>" +
				"</body></html>";
	}

	private static String insertTokenHtmlData(Source source) {
		String text = source.asString();
		SourceIndex index = source.index();
		SourceRemapper remapper = new SourceRemapper(text, index.referenceTokens());
		SourceRemapper.Result result = remapper.remap((token, movedToken) -> remapTokenHtml(index, token));
		return result.getSource();
	}

	private static String remapTokenHtml(SourceIndex index, Token token) {
		EntryReference<Entry<?>, Entry<?>> ref = index.getReference(token);
		if (ref != null) {
			return "<span class=\"ref\" title=\"" + ref + "\">" + token.text + "</span>";
		}

		return null;
	}
}
