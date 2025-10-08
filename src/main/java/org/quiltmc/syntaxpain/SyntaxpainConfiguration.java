package org.quiltmc.syntaxpain;

import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Function;

@SuppressWarnings("unused")
public class SyntaxpainConfiguration {
	private static Color highlightColor = new Color(0x3333EE);
	private static Color stringColor = new Color(0xCC6600);
	private static Color numberColor = new Color(0x999933);
	private static Color operatorColor = new Color(0x000000);
	private static Color delimiterColor = new Color(0x000000);
	private static Color typeColor = new Color(0x000000);
	private static Color identifierColor = new Color(0x000000);
	private static Color commentColour = new Color(0x339933);
	private static Color textColor = new Color(0x000000);
	private static Color regexColor = new Color(0xcc6600);

	private static Color lineRulerPrimaryColor = new Color(0x333300);
	private static Color lineRulerSecondaryColor = new Color(0xEEEEFF);
	private static Color lineRulerSelectionColor = new Color(0xCCCCEE);

	private static Font editorFont = Font.decode(Font.DIALOG);
	private static Function<JTextComponent, QuickFindDialog> quickFindDialogFactory = QuickFindDialog::new;

	public static Color getHighlightColor() {
		return highlightColor;
	}

	public static void setHighlightColor(Color highlightColor) {
		SyntaxpainConfiguration.highlightColor = highlightColor;
	}

	public static Color getStringColor() {
		return stringColor;
	}

	public static void setStringColor(Color stringColor) {
		SyntaxpainConfiguration.stringColor = stringColor;
	}

	public static Color getNumberColor() {
		return numberColor;
	}

	public static void setNumberColor(Color numberColor) {
		SyntaxpainConfiguration.numberColor = numberColor;
	}

	public static Color getOperatorColor() {
		return operatorColor;
	}

	public static void setOperatorColor(Color operatorColor) {
		SyntaxpainConfiguration.operatorColor = operatorColor;
	}

	public static Color getDelimiterColor() {
		return delimiterColor;
	}

	public static void setDelimiterColor(Color delimiterColor) {
		SyntaxpainConfiguration.delimiterColor = delimiterColor;
	}

	public static Color getTypeColor() {
		return typeColor;
	}

	public static void setTypeColor(Color typeColor) {
		SyntaxpainConfiguration.typeColor = typeColor;
	}

	public static Color getIdentifierColor() {
		return identifierColor;
	}

	public static void setIdentifierColor(Color identifierColor) {
		SyntaxpainConfiguration.identifierColor = identifierColor;
	}

	public static Color getCommentColour() {
		return commentColour;
	}

	public static void setCommentColour(Color commentColour) {
		SyntaxpainConfiguration.commentColour = commentColour;
	}

	public static Color getTextColor() {
		return textColor;
	}

	public static void setTextColor(Color textColor) {
		SyntaxpainConfiguration.textColor = textColor;
	}

	public static Color getRegexColor() {
		return regexColor;
	}

	public static void setRegexColor(Color regexColor) {
		SyntaxpainConfiguration.regexColor = regexColor;
	}

	public static Color getLineRulerPrimaryColor() {
		return lineRulerPrimaryColor;
	}

	public static void setLineRulerPrimaryColor(Color lineRulerPrimaryColor) {
		SyntaxpainConfiguration.lineRulerPrimaryColor = lineRulerPrimaryColor;
	}

	public static Color getLineRulerSecondaryColor() {
		return lineRulerSecondaryColor;
	}

	public static void setLineRulerSecondaryColor(Color lineRulerSecondaryColor) {
		SyntaxpainConfiguration.lineRulerSecondaryColor = lineRulerSecondaryColor;
	}

	public static Color getLineRulerSelectionColor() {
		return lineRulerSelectionColor;
	}

	public static void setLineRulerSelectionColor(Color lineRulerSelectionColor) {
		SyntaxpainConfiguration.lineRulerSelectionColor = lineRulerSelectionColor;
	}

	public static Font getEditorFont() {
		return editorFont;
	}

	public static void setEditorFont(Font font) {
		editorFont = font;
		JavaSyntaxKit.setFont(font);
	}

	/**
	 * @return whether automatic installation of {@link QuickFindDialog}s in {@link JEditorPane}s is enabled
	 */
	public static boolean isQuickFindDialogEnabled() {
		return quickFindDialogFactory != null;
	}

	/**
	 * @return a new dialog, or {@code null} if {@linkplain  #isQuickFindDialogEnabled dialogs are disabled}
	 */
	public static QuickFindDialog getQuickFindDialog(JTextComponent component) {
		return quickFindDialogFactory == null ? null : quickFindDialogFactory.apply(component);
	}

	/**
	 * Set's the factory method used by {@link #getQuickFindDialog(JTextComponent)} to create new dialogs.
	 *
	 * <p> Pass {@code null} to disable automatic installation of {@link QuickFindDialog}s in {@link JEditorPane}s.
	 *
	 * @param dialogFactory the dialog factory; may be {@code null}, but a factory must not return {@code null}
	 */
	public static void setQuickFindDialogFactory(Function<JTextComponent, QuickFindDialog> dialogFactory) {
		quickFindDialogFactory = dialogFactory;
	}
}
