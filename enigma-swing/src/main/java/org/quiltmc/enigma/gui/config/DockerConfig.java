package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.gui.docker.CallsTreeDocker;
import org.quiltmc.enigma.gui.docker.ClassesDocker;
import org.quiltmc.enigma.gui.docker.CollabDocker;
import org.quiltmc.enigma.gui.docker.DeobfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;
import org.quiltmc.enigma.gui.docker.ImplementationsTreeDocker;
import org.quiltmc.enigma.gui.docker.InheritanceTreeDocker;
import org.quiltmc.enigma.gui.docker.NotificationsDocker;
import org.quiltmc.enigma.gui.docker.ObfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.StructureDocker;
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

	private static void putDockerLocation(DockerConfig config, Docker docker, Docker.Side side, Docker.VerticalLocation verticalLocation) {
		putDockerLocation(config.dockerLocations, docker.getId(), new Docker.Location(side, verticalLocation));
	}

	public static TrackedValue<ValueMap<Pair<String, String>>> getDefaultLocations(DockerManager manager) {
		DockerConfig defaultConfig = new DockerConfig();

		// left
		putDockerLocation(defaultConfig, manager.getDocker(ObfuscatedClassesDocker.class), Docker.Side.LEFT, Docker.VerticalLocation.TOP);
		putDockerLocation(defaultConfig, manager.getDocker(ClassesDocker.class), Docker.Side.LEFT, Docker.VerticalLocation.TOP);
		putDockerLocation(defaultConfig, manager.getDocker(DeobfuscatedClassesDocker.class), Docker.Side.LEFT, Docker.VerticalLocation.BOTTOM);

		// right
		putDockerLocation(defaultConfig, manager.getDocker(StructureDocker.class), Docker.Side.RIGHT, Docker.VerticalLocation.TOP);
		putDockerLocation(defaultConfig, manager.getDocker(InheritanceTreeDocker.class), Docker.Side.RIGHT, Docker.VerticalLocation.TOP);
		putDockerLocation(defaultConfig, manager.getDocker(ImplementationsTreeDocker.class), Docker.Side.RIGHT, Docker.VerticalLocation.TOP);
		putDockerLocation(defaultConfig, manager.getDocker(CallsTreeDocker.class), Docker.Side.RIGHT, Docker.VerticalLocation.TOP);

		putDockerLocation(defaultConfig, manager.getDocker(CollabDocker.class), Docker.Side.RIGHT, Docker.VerticalLocation.BOTTOM);
		putDockerLocation(defaultConfig, manager.getDocker(NotificationsDocker.class), Docker.Side.RIGHT, Docker.VerticalLocation.BOTTOM);

		return defaultConfig.dockerLocations;
	}
}
