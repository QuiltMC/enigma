/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cuchaz.enigma.gui.syntax;

import javax.swing.text.JTextComponent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Data that is shared by Find / Replace and Find Next actions for a Document
 * The data here will be added as a property of the Document using the key
 * PROPERTY_KEY.  Only through the getFtmEditor can you crate a new instance.
 *
 * <p>
 * The class is responsible for handling the doFind and doReplace all actions.
 *
 * <p>
 * The class is also responsible for displaying the Find / Replace dialog
 *
 * @author Ayman Al-Sairafi
 */
public class DocumentSearchData {
	private static final String PROPERTY_KEY = "SearchData";
	private Pattern pattern = null;
	private boolean wrap = true;

	/**
	 * This prevents creating a new instance.  You must call the getFromEditor
	 * to crate a new instance attached to a Document
	 *
	 */
	private DocumentSearchData() {
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	/**
	 * Sets the pattern to the given compiled pattern.
	 *
	 * @see DocumentSearchData#setPattern(String, boolean, boolean)
	 */
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	/**
	 * Sets the pattern from a string and flags
	 * @param pat String of pattern
	 * @param regex true if the pattern should be a regexp
	 * @param ignoreCase true to ignore case
	 */
	public void setPattern(String pat, boolean regex, boolean ignoreCase)
		throws PatternSyntaxException {
		if (pat != null && pat.length() > 0) {
			int flag = (regex) ? 0 : Pattern.LITERAL;
			flag |= (ignoreCase) ? Pattern.CASE_INSENSITIVE : 0;
			this.setPattern(Pattern.compile(pat, flag));
		} else {
			this.setPattern(null);
		}
	}

	public boolean isWrap() {
		return this.wrap;
	}

	public void setWrap(boolean wrap) {
		this.wrap = wrap;
	}

	/**
	 * Gets the Search data from a Document.  If document does not have any
	 * search data, then a new instance is added, put and reurned.
	 * @param target JTextCOmponent we are attaching to
	 */
	public static DocumentSearchData getFromEditor(JTextComponent target) {
		if (target == null) {
			return null;
		}

		Object o = target.getDocument().getProperty(PROPERTY_KEY);
		if (o instanceof DocumentSearchData documentSearchData) {
			return documentSearchData;
		} else {
			DocumentSearchData newDSD = new DocumentSearchData();
			target.getDocument().putProperty(PROPERTY_KEY, newDSD);
			return newDSD;
		}
	}

	/**
	 * Finds the previous match
	 */
	public boolean doFindPrev(JTextComponent target) {
		if (this.getPattern() == null) {
			return false;
		}

		SyntaxDocument sDoc = ActionUtils.getSyntaxDocument(target);
		if (sDoc == null) {
			return false;
		}

		int dot = target.getSelectionStart();
		Matcher matcher = sDoc.getMatcher(this.getPattern());
		if (matcher == null) {
			return false;
		}

		// we have no way of jumping to last match, so we need to
		// go throw all matches, and stop when we reach current pos
		int start = -1;
		int end = -1;
		while (matcher.find()) {
			if (matcher.end() >= dot) {
				break;
			}

			start = matcher.start();
			end = matcher.end();
		}

		if (end > 0) {
			target.select(start, end);
			return true;
		} else {
			return false;
		}
	}

	public boolean doFindNext(JTextComponent target) {
		if (this.getPattern() == null) {
			return false;
		}

		SyntaxDocument sDoc = ActionUtils.getSyntaxDocument(target);
		if (sDoc == null) {
			return false;
		}

		int start = target.getSelectionEnd();
		if (target.getSelectionEnd() == target.getSelectionStart()) {
			// we must advance the position by one, otherwise we will find
			// the same text again
			start++;
		}

		if (start >= sDoc.getLength()) {
			start = sDoc.getLength();
		}

		Matcher matcher = sDoc.getMatcher(this.getPattern(), start);
		if (matcher != null && matcher.find()) {
			// since we used an offset in the matcher, the matcher location
			// MUST be offset by that location
			target.select(matcher.start() + start, matcher.end() + start);
			return true;
		} else {
			if (this.isWrap()) {
				matcher = sDoc.getMatcher(this.getPattern());
				if (matcher != null && matcher.find()) {
					target.select(matcher.start(), matcher.end());
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}
}
