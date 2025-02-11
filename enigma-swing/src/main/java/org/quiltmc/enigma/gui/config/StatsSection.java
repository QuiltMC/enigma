package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueList;
import org.quiltmc.enigma.api.stats.StatType;

import java.util.EnumSet;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class StatsSection extends ReflectiveConfig.Section {
	public final TrackedValue<String> lastSelectedDir = this.value("");
	public final TrackedValue<String> lastTopLevelPackage = this.value("");

	public final TrackedValue<ValueList<StatType>> includedStatTypes = this.list(StatType.CLASSES, EnumSet.allOf(StatType.class).toArray(StatType[]::new));
	public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);
	public final TrackedValue<Boolean> shouldCountFallbackNames = this.value(false);
}
