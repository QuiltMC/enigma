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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import static javax.swing.BorderFactory.createEmptyBorder;

public class NumberInputDialog<N extends Number, D extends NumberInputDialog<N, D>> extends JDialog {
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
		validateBoundedArgs(initialValue, min, max);
		validateStep(step, 0);

		final var field = new NumberTextField<>(
				initialValue,
				createBoundedParser(min, max, Integer::parseInt, I18n.translate("prompt.number.not_whole"))
		);

		final BiFunction<NumberInputDialog<Integer, ?>, Integer, Integer> upStepper = (dialog, value) -> {
			final int stepped = value + step;
			if (stepped >= max) {
				dialog.stepUp.setEnabled(false);
				return max;
			} else {
				return stepped;
			}
		};

		final BiFunction<NumberInputDialog<Integer, ?>, Integer, Integer> downStepper = (dialog, value) -> {
			final int stepped = value + step;
			if (min >= stepped) {
				dialog.stepDown.setEnabled(false);
				return min;
			} else {
				return stepped;
			}
		};

		final var numberDialog = new NumberInputDialog<>(
				owner, title, message, I18n.translate("prompt.cancel"), submit, field,
				(dialog, value) -> {
					dialog.stepDown.setEnabled(true);

					final int stepped = value + step;
					if (stepped >= max) {
						dialog.stepUp.setEnabled(false);
						return max;
					} else {
						return stepped;
					}
				},
				(dialog, value) -> {
					dialog.stepUp.setEnabled(true);

					final int stepped = value - step;
					if (min >= stepped) {
						dialog.stepDown.setEnabled(false);
						return min;
					} else {
						return stepped;
					}
				}
		);

		numberDialog.setFont(ScaleUtil.scaleFont(numberDialog.getFont()));

		if (Objects.equals(initialValue, max)) {
			numberDialog.stepUp.setEnabled(false);
		} else if (Objects.equals(initialValue, min)) {
			numberDialog.stepDown.setEnabled(false);
		}

		numberDialog.pack();
		numberDialog.setLocationRelativeTo(owner);

		numberDialog.setVisible(true);
		numberDialog.dispose();

		return numberDialog.field.getValue();
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

	private static <N extends Number & Comparable<N>> void validateStep(N stepAmount, N zero) {
		if (zero.compareTo(stepAmount) >= 0) {
			throw new IllegalArgumentException("step (%s) must be positive!".formatted(stepAmount));
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

	protected final JTextArea message = new JTextArea();

	protected final NumberTextField<N> field;
	protected final JButton stepUp = new JButton();
	protected final JButton stepDown = new JButton();

	protected final JTextArea error = new MinimumSizeTextArea();

	protected final JButton cancel = new JButton();
	protected final JButton submit = new JButton();

	protected NumberInputDialog(
			@Nullable Frame owner, String title, String message, String cancel, String submit,
			NumberTextField<N> field,
			BiFunction<NumberInputDialog<N, D>, N, N> stepUp, BiFunction<NumberInputDialog<N, D>, N, N> stepDown
	) {
		super(owner, title, true);

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setResizable(false);
		this.setLayout(new GridBagLayout());

		this.message.setText(message);
		this.message.setEditable(false);
		this.message.setBorder(createEmptyBorder());

		this.field = field;

		// TODO step button actions
		this.stepUp.setIcon(GuiUtil.getUpChevron());
		this.stepUp.addActionListener(e -> this.field.setValue(stepUp.apply(this, this.field.getValue())));

		this.stepDown.setIcon(GuiUtil.getDownChevron());
		this.stepDown.addActionListener(e -> this.field.setValue(stepDown.apply(this, this.field.getValue())));

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

		this.field.addEditListener(edit -> {
			if (edit.isOk()) {
				this.submit.setEnabled(true);
				this.hideError();
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
