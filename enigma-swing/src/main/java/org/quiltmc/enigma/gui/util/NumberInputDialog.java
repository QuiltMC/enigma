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
import java.awt.event.KeyEvent;
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
	 * @param owner        the {@link Frame} from which the dialog is displayed
	 * @param initialValue the initial value
	 * @param min          the minimum value
	 * @param max          the maximum value
	 * @param defaultStep  the amount to step the value by when the user clicks step up/down buttons; must be positive
	 * @param altStep      the amount to step the value by when the user inputs {@link  KeyBinds#ALT_STEP_UP} or
	 *                     {@link KeyBinds#ALT_STEP_DOWN}; must be positive
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
	 * @param defaultStep  the amount to step the value by when the user clicks step up/down buttons; must be positive
	 * @param altStep      the amount to step the value by when the user inputs {@link  KeyBinds#ALT_STEP_UP} or
	 *                     {@link KeyBinds#ALT_STEP_DOWN}; must be positive
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
			throw new IllegalArgumentException("step (%s) must be positive!".formatted(defaultStep));
		}

		final N rangeSize = subtract.apply(max, min);
		if (defaultStep.compareTo(rangeSize) > 0) {
			throw new IllegalArgumentException(
				"step (%s) must not be greater than the size (%s) of the range [%s, %s]!"
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
			if ((e.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
				stepUpAlt.run();
			} else {
				stepUpDefault.run();
			}
		});

		this.stepDownButton.setIcon(GuiUtil.getDownChevron());
		this.stepDownButton.addActionListener(e -> {
			if ((e.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0) {
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
			this.updateStepButtons(Result.ok(initialValue));
		}

		this.field.addEditListener(edit -> {
			if (edit.isOk()) {
				this.submit.setEnabled(true);
				this.hideError();
			} else {
				this.showError(edit.unwrapErr().message);
			}

			this.updateStepButtons(edit);
		});

		putKeyBindAction(KeyBinds.DIALOG_SAVE, content, e -> ifEnabledElseErrorFeedback(
				this.submit, AbstractButton::doClick
		));
		putKeyBindAction(KeyBinds.EXIT, content, e -> this.dispose());

		putKeyBindAction(KeyBinds.STEP_UP, content, e -> ifEnabledElseErrorFeedback(
				this.stepUpButton, ignored -> stepUpDefault.run()
		));
		putKeyBindAction(KeyBinds.ALT_STEP_UP, content, e -> ifEnabledElseErrorFeedback(
				this.stepUpButton, ignored -> stepUpAlt.run()
		));

		putKeyBindAction(KeyBinds.STEP_DOWN, content, e -> ifEnabledElseErrorFeedback(
				this.stepDownButton, ignored -> stepDownDefault.run()
		));
		putKeyBindAction(KeyBinds.ALT_STEP_DOWN, content, e -> ifEnabledElseErrorFeedback(
				this.stepDownButton, ignored -> stepDownAlt.run()
		));

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

	protected void updateStepButtons(Result<N, Error> edit) {
		final boolean enableUp;
		final boolean enableDown;

		if (edit.isOk()) {
			final N value = edit.unwrap();
			if (this.min.compareTo(value) >= 0) {
				enableUp = true;
				enableDown = false;
			} else if (value.compareTo(this.max) >= 0) {
				enableUp = false;
				enableDown = true;
			} else {
				enableUp = true;
				enableDown = true;
			}
		} else {
			final Error.Type error = edit.unwrapErr().type;
			switch (error) {
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
		this.stepDownButton.setEnabled(true);

		final N stepped = this.add.apply(this.field.getEditOrValue(), step);
		final N clamped;
		if (stepped.compareTo(this.max) >= 0) {
			this.stepUpButton.setEnabled(false);
			clamped = this.max;
		} else {
			clamped = stepped;
		}

		this.field.edit(clamped);
	}

	protected void stepDownBy(N step) {
		this.stepUpButton.setEnabled(true);

		final N stepped = this.subtract.apply(this.field.getEditOrValue(), step);
		final N clamped;
		if (this.min.compareTo(stepped) >= 0) {
			this.stepDownButton.setEnabled(false);
			clamped = this.min;
		} else {
			clamped = stepped;
		}

		this.field.edit(clamped);
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
