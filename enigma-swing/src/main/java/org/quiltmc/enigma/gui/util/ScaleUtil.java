package org.quiltmc.enigma.gui.util;

import com.github.swingdpi.UiDefaultsScaler;
import com.github.swingdpi.plaf.BasicTweaker;
import com.github.swingdpi.plaf.MetalTweaker;
import com.github.swingdpi.plaf.NimbusTweaker;
import com.github.swingdpi.plaf.WindowsTweaker;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.syntaxpain.SyntaxpainConfiguration;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;

public class ScaleUtil {
	private static final List<ScaleChangeListener> listeners = new ArrayList<>();

	public static void setScaleFactor(float scaleFactor) {
		float oldScale = Config.main().scaleFactor.value();
		float clamped = Utils.clamp(scaleFactor, 0.25f, 10.0f);
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
		return createEnsuredFontScalingTweakerForCurrentLook(Config.main().scaleFactor.value()).modifyFont("", font);
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
		final double scale = Config.main().scaleFactor.value();

		if (Config.currentTheme().onlyScaleFonts()) {
			scaleFontsOnly((float) scale);
		} else {
			UiDefaultsScaler.updateAndApplyGlobalScaling((int) (100 * scale), true);
		}

		final Font font = SyntaxpainConfiguration.getEditorFont();
		SyntaxpainConfiguration.setEditorFont(font.deriveFont((float) (font.getSize() * scale)));
	}

	// effectively UiDefaultsScaler::modifyDefaults but only for fonts
	private static void scaleFontsOnly(float scale) {
		final UIDefaults defaults = UIManager.getLookAndFeelDefaults();

		final BasicTweaker tweaker = new BasicTweaker(scale);
		for (Object key: Collections.list(defaults.keys())) {
			final Object original = defaults.get(key);

			if (original instanceof Font originalFont) {
				final Font modifiedFont = tweaker.modifyFont(key, originalFont);

				if (modifiedFont != null && modifiedFont != originalFont) {
					defaults.put(key, modifiedFont);
				}
			}
		}
	}

	@SuppressWarnings("null")
	private static BasicTweaker createEnsuredFontScalingTweakerForCurrentLook(float dpiScaling) {
		final String lookAndFeelName = UIManager.getLookAndFeel().getName().toLowerCase();

		final Function<Float, BasicTweaker> delegateTweakerFactory;
		if (lookAndFeelName.contains("windows")) {
			delegateTweakerFactory = scale -> new WindowsTweaker(scale, lookAndFeelName.contains("classic"));
		} else if (lookAndFeelName.contains("metal")) {
			delegateTweakerFactory = MetalTweaker::new;
		} else if (lookAndFeelName.contains("nimbus")) {
			delegateTweakerFactory = NimbusTweaker::new;
		} else {
			delegateTweakerFactory = BasicTweaker::new;
		}

		return new DelegatingEnsuredFontScalingBasicTweaker(dpiScaling, delegateTweakerFactory);
	}

	private static class DelegatingEnsuredFontScalingBasicTweaker extends BasicTweaker {
		private final BasicTweaker delegate;

		DelegatingEnsuredFontScalingBasicTweaker(
				float scaleFactor, Function<Float, BasicTweaker> delegateFactory
		) {
			super(scaleFactor);
			this.delegate = delegateFactory.apply(scaleFactor);
		}

		@Override
		public void initialTweaks() {
			this.delegate.initialTweaks();
		}

		@Override
		public Font modifyFont(Object key, Font original) {
			final Font modified = this.delegate.modifyFont(key, original);
			if (modified == original && !BasicTweaker.isUnscaled(this.scaleFactor)) {
				return original.deriveFont(original.getSize() * this.scaleFactor);
			}

			return modified;
		}

		@Override
		public Icon modifyIcon(Object key, Icon original) {
			return this.delegate.modifyIcon(key, original);
		}

		@Override
		public Dimension modifyDimension(Object key, Dimension original) {
			return this.delegate.modifyDimension(key, original);
		}

		@Override
		public Integer modifyInteger(Object key, Integer original) {
			return this.delegate.modifyInteger(key, original);
		}

		@Override
		public Insets modifyInsets(Object key, Insets original) {
			return this.delegate.modifyInsets(key, original);
		}

		@Override
		public void finalTweaks() {
			this.delegate.finalTweaks();
		}

		@Override
		public void setDoExtraTweaks(boolean tweak) {
			this.delegate.setDoExtraTweaks(tweak);
		}
	}
}
