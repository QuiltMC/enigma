package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;
import org.quiltmc.enigma.gui.docker.ObfuscatedClassesDocker;
import org.quiltmc.enigma.util.Pair;

public class DockerConfig extends ReflectiveConfig.Section {
	public final TrackedValue<Integer> leftVerticalDividerLocation = this.value(300);
	public final TrackedValue<Integer> rightVerticalDividerLocation = this.value(300);
	public final TrackedValue<Integer> leftHorizontalDividerLocation = this.value(300);
	public final TrackedValue<Integer> rightHorizontalDividerLocation = this.value(700);
	public final TrackedValue<Boolean> savedWithLeftDockerOpen = this.value(true);

	public final TrackedValue<ValueMap<Pair<String, String>>> dockerLocations = this.map(new Pair<>("", "")).build();

	public void putDockerLocation(String id, Docker.Location location) {
		putDockerLocation(this.dockerLocations, id, location);
	}

	private static void putDockerLocation(TrackedValue<ValueMap<Pair<String, String>>> locations, String id, Docker.Location location) {
		if (location.verticalLocation() == Docker.VerticalLocation.FULL) {
			throw new RuntimeException();
		}

		locations.value().put(id, new Pair<>(location.side().toString(), location.verticalLocation().toString()));
	}

	public static DockerConfig getDefault(DockerManager manager) {
		DockerConfig defaultConfig = new DockerConfig();
		putDockerLocation(defaultConfig.dockerLocations, manager.getDocker(ObfuscatedClassesDocker.class).getId(), new Docker.Location(Docker.Side.LEFT, Docker.VerticalLocation.TOP));
	}
}
