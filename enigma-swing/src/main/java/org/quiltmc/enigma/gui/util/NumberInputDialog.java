package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Result;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.function.Function;

import static javax.swing.BorderFactory.createEmptyBorder;

public class NumberInputDialog<N extends Number> extends JDialog {
	private static final int INSET = 4;

	/**
	 * Only returns {@code null} if the passed {@code initialValue} is {@code null} and valid input was not submitted.
	 *
	 *  <p>
	 * TODO
	 *
	 * @param owner
	 * @param initialValue
	 * @param min
	 * @param max
	 * @param title
	 * @param message
	 * @return
	 */
	public static Integer promptInt(
			@Nullable Frame owner, @Nullable Integer initialValue, int min, int max,
			String title, String message, String submit
	) {
		validateBoundedArgs(initialValue, min, max);

		final var field = new NumberTextField<>(
				initialValue,
				createBoundedParser(min, max, Integer::parseInt, I18n.translate("prompt.number.not_whole"))
		);

		final var dialog = new NumberInputDialog<>(
				owner, field,
				title, message, I18n.translate("prompt.cancel"), submit
		);
		dialog.setFont(ScaleUtil.scaleFont(dialog.getFont()));

		dialog.pack();
		dialog.setLocationRelativeTo(owner);

		dialog.setVisible(true);
		dialog.dispose();

		return dialog.field.getValue();
	}

	// TODO promptFloat

	private static <N extends Number & Comparable<N>> void validateBoundedArgs(@Nullable N initialValue, N min, N max) {
		if (min.compareTo(max) >= 0) {
			throw new IllegalArgumentException("min (%s) must be strictly less than max (%s)!".formatted(min, max));
		}

		if (initialValue != null & !isInRange(initialValue, min, max)) {
			throw new IllegalArgumentException(
				"initialValue (%s) is out of range [%s, %s]!".formatted(initialValue, min, max)
			);
		}
	}

	private static <N extends Number & Comparable<N>> Function<String, Result<N, String>> createBoundedParser(
			N min, N max, Function<String, N> baseParser, String formatErrorMessage
	) {
		return input -> {
			try {
				final N parsed = baseParser.apply(input);
				if (isInRange(parsed, min, max)) {
					return Result.ok(parsed);
				} else {
					return Result.err(I18n.translateFormatted("prompt.number.not_in_range", min, max));
				}
			} catch (NumberFormatException e) {
				return Result.err(formatErrorMessage);
			}
		};
	}

	private static <N extends Number & Comparable<N>> boolean isInRange(N value, N min, N max) {
		return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
	}

	protected final JTextArea messageText;
	protected final NumberTextField<N> field;
	protected final JButton stepUp = new JButton();
	protected final JButton stepDown = new JButton();
	protected final JButton cancel;
	protected final JButton submit;

	protected NumberInputDialog(
			@Nullable Frame owner, NumberTextField<N> field,
			String title, String message, String cancel, String submit
	) {
		super(owner, title, true);

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setLayout(new GridBagLayout());

		this.messageText = new JTextArea();
		this.messageText.setText(message);
		this.messageText.setEditable(false);
		this.messageText.setBorder(createEmptyBorder());

		this.field = field;

		// TODO step button actions
		this.stepUp.setIcon(GuiUtil.getUpChevron());
		this.stepDown.setIcon(GuiUtil.getDownChevron());

		this.cancel = new JButton(cancel);
		this.cancel.addActionListener(e -> this.setVisible(false));

		this.submit = new JButton(submit);
		this.submit.addActionListener(e -> {
			this.field.tryCommit();

			this.setVisible(false);
		});
		this.submit.setEnabled(false);

		// TODO display validation feedback
		this.field.addValidListener(this.submit::setEnabled);

		final GridBagConstraintsBuilder baseBuilder = GridBagConstraintsBuilder.create();
		final GridBagConstraintsBuilder insetBuilder = baseBuilder.insets(INSET);
		int y = 0;

		this.add(this.messageText, insetBuilder.pos(0, y++).build());

		final JPanel fieldRow = new JPanel(new GridBagLayout());
		fieldRow.add(this.field, baseBuilder
				.insets(0, INSET, 0, 0)
				.pos(0, 0)
				.height(2)
				.weight(1, 1)
				.fill(GridBagConstraints.BOTH)
				.build()
		);
		fieldRow.add(this.stepUp, baseBuilder.insets(0, 0, INSET, 0).pos(1, 0).build());
		fieldRow.add(this.stepDown, baseBuilder.pos(1, 1).build());
		this.add(fieldRow, insetBuilder.pos(0, y++).fill(GridBagConstraints.HORIZONTAL).build());

		final JPanel buttonRow = new JPanel(new GridBagLayout());
		buttonRow.add(this.cancel, baseBuilder.insets(0, INSET, 0, 0).pos(0, 0).build());
		buttonRow.add(this.submit, baseBuilder.pos(1, 0).build());
		this.add(buttonRow, insetBuilder.pos(0, y++).anchor(GridBagConstraints.LINE_END).build());
	}
}
