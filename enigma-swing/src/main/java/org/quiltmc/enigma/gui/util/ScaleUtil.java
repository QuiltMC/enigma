package org.quiltmc.enigma.gui.util;

import com.github.swingdpi.UiDefaultsScaler;
import com.github.swingdpi.plaf.BasicTweaker;
import com.github.swingdpi.plaf.MetalTweaker;
import com.github.swingdpi.plaf.NimbusTweaker;
import com.github.swingdpi.plaf.WindowsTweaker;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.syntaxpain.SyntaxpainConfiguration;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

public class ScaleUtil {
	private static final List<ScaleChangeListener> listeners = new ArrayList<>();

	public static void setScaleFactor(float scaleFactor) {
		float oldScale = Config.main().scaleFactor.value();
		float clamped = Math.min(Math.max(0.25f, scaleFactor), 10.0f);
		Config.main().scaleFactor.setValue(clamped, true);
		listeners.forEach(l -> l.onScaleChanged(clamped, oldScale));
	}

	public static void addListener(ScaleChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(ScaleChangeListener listener) {
		listeners.remove(listener);
	}

	public static Dimension getDimension(int width, int height) {
		return new Dimension(scale(width), scale(height));
	}

	public static Insets getInsets(int top, int left, int bottom, int right) {
		return new Insets(scale(top), scale(left), scale(bottom), scale(right));
	}

	public static Font getFont(String fontName, int style, int fontSize) {
		return scaleFont(new Font(fontName, style, fontSize));
	}

	public static Font scaleFont(Font font) {
		return createTweakerForCurrentLook(Config.main().scaleFactor.value()).modifyFont("", font);
	}

	public static float scale(float f) {
		return f * Config.main().scaleFactor.value();
	}

	public static float invert(float f) {
		return f / Config.main().scaleFactor.value();
	}

	public static int scale(int i) {
		return (int) (i * Config.main().scaleFactor.value());
	}

	public static Border createEmptyBorder(int top, int left, int bottom, int right) {
		return BorderFactory.createEmptyBorder(scale(top), scale(left), scale(bottom), scale(right));
	}

	public static int invert(int i) {
		return (int) (i / Config.main().scaleFactor.value());
	}

	public static void applyScaling() {
		double scale = Config.main().scaleFactor.value();

		if (Config.currentTheme().needsScaling()) {
			UiDefaultsScaler.updateAndApplyGlobalScaling((int) (100 * scale), true);
		}

		final Font font = SyntaxpainConfiguration.getEditorFont();
		SyntaxpainConfiguration.setEditorFont(font.deriveFont((float) (font.getSize() * scale)));
	}

	@SuppressWarnings("null")
	private static BasicTweaker createTweakerForCurrentLook(float dpiScaling) {
		String testString = UIManager.getLookAndFeel().getName().toLowerCase();

		if (testString.contains("windows")) {
			return new WindowsTweaker(dpiScaling, testString.contains("classic")) {
				@Override
				public Font modifyFont(Object key, Font original) {
					return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
				}
			};
		}

		if (testString.contains("metal")) {
			return new MetalTweaker(dpiScaling) {
				@Override
				public Font modifyFont(Object key, Font original) {
					return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
				}
			};
		}

		if (testString.contains("nimbus")) {
			return new NimbusTweaker(dpiScaling) {
				@Override
				public Font modifyFont(Object key, Font original) {
					return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
				}
			};
		}

		return new BasicTweaker(dpiScaling) {
			@Override
			public Font modifyFont(Object key, Font original) {
				return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
			}
		};
	}

	private static Font fallbackModifyFont(Object key, Font original, Font modified, float scaleFactor, Predicate<Float> unscaledCheck) {
		if (modified == original && !unscaledCheck.test(scaleFactor)) {
			return original.deriveFont(original.getSize() * scaleFactor);
		}

		return modified;
	}
}
