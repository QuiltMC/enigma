package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Result;

import javax.annotation.Nullable;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.quiltmc.enigma.gui.util.GuiUtil.putKeyBindAction;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A dialog that prompts the user for a number within a specified range.
 *
 * <p> User input can be obtained from the static {@code prompt...} methods which create single-use dialogs.
 *
 * @param <N> the type of number the dialog collects
 *
 * @see #promptInt
 * @see #promptFloat
 */
public class NumberInputDialog<N extends Number & Comparable<N>> extends JDialog {
	private static final int INSET = 4;

	/**
	 * Prompts the user for an int between the passed {@code min} and {@code max}, inclusive.
	 *
	 * <p> Consider using {@link GuiUtil#createIntConfigRadioMenu} instead for small int ranges.
	 *
	 * @param owner        the {@link Frame} from which the dialog is displayed
	 * @param initialValue the initial value
	 * @param min          the minimum value
	 * @param max          the maximum value
	 * @param defaultStep  the amount to step the value by when the user clicks a step up/down button or inputs
	 *                     {@link KeyBinds#STEP_UP STEP_UP}/{@link KeyBinds#STEP_DOWN STEP_DOWN};
	 *                     must be positive
	 * @param altStep      the amount to step the value by when the user inputs
	 *                     {@link KeyBinds#ALT_STEP_UP ALT_STEP_UP}/{@link KeyBinds#ALT_STEP_DOWN ALT_STEP_DOWN};
	 *                     must be positive
	 * @param title        the title displayed in the window's title bar
	 * @param message      the message prompting the user for input
	 *
	 * @return the user's input if it was submitted, or the passed {@code initialValue} otherwise;
	 *         only {@code null} if the passed {@code initialValue} was {@code null} and input was not submitted
	 *
	 * @throws IllegalArgumentException if:
	 * <ul>
	 *     <li> the passed {@code min} is not strictly less than the passed {@code max}
	 *     <li> the passed {@code initialValue} is non-{@code null} and not between the passed
	 *          {@code min} and {@code max}
	 *     <li> the passed {@code step} or {@code altStep} is not positive
	 *     <li> the passed {@code step} or {@code altStep} is greater than the difference between the passed
	 *          {@code min} and {@code max}
	 * </ul>
	 *
	 * @see #promptFloat
	 */
	public static Integer promptInt(
			@Nullable Frame owner,
			@Nullable Integer initialValue, int min, int max, int defaultStep, int altStep,
			String title, String message, String submit
	) {
		return promptNumber(
				owner, title, message, submit,
				initialValue, min, max, defaultStep, altStep, 0,
				Integer::sum, (left, right) -> left - right,
				input -> {
					try {
						return Result.ok(Integer.parseInt(input));
					} catch (NumberFormatException e) {
						return Error.Type.OTHER.err(I18n.translate("prompt.number.not_whole"));
					}
				}
		);
	}

	/**
	 * Prompts the user for a float between the passed {@code min} and {@code max}, inclusive.
	 *
	 * @param owner        the {@link Frame} from which the dialog is displayed
	 * @param initialValue the initial value
	 * @param min          the minimum value
	 * @param max          the maximum value
	 * @param defaultStep  the amount to step the value by when the user clicks a step up/down button or inputs
	 *                     {@link KeyBinds#STEP_UP STEP_UP}/{@link KeyBinds#STEP_DOWN STEP_DOWN};
	 *                     must be positive
	 * @param altStep      the amount to step the value by when the user inputs
	 *                     {@link KeyBinds#ALT_STEP_UP ALT_STEP_UP}/{@link KeyBinds#ALT_STEP_DOWN ALT_STEP_DOWN};
	 *                     must be positive
	 * @param title        the title displayed in the window's title bar
	 * @param message      the message prompting the user for input
	 *
	 * @return the user's input if it was submitted, or the passed {@code initialValue} otherwise;
	 *         only {@code null} if the passed {@code initialValue} was {@code null} and input was not submitted
	 *
	 * @throws IllegalArgumentException if:
	 * <ul>
	 *     <li> the passed {@code min} is not strictly less than the passed {@code max}
	 *     <li> the passed {@code initialValue} is non-{@code null} and not between the passed
	 *          {@code min} and {@code max}
	 *     <li> the passed {@code step} or {@code altStep} is not positive
	 *     <li> the passed {@code step} or {@code altStep} is greater than the difference between the passed
	 *          {@code min} and {@code max}
	 * </ul>
	 *
	 * @see #promptInt
	 */
	public static Float promptFloat(
			@Nullable Frame owner,
			@Nullable Float initialValue, float min, float max, float defaultStep, float altStep,
			String title, String message, String submit
	) {
		return promptNumber(
				owner, title, message, submit,
				initialValue, min, max, defaultStep, altStep, 0f,
				Float::sum, (left, right) -> left - right,
				input -> {
					try {
						return Result.ok(Float.parseFloat(input));
					} catch (NumberFormatException e) {
						return Error.Type.OTHER.err(I18n.translate("prompt.number.not_number"));
					}
				}
		);
	}

