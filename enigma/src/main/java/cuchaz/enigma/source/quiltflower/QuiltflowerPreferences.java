package cuchaz.enigma.source.quiltflower;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuiltflowerPreferences {
    public static final List<String> IGNORED_PREFERENCES = List.of(
            IFernflowerPreferences.BANNER,
            IFernflowerPreferences.INDENT_STRING,
            IFernflowerPreferences.BYTECODE_SOURCE_MAPPING,
            IFernflowerPreferences.MAX_PROCESSING_METHOD,
            IFernflowerPreferences.LOG_LEVEL,
            IFernflowerPreferences.THREADS,
            IFernflowerPreferences.USER_RENAMER_CLASS,
            IFernflowerPreferences.NEW_LINE_SEPARATOR,
            IFernflowerPreferences.ERROR_MESSAGE,
            IFernflowerPreferences.DUMP_TEXT_TOKENS,
            IFernflowerPreferences.REMOVE_IMPORTS
    );

    public static final Map<String, Object> EXTRA_DEFAULTS = Map.of(
            IFernflowerPreferences.BANNER, """
                        /*
                         * Class file decompiled with Quiltflower
                         */

                        """,
            IFernflowerPreferences.INDENT_STRING, "    ",
            IFernflowerPreferences.LOG_LEVEL, IFernflowerLogger.Severity.WARN.name(),
            IFernflowerPreferences.NEW_LINE_SEPARATOR, "1", // Always use LF

            IFernflowerPreferences.PREFERRED_LINE_LENGTH, "180"
    );

    public static final Map<String, Object> OPTIONS = new HashMap<>();

    public static final Map<String, Object> DEFAULTS = getDefaults();

    public static Object getValue(String key) {
        if (OPTIONS.containsKey(key)) {
            return OPTIONS.get(key);
        }

        return DEFAULTS.get(key);
    }

    public static Map<String, Object> getEffectiveOptions() {
        Map<String, Object> options = new HashMap<>(DEFAULTS);
        options.putAll(OPTIONS);
        return options;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getDefaults() {
        try {
            Map<String, Object> defaults = new HashMap<>((Map<String, Object>) IFernflowerPreferences.class.getField("DEFAULTS").get(null));
            defaults.putAll(EXTRA_DEFAULTS);

            return defaults;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Preference> getPreferences() {
        try {
            List<Preference> preferences = new ArrayList<>();
            for (Field field : IFernflowerPreferences.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class || !field.isAnnotationPresent(IFernflowerPreferences.Name.class)) {
                    continue;
                }

                String key = (String) field.get(null);
                if (IGNORED_PREFERENCES.contains(key)) {
                    continue;
                }

                Type type = inferType(key);
                String name = field.getAnnotation(IFernflowerPreferences.Name.class).value();
                String description = field.getAnnotation(IFernflowerPreferences.Description.class).value();

                if (type != null) {
                    preferences.add(new Preference(key, type, name, description));
                }
            }

            return preferences;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Type inferType(String key) {
        Object defaultValue = QuiltflowerPreferences.DEFAULTS.get(key);
        if (defaultValue == null) {
            return null;
        }

        if (defaultValue == "0" || defaultValue == "1") {
            return Type.BOOLEAN;
        }

        try {
            Integer.parseInt(defaultValue.toString());
            return Type.INTEGER;
        } catch (Exception ignored) {
        }

        return Type.STRING;
    }

    public enum Type {
        BOOLEAN,
        STRING,
        INTEGER
    }

    public record Preference(String key, Type type, String name, String description) {
    }
}
