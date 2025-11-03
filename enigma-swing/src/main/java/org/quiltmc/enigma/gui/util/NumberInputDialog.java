package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Result;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static javax.swing.BorderFactory.createEmptyBorder;

public class NumberInputDialog<N extends Number & Comparable<N>> extends JDialog {
	private static final int INSET = 4;

	/**
	 * Only returns {@code null} if the passed {@code initialValue} is {@code null} and valid input was not submitted.
	 *
	 * <p> TODO
	 *
	 * @param owner        TODO
	 * @param initialValue TODO
	 * @param min          TODO
	 * @param max          TODO
	 * @param step         TODO
	 * @param title        TODO
	 * @param message      TODO
	 *
	 * @return TODO
	 *
	 * @throws IllegalArgumentException if the passed {@code min} is not strictly less than the passed {@code max}
	 * @throws IllegalArgumentException if the passed {@code initialValue} is non-{@code null} and not between the
	 *                                  passed {@code min} and {@code max}
	 * @throws IllegalArgumentException if the passed {@code step} is not positive
	 */
	public static Integer promptInt(
			@Nullable Frame owner, @Nullable Integer initialValue, int min, int max, int step,
			String title, String message, String submit
	) {
		return promptNumber(
				owner, title, message, submit,
				initialValue, min, max, step, 0,
				Integer::sum, (left, right) -> left - right,
				input -> {
					try {
						return Result.ok(Integer.parseInt(input));
					} catch (NumberFormatException e) {
						return Result.err(I18n.translate("prompt.number.not_whole"));
					}
				}
		);
	}

	/**
	 * Only returns {@code null} if the passed {@code initialValue} is {@code null} and valid input was not submitted.
	 *
	 * <p> TODO
	 *
	 * @param owner        TODO
	 * @param initialValue TODO
	 * @param min          TODO
	 * @param max          TODO
	 * @param step         TODO
	 * @param title        TODO
	 * @param message      TODO
	 *
	 * @return TODO
	 *
	 * @throws IllegalArgumentException if the passed {@code min} is not strictly less than the passed {@code max}
	 * @throws IllegalArgumentException if the passed {@code initialValue} is non-{@code null} and not between the
	 *                                  passed {@code min} and {@code max}
	 * @throws IllegalArgumentException if the passed {@code step} is not positive
	 */
	public static Float promptFloat(
			@Nullable Frame owner, @Nullable Float initialValue, float min, float max, float step,
			String title, String message, String submit
	) {
		return promptNumber(
				owner, title, message, submit,
				initialValue, min, max, step, 0f,
				Float::sum, (left, right) -> left - right,
				input -> {
					try {
						return Result.ok(Float.parseFloat(input));
					} catch (NumberFormatException e) {
						return Result.err(I18n.translate("prompt.number.not_number"));
					}
				}
		);
	}

	private static <N extends Number & Comparable<N>> N promptNumber(
			@Nullable Frame owner, String title, String message, String submit,
			@Nullable N initialValue, N min, N max, N step, N zero,
			BinaryOperator<N> add, BinaryOperator<N> subtract,
			Function<String, Result<N, String>> parse
	) {
		if (min.compareTo(max) >= 0) {
			throw new IllegalArgumentException("min (%s) must be strictly less than max (%s)!".formatted(min, max));
		}

		if (initialValue != null & !isInRange(initialValue, min, max)) {
			throw new IllegalArgumentException(
				"initialValue (%s) is out of range [%s, %s]!".formatted(initialValue, min, max)
			);
		}

		if (zero.compareTo(step) >= 0) {
			throw new IllegalArgumentException("step (%s) must be positive!".formatted(step));
		}

		final var numberDialog = new NumberInputDialog<>(
				owner, title, message, I18n.translate("prompt.cancel"), submit,
				initialValue, min, max,
				value -> add.apply(value, step), value -> subtract.apply(value, step),
				parse
		);

		numberDialog.setFont(ScaleUtil.scaleFont(numberDialog.getFont()));

		numberDialog.pack();
		numberDialog.setLocationRelativeTo(owner);

		numberDialog.setVisible(true);
		numberDialog.dispose();

		return numberDialog.field.getValue();
	}

	private static <N extends Number & Comparable<N>> boolean isInRange(N value, N min, N max) {
		return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
	}

	protected final N min;
	protected final N max;

	protected final JTextArea message = new JTextArea();

	protected final NumberTextField<N> field;
	protected final JButton stepUp = new JButton();
	protected final JButton stepDown = new JButton();