	private static <N extends Number & Comparable<N>> N promptNumber(
			@Nullable Frame owner, String title, String message, String submit,
			@Nullable N initialValue, N min, N max, N defaultStep, N altStep, N zero,
			BinaryOperator<N> add, BinaryOperator<N> subtract,
			Function<String, Result<N, Error>> parse
	) {
		if (min.compareTo(max) >= 0) {
			throw new IllegalArgumentException("min (%s) must be strictly less than max (%s)!".formatted(min, max));
		}

		if (initialValue != null && !isInRange(initialValue, min, max)) {
			throw new IllegalArgumentException(
				"initialValue (%s) is out of range [%s, %s]!".formatted(initialValue, min, max)
			);
		}

		if (zero.compareTo(defaultStep) >= 0) {
			throw new IllegalArgumentException("defaultStep (%s) must be positive!".formatted(defaultStep));
		}

		if (zero.compareTo(altStep) >= 0) {
			throw new IllegalArgumentException("altStep (%s) must be positive!".formatted(defaultStep));
		}

		final N rangeSize = subtract.apply(max, min);
		if (defaultStep.compareTo(rangeSize) > 0) {
			throw new IllegalArgumentException(
				"defaultStep (%s) must not be greater than the size (%s) of the range [%s, %s]!"
					.formatted(defaultStep, rangeSize, min, max)
			);
		}

		if (altStep.compareTo(rangeSize) > 0) {
			throw new IllegalArgumentException(
				"altStep (%s) must not be greater than the size (%s) of the range [%s, %s]!"
					.formatted(defaultStep, rangeSize, min, max)
			);
		}

		final var numberDialog = new NumberInputDialog<>(
				owner, title, message, I18n.translate("prompt.cancel"), submit,
				initialValue, min, max, defaultStep, altStep,
				add, subtract, parse
		);

		numberDialog.setFont(ScaleUtil.scaleFont(numberDialog.getFont()));

		numberDialog.pack();
		numberDialog.setLocationRelativeTo(owner);

		numberDialog.field.requestFocus();
		numberDialog.setVisible(true);
		numberDialog.dispose();

		return numberDialog.field.getValue();
	}

	private static <N extends Number & Comparable<N>> boolean isInRange(N value, N min, N max) {
		return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
	}

	private static void ifEnabledElseErrorFeedback(Component component, Runnable onEnabled) {
		ifEnabledElseErrorFeedback(component, ignored -> onEnabled.run());
	}

	private static <C extends Component> void ifEnabledElseErrorFeedback(C component, Consumer<C> onEnabled) {
		if (component.isEnabled()) {
			onEnabled.accept(component);
		} else {
			UIManager.getLookAndFeel().provideErrorFeedback(component);
		}
	}

	private static <N extends Number> String outOfRangeMessageOf(N min, N max) {
		return I18n.translateFormatted("prompt.number.not_in_range", min, max);
	}

	protected final N min;
	protected final N max;

	protected final BinaryOperator<N> add;
	protected final BinaryOperator<N> subtract;

