package org.quiltmc.enigma.gui.config;

import org.jspecify.annotations.Nullable;
import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;

import java.util.HashMap;
import java.util.Map;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class DockerConfig extends ReflectiveConfig {
	public final TrackedValue<Integer> leftVerticalDividerLocation = this.value(300);
	public final TrackedValue<Integer> rightVerticalDividerLocation = this.value(300);
	public final TrackedValue<Integer> leftHorizontalDividerLocation = this.value(300);
	public final TrackedValue<Integer> rightHorizontalDividerLocation = this.value(700);
	@Comment("A bodge to properly offset the right divider location depending on UI state.")
	public final TrackedValue<Boolean> savedWithLeftDockerOpen = this.value(true);

	@Comment("The location of each docker's button in the button sidebars.")
	@Comment("Encoded with the side (RIGHT or LEFT) followed by a semicolon (;) and the vertical location (TOP or BOTTOM).")
	public final TrackedValue<ValueMap<Docker.Location>> buttonLocations = this.map(new Docker.Location(Docker.Side.LEFT, Docker.VerticalLocation.TOP)).build();
	@Comment("The currently active dockers on the left side of the UI.")
	public final TrackedValue<SelectedDockers> leftDockers = this.value(new SelectedDockers("", "", "all_classes"));
	@Comment("The currently active dockers on the right side of the UI.")
	public final TrackedValue<SelectedDockers> rightDockers = this.value(new SelectedDockers("", "", "structure"));

	public SelectedDockers getSelectedDockers(Docker.Side side) {
		return side == Docker.Side.LEFT ? this.leftDockers.value() : this.rightDockers.value();
	}

	public void putButtonLocation(String id, Docker.Location location) {
		this.buttonLocations.value().put(id, location);
	}

	public void putButtonLocation(Docker docker, Docker.Side side, Docker.VerticalLocation verticalLocation) {
		this.putButtonLocation(docker.getId(), new Docker.Location(side, verticalLocation));
	}

	@Nullable
	public Docker.Location getButtonLocation(String id) {
		return this.buttonLocations.value().get(id);
	}

	public int getVerticalDividerLocation(Docker.Side side) {
		return side == Docker.Side.LEFT ? this.leftVerticalDividerLocation.value() : this.rightVerticalDividerLocation.value();
	}

	public void setVerticalDividerLocation(Docker.Side side, int value) {
		if (side == Docker.Side.LEFT) {
			this.leftVerticalDividerLocation.setValue(value, true);
		} else {
			this.rightVerticalDividerLocation.setValue(value, true);
		}
	}

	public int getHorizontalDividerLocation(Docker.Side side) {
		return side == Docker.Side.LEFT ? this.leftHorizontalDividerLocation.value() : this.rightHorizontalDividerLocation.value();
	}

	public void setHorizontalDividerLocation(Docker.Side side, int value) {
		if (side == Docker.Side.LEFT) {
			this.leftHorizontalDividerLocation.setValue(value, true);
		} else {
			this.rightHorizontalDividerLocation.setValue(value, true);
		}
	}

	public void updateButtonLocations(DockerManager manager) {
		for (Docker docker : manager.getDockers()) {
			this.putButtonLocation(docker.getId(), docker.getPreferredButtonLocation());
		}
	}

	public static class SelectedDockers implements ConfigSerializableObject<ValueMap<String>> {
		private String top;
		private String bottom;
		private String full;

		public SelectedDockers(String top, String bottom, String full) {
			this.top = top;
			this.bottom = bottom;
			this.full = full;
		}

		public void add(String id, Docker.VerticalLocation location) {
			switch (location) {
				case TOP -> {
					if (!this.full.isBlank()) {
						this.bottom = this.full;
						this.full = "";
					}

					this.top = id;
				}
				case BOTTOM -> {
					if (!this.full.isBlank()) {
						this.top = this.full;
						this.full = "";
					}

					this.bottom = id;
				}
				case FULL -> {
					this.top = "";
					this.bottom = "";
					this.full = id;
				}
			}
		}

		public Map<String, Docker.VerticalLocation> asMap() {
			Map<String, Docker.VerticalLocation> map = new HashMap<>();
			if (!this.top.isBlank()) {
				map.put(this.top, Docker.VerticalLocation.TOP);
			}

			if (!this.bottom.isBlank()) {
				map.put(this.bottom, Docker.VerticalLocation.BOTTOM);
			}

			if (!this.full.isBlank()) {
				map.put(this.full, Docker.VerticalLocation.FULL);
			}

			return map;
		}

		@Override
		public SelectedDockers convertFrom(ValueMap<String> representation) {
			return new SelectedDockers(representation.get("top"), representation.get("bottom"), representation.get("full"));
		}

		@Override
		public ValueMap<String> getRepresentation() {
			return ValueMap.builder("")
				.put("top", this.top)
				.put("bottom", this.bottom)
				.put("full", this.full)
				.build();
		}

		@Override
		public ComplexConfigValue copy() {
			return this;
		}

		@Override
		public String toString() {
			return "SelectedDockers["
				+ "top='" + this.top + "'"
				+ ", bottom='" + this.bottom + "'"
				+ ", full='" + this.full + "'"
				+ "]";
		}
	}
}