	protected final JTextArea error = new MinimumSizeTextArea();

	protected final JButton cancel = new JButton();
	protected final JButton submit = new JButton();

	protected NumberInputDialog(
			@Nullable Frame owner, String title, String message, String cancel, String submit,
			@Nullable N initialValue, N min, N max,
			UnaryOperator<N> stepUp, UnaryOperator<N> stepDown, Function<String, Result<N, String>> parse
	) {
		super(owner, title, true);

		this.min = min;
		this.max = max;

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setResizable(false);
		this.setLayout(new GridBagLayout());

		this.message.setText(message);
		this.message.setEditable(false);
		this.message.setBorder(createEmptyBorder());

		this.field = new NumberTextField<>(initialValue, input -> {
			return parse.apply(input).andThen(parsed -> {
				if (isInRange(parsed, min, max)) {
					return Result.ok(parsed);
				} else {
					return Result.err(I18n.translateFormatted("prompt.number.not_in_range", min, max));
				}
			});
		});

		this.stepUp.setIcon(GuiUtil.getUpChevron());
		this.stepUp.addActionListener(e -> {
			this.stepDown.setEnabled(true);

			final N stepped = stepUp.apply(this.field.getEditOrValue());
			final N clamped;
			if (stepped.compareTo(max) >= 0) {
				this.stepUp.setEnabled(false);
				clamped = max;
			} else {
				clamped = stepped;
			}

			this.field.edit(clamped);
		});

		this.stepDown.setIcon(GuiUtil.getDownChevron());
		this.stepDown.addActionListener(e -> {
			this.stepUp.setEnabled(true);

			final N stepped = stepDown.apply(this.field.getEditOrValue());
			final N clamped;
			if (min.compareTo(stepped) >= 0) {
				this.stepDown.setEnabled(false);
				clamped = min;
			} else {
				clamped = stepped;
			}

			this.field.edit(clamped);
		});

		this.error.setEditable(false);
		this.error.setLineWrap(false);
		this.hideError();

		this.cancel.setText(cancel);
		this.cancel.addActionListener(e -> this.setVisible(false));

		this.submit.setText(submit);
		this.submit.addActionListener(e -> {
			this.field.tryCommit();

			this.setVisible(false);
		});
		this.submit.setEnabled(false);

		if (initialValue != null) {
			this.updateStepButtons(initialValue);
		}

		this.field.addEditListener(edit -> {
			if (edit.isOk()) {
				this.submit.setEnabled(true);
				this.hideError();
				this.updateStepButtons(edit.unwrap());
			} else {
				this.submit.setEnabled(false);
				this.showError(edit.unwrapErr());
			}
		});

		final GridBagConstraintsBuilder baseBuilder = GridBagConstraintsBuilder.create();
		final GridBagConstraintsBuilder insetBuilder = baseBuilder.insets(INSET);
		int y = 0;

		this.add(this.message, insetBuilder.pos(0, y++).build());

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

		this.add(this.error, insetBuilder.pos(0, y++).build());

		final JPanel buttonRow = new JPanel(new GridBagLayout());
		buttonRow.add(this.cancel, baseBuilder.insets(0, INSET, 0, 0).pos(0, 0).build());
		buttonRow.add(this.submit, baseBuilder.pos(1, 0).build());
		this.add(buttonRow, insetBuilder.pos(0, y++).anchor(GridBagConstraints.LINE_END).build());
	}

	private void updateStepButtons(N value) {
		if (Objects.equals(value, this.max)) {
			this.stepUp.setEnabled(false);
		} else if (Objects.equals(value, this.min)) {
			this.stepDown.setEnabled(false);
		}
	}

	private void showError(String error) {
		this.error.setForeground(Config.getCurrentSyntaxPaneColors().error.value());
		this.error.setText(error);
		this.error.repaint();
	}

	private void hideError() {
		this.error.setForeground(new Color(0, 0, 0, 0));
		// repainting just this.error leaves an artifact of this.field's error border
		this.repaint();
	}

	/**
	 * A text area whose preferred size is at least as large as its font's largest character.
	 */
	private static class MinimumSizeTextArea extends JTextArea {
		@Override
		public Dimension getPreferredSize() {
			final Dimension size = super.getPreferredSize();

			final FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
			final int fontHeight = fontMetrics.getHeight();
			final int maxFontWidth = IntStream.of(fontMetrics.getWidths()).max().orElse(1);

			size.height = Math.max(size.height, fontHeight);
			size.width = Math.max(size.width, maxFontWidth);

			return size;
		}
	}
}
