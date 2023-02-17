package cuchaz.enigma.gui.newabstraction;

import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryValidation {
	public static void validateJavadoc(ValidationContext vc, String javadoc) {
		if (javadoc.contains("*/")) {
			vc.raise(Message.ILLEGAL_DOC_COMMENT_END);
		}
	}
}