	// children
	// message row
	protected final JTextArea message = new JTextArea();
	// field row
	protected final NumberTextField<N, Error> field;
	protected final JButton stepUpButton = new JButton();
	protected final JButton stepDownButton = new JButton();
	// error row
	protected final JTextArea error = new JTextArea();
	// button row
	protected final JButton cancel = new JButton();
	protected final JButton submit = new JButton();

	@Nullable
	private Error.Type lastError;

	/**
	 * Constructs a dialog and initializes its components. Performs no input validation.
	 */
	protected NumberInputDialog(
			@Nullable Frame owner, String title, String message, String cancel, String submit,
			@Nullable N initialValue, N min, N max, N defaultStep, N altStep,
			BinaryOperator<N> add, BinaryOperator<N> subtract, Function<String, Result<N, Error>> parse
	) {
		this(owner, title, min, max, add, subtract, new NumberTextField<>(initialValue, input -> {
			return parse.apply(input).andThen(parsed -> {
				if (min.compareTo(parsed) > 0) {
					return Error.Type.LOW.err(outOfRangeMessageOf(min, max));
				} else if (parsed.compareTo(max) > 0) {
					return Error.Type.HIGH.err(outOfRangeMessageOf(min, max));
				} else {
					return Result.ok(parsed);
				}
			});
		}));

		this.setAlwaysOnTop(true);
		this.setType(Window.Type.POPUP);
		this.setResizable(false);
		final var content = new JPanel(new GridBagLayout());
		this.setContentPane(content);

		this.message.setText(message);
		this.message.setEditable(false);
		this.message.setBorder(createEmptyBorder());

		final Runnable stepUpDefault = () -> this.stepUpBy(defaultStep);
		final Runnable stepUpAlt = () -> this.stepUpBy(altStep);
		final Runnable stepDownDefault = () -> this.stepDownBy(defaultStep);
		final Runnable stepDownAlt = () -> this.stepDownBy(altStep);

		this.stepUpButton.setIcon(GuiUtil.getUpChevron());
		this.stepUpButton.addActionListener(e -> {
			if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
				stepUpAlt.run();
			} else {
				stepUpDefault.run();
			}
		});

