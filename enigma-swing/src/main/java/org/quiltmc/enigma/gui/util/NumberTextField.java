package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.util.Result;

import javax.annotation.Nullable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createLineBorder;

public class NumberTextField<N extends Number> extends JTextField {
	private final Function<String, Result<N, String>> parser;

	private N value;
	@Nullable
	private Result<N, String> editResult;

	private final Set<ValidListener> validListeners = new HashSet<>();

	public NumberTextField(@Nullable N initialValue, Function<String, Result<N, String>> parser) {
		this.parser = parser;
		this.value = initialValue;

		if (initialValue != null) {
			this.setText(initialValue.toString());
		}

		this.setBorder(true);

		this.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				NumberTextField.this.onEdit();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				NumberTextField.this.onEdit();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				NumberTextField.this.onEdit();
			}
		});
	}

	public Result<N, String> tryCommit() {
		if (this.editResult == null) {
			return Result.ok(this.value);
		} else {
			this.editResult.ok().ifPresent(edited -> this.value = edited);

			final var result = this.editResult;
			this.editResult = null;
			return result;
		}
	}

	/**
	 * Returns the current value.
	 *
	 * <p> The current value may not match the edited value if editing has occurred since the last
	 * {@linkplain #tryCommit() commit} or if the edited value is invalid.
	 *
	 * <p> The value will only be {@code null} if this field was created with a {@code null} {@code initialValue} and
	 * a valid edit hasn't been {@linkplain #tryCommit() commited}.
	 */
	public N getValue() {
		return this.value;
	}

	public void addValidListener(ValidListener listener) {
		this.validListeners.add(listener);
	}

	public void removeValidListener(ValidListener listener) {
		this.validListeners.remove(listener);
	}

	/**
	 * {@linkplain #parser Parses} this field's current {@linkplain #getText() text} and assigns the result to
	 * {@link #editResult}.
	 *
	 * <p> Also invokes {@link #validListeners} when the validity of {@link #editResult} changes.
	 */
	private void onEdit() {
		final boolean hadResult;
		final boolean wasOk;
		if (this.editResult == null) {
			hadResult = false;
			wasOk = false;
		} else {
			hadResult = true;
			wasOk = this.editResult.isOk();
		}

		this.editResult = this.parser.apply(this.getText().trim());

		final boolean ok = this.editResult.isOk();
		if (!hadResult || wasOk != ok) {
			this.setBorder(ok);
			this.validListeners.forEach(listener -> listener.listen(ok));
		}
	}

	private void setBorder(boolean valid) {
		final Color indicatorColor = valid ? new Color(0, 0, 0, 0) : Color.RED;
		final Border indicatorBorder = createLineBorder(indicatorColor);
		final Border padBorder = createEmptyBorder(1, 1, 1, 1);
		final Border innerBorder = Config.currentTheme().createBorder();

		this.setBorder(createCompoundBorder(createCompoundBorder(indicatorBorder, padBorder), innerBorder));
	}

	/**
	 * A listener which is invoked when a field's edited value's validity changes. It receives the new validity.
	 */
	@FunctionalInterface
	public interface ValidListener {
		void listen(boolean valid);
	}
}
