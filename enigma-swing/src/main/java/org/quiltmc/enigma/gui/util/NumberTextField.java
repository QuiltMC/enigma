package org.quiltmc.enigma.gui.util;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.util.Result;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createLineBorder;

/**
 * A text field that only allows number input. Holds a value and an edit result.
 *
 * <p> The value is only updated when {@link #tryCommit} is called while the edit result is valid.
 *
 * @param <N> the type of number the field holds
 * @param <E> the type of error held by invalid edit results
 *
 * @see #getValue()
 * @see #getEditOrValue()
 * @see #edit(Number)
 * @see #tryCommit()
 * @see #addEditListener(Consumer)
 */
public class NumberTextField<N extends Number, E> extends JTextField {
	private static final int INDICATOR_THICKNESS = 1;
	private static final int BORDER_PAD = 1;

	private static final Border INVALID_BORDER =
			createLineBorder(Config.getCurrentSyntaxPaneColors().error.value(), INDICATOR_THICKNESS);
	private static final Border VALID_BORDER =
			createEmptyBorder(INDICATOR_THICKNESS, INDICATOR_THICKNESS, INDICATOR_THICKNESS, INDICATOR_THICKNESS);
	private static final Border PAD_BORDER = createEmptyBorder(BORDER_PAD, BORDER_PAD, BORDER_PAD, BORDER_PAD);
	private static final Border INNER_BORDER = Config.currentTheme().createBorder();

	private final Function<String, Result<N, E>> parse;

	private N value;
	@Nullable
	private Result<N, E> editResult;

	private final Set<Consumer<Result<N, E>>> editListeners = new HashSet<>();

	/**
	 * @param initialValue the initial value
	 * @param parse        the method used to parse user input; its results are passed to
	 *                     {@linkplain #addEditListener(Consumer) listeners}
	 *
	 * @see #addEditListener(Consumer)
	 */
	public NumberTextField(@Nullable N initialValue, Function<String, Result<N, E>> parse) {
		this.parse = parse;
		this.value = initialValue;

		if (initialValue != null) {
			this.setText(initialValue.toString());
		}

		this.setBorder(true);

		this.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				NumberTextField.this.onEdit(true);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				// don't play error noise when deleting
				// also setText always removes text first and fires removeUpdate with empty text
				NumberTextField.this.onEdit(false);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				NumberTextField.this.onEdit(true);
			}
		});
	}

	/**
	 * If the most recent edit was valid, commits it to the held value.
	 */
	public void tryCommit() {
		if (this.editResult != null) {
			this.editResult.ok().ifPresent(edited -> this.value = edited);

			this.editResult = null;
		}
	}

	/**
	 * Gets the currently committed value.
	 *
	 * <p> The value will only be {@code null} if {@code null} was passed as the {@code initialValue}
	 * and a valid edit hasn't since been {@linkplain #tryCommit() commited}.
	 *
	 * @return the current value
	 *
	 * @see #getEditOrValue()
	 * @see #edit(Number)
	 * @see #tryCommit()
	 */
	public N getValue() {
		return this.value;
	}

	/**
	 * Gets the currently edited value if it's valid, or the currently committed value otherwise.
	 *
	 * <p> Only returns {@code null} if {@code null} was passed as the {@code initialValue}
	 * and there's been no valid edit.
	 *
	 * @return the result of the most recent edit if valid, or the current value otherwise
	 *
	 * @see #getValue()
	 * @see #edit(Number)
	 * @see #tryCommit()
	 */
	public N getEditOrValue() {
		return Optional.ofNullable(this.editResult).flatMap(result -> result.ok()).orElse(this.value);
	}

	/**
	 * Sets a (valid) edit result and updates the user-facing text.<br>
	 * Does <em>not</em> set the held value; use {@link #tryCommit()} afterward to set the value.
	 *
	 * <p> Since the passed {@code editValue} is not {@linkplain #parse parsed},
	 * any validation performed during parsing is bypassed.
	 *
	 * @param editValue the edited value; must <em>not</em> be {@code null}
	 *
	 * @see #tryCommit()
	 * @see #getEditOrValue()
	 */
	public void edit(N editValue) {
		this.editResult = Result.ok(editValue);
		this.setBorder(true);
		this.setText(editValue.toString());

		this.editListeners.forEach(listener -> listener.accept(this.editResult));
	}

	/**
	 * Adds a listener to be invoked when a field's edited value changes. It receives the new {@link #editResult}.
	 *
	 * @see #removeEditListener(Consumer)
	 */
	public void addEditListener(Consumer<Result<N, E>> listener) {
		this.editListeners.add(listener);
	}

	/**
	 * @see #addEditListener(Consumer)
	 */
	public void removeEditListener(Consumer<Result<N, E>> listener) {
		this.editListeners.remove(listener);
	}

	/**
	 * {@linkplain #parse Parses} this field's current {@linkplain #getText() text} and assigns the result to
	 * {@link #editResult}.
	 *
	 * <p> Also invokes {@link #editListeners}.
	 */
	private void onEdit(boolean invalidFeedback) {
		final boolean hadResult;
		final boolean wasOk;
		if (this.editResult == null) {
			hadResult = false;
			wasOk = false;
		} else {
			hadResult = true;
			wasOk = this.editResult.isOk();
		}

		this.editResult = this.parse.apply(this.getText().trim());

		final boolean ok = this.editResult.isOk();

		if (invalidFeedback && !ok) {
			UIManager.getLookAndFeel().provideErrorFeedback(this);
		}

		if (!hadResult || wasOk != ok) {
			this.setBorder(ok);
		}

		this.editListeners.forEach(listener -> listener.accept(this.editResult));
	}

	private void setBorder(boolean valid) {
		final Border indicatorBorder = valid ? VALID_BORDER : INVALID_BORDER;

		this.setBorder(createCompoundBorder(createCompoundBorder(indicatorBorder, PAD_BORDER), INNER_BORDER));
	}
}