		this.stepDownButton.setIcon(GuiUtil.getDownChevron());
		this.stepDownButton.addActionListener(e -> {
			if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
				stepDownAlt.run();
			} else {
				stepDownDefault.run();
			}
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
			final boolean ok = edit.isOk();
			if (ok) {
				this.lastError = null;
				this.hideError();
				this.updateStepButtons(edit.unwrap());
			} else {
				final Error error = edit.unwrapErr();
				this.lastError = error.type;
				this.showError(error.message);
				this.updateStepButtons(error.type);
			}

			this.submit.setEnabled(ok);
		});

		putKeyBindAction(
				KeyBinds.DIALOG_SAVE, content,
				e -> ifEnabledElseErrorFeedback(this.submit, AbstractButton::doClick)
		);
		putKeyBindAction(KeyBinds.EXIT, content, e -> this.cancel.doClick());

		putKeyBindAction(
				KeyBinds.STEP_UP, content,
				e -> ifEnabledElseErrorFeedback(this.stepUpButton, stepUpDefault)
		);
		putKeyBindAction(
				KeyBinds.ALT_STEP_UP, content,
				e -> ifEnabledElseErrorFeedback(this.stepUpButton, stepUpAlt)
		);

		putKeyBindAction(
				KeyBinds.STEP_DOWN, content,
				e -> ifEnabledElseErrorFeedback(this.stepDownButton, stepDownDefault)
		);
		putKeyBindAction(
				KeyBinds.ALT_STEP_DOWN, content,
				e -> ifEnabledElseErrorFeedback(this.stepDownButton, stepDownAlt)
		);

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
		fieldRow.add(this.stepUpButton, baseBuilder.insets(0, 0, INSET, 0).pos(1, 0).build());
		fieldRow.add(this.stepDownButton, baseBuilder.pos(1, 1).build());
		this.add(fieldRow, insetBuilder.pos(0, y++).fill(GridBagConstraints.HORIZONTAL).build());

		this.add(this.error, insetBuilder.pos(0, y++).build());

		final JPanel buttonRow = new JPanel(new GridBagLayout());
		buttonRow.add(this.cancel, baseBuilder.insets(0, INSET, 0, 0).pos(0, 0).build());
		buttonRow.add(this.submit, baseBuilder.pos(1, 0).build());
		this.add(buttonRow, insetBuilder.pos(0, y++).anchor(GridBagConstraints.LINE_END).build());
	}

	/**
	 * Constructs a dialog by initializing its fields. Performs no further setup.
	 */
	protected NumberInputDialog(
			@Nullable Frame owner, String title, N min, N max,
			BinaryOperator<N> add, BinaryOperator<N> subtract, NumberTextField<N, Error> field
	) {
		super(owner, title, true);

		this.min = min;
		this.max = max;

		this.add = add;
		this.subtract = subtract;

		this.field = field;
	}

	@Nullable
	protected Error.Type getLastError() {
		return this.lastError;
	}

	/**
	 * @return the clamped {@code value}
	 */
	protected N updateStepButtons(N value) {
		final boolean enableUp;
		final boolean enableDown;

		final N clamped;
		if (this.min.compareTo(value) >= 0) {
			enableUp = true;
			enableDown = false;

			clamped = this.min;
		} else if (value.compareTo(this.max) >= 0) {
			enableUp = false;
			enableDown = true;

			clamped = this.max;
		} else {
			enableUp = true;
			enableDown = true;

			clamped = value;
		}

		this.stepUpButton.setEnabled(enableUp);
		this.stepDownButton.setEnabled(enableDown);

		return clamped;
	}

	private void updateStepButtons(Error.Type errorType) {
		final boolean enableUp;
		final boolean enableDown;
		switch (errorType) {
			case LOW -> {
				enableUp = true;
				enableDown = false;
			}
			case HIGH -> {
				enableUp = false;
				enableDown = true;
			}
			case OTHER -> {
				enableUp = true;
				enableDown = true;
			}
			default -> throw new AssertionError();
		}

		this.stepUpButton.setEnabled(enableUp);
		this.stepDownButton.setEnabled(enableDown);
	}

	/**
	 * Shows the passed {@code error} as feedback to the user.
	 *
	 * <p> {@code this.error} is repainted, but nothing is re-packed,
	 * so error messages wider than the width of the dialog won't render correctly.
	 */
	protected void showError(String error) {
		this.error.setForeground(Config.getCurrentSyntaxPaneColors().error.value());
		this.error.setText(error);
		this.error.repaint();
	}

	protected void hideError() {
		this.error.setForeground(new Color(0, true));
		// repainting just this.error leaves an artifact of this.field's error border
		this.repaint();
	}

	protected void stepUpBy(N step) {
		if (!this.tryStepErrorUp()) {
			this.stepBy(step, this.add);
		}
	}

	protected void stepDownBy(N step) {
		if (!this.tryStepErrorDown()) {
			this.stepBy(step, this.subtract);
		}
	}

	protected void stepBy(N step, BinaryOperator<N> operation) {
		this.stepUpButton.setEnabled(true);

		final N stepped = operation.apply(this.field.getEditOrValue(), step);

		final N clamped = this.updateStepButtons(stepped);

		this.field.edit(clamped);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	protected boolean tryStepErrorUp() {
		if (this.lastError == null) {
			return false;
		} else {
			switch (this.lastError) {
				case LOW, OTHER -> {
					this.lastError = null;

					this.field.edit(this.min);

					this.stepUpButton.setEnabled(true);
					this.stepDownButton.setEnabled(false);
				}
			}

			return true;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	protected boolean tryStepErrorDown() {
		if (this.lastError == null) {
			return false;
		} else {
			switch (this.lastError) {
				case HIGH, OTHER -> {
					this.lastError = null;

					this.field.edit(this.max);

					this.stepUpButton.setEnabled(false);
					this.stepDownButton.setEnabled(true);
				}
			}

			return true;
		}
	}

	protected record Error(Error.Type type, String message) {
		protected enum Type {
			LOW, HIGH, OTHER;

			public <O> Result<O, Error> err(String message) {
				return Result.err(new Error(this, message));
			}
		}
	}
}
