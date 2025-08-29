package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueList;
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.StatType;

import java.util.HashSet;
import java.util.Set;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class StatsSection extends ReflectiveConfig.Section {
	public final TrackedValue<String> lastSelectedDir = this.value("");
	public final TrackedValue<String> lastTopLevelPackage = this.value("");

	@Comment("Used only for stat icons.")
	public final TrackedValue<ValueList<StatType>> includedStatTypes = this.list(StatType.CLASSES, StatType.values());
	public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);
	public final TrackedValue<Boolean> shouldCountFallbackNames = this.value(false);

	public Set<StatType> getIncludedTypesForIcons(Set<StatType> editableTypes) {
		var types = new HashSet<>(editableTypes);
		types.removeIf(type -> !this.includedStatTypes.value().contains(type));
		return types;
	}

	public GenerationParameters createIconGenParameters(Set<StatType> editableTypes) {
		return this.createGenParameters(this.getIncludedTypesForIcons(editableTypes));
	}

	public GenerationParameters createGenParameters(Set<StatType> includedTypes) {
		return new GenerationParameters(includedTypes, this.shouldIncludeSyntheticParameters.value(), this.shouldCountFallbackNames.value());
	}
}
