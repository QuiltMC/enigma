package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.enigma.gui.docker.AllClassesDocker;
import org.quiltmc.enigma.gui.docker.CallsTreeDocker;
import org.quiltmc.enigma.gui.docker.CollabDocker;
import org.quiltmc.enigma.gui.docker.DeobfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;
import org.quiltmc.enigma.gui.docker.ImplementationsTreeDocker;
import org.quiltmc.enigma.gui.docker.InheritanceTreeDocker;
import org.quiltmc.enigma.gui.docker.NotificationsDocker;
import org.quiltmc.enigma.gui.docker.ObfuscatedClassesDocker;
import org.quiltmc.enigma.gui.docker.StructureDocker;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DockerConfig extends ReflectiveConfig {
	public final TrackedValue<Integer> leftVerticalDividerLocation = this.value(300);
	public final TrackedValue<Integer> rightVerticalDividerLocation = this.value(300);
	public final TrackedValue<Integer> leftHorizontalDividerLocation = this.value(300);
	public final TrackedValue<Integer> rightHorizontalDividerLocation = this.value(700);
	public final TrackedValue<Boolean> savedWithLeftDockerOpen = this.value(true);

	public final TrackedValue<ValueMap<Docker.Location>> dockerLocations = this.map(new Docker.Location(Docker.Side.LEFT, Docker.VerticalLocation.TOP)).build();

	public void putLocation(Docker docker, Docker.Location location) {
		this.putLocation(docker.getId(), location);
	}

	public void putLocation(String id, Docker.Location location) {
		this.dockerLocations.value().put(id, location);
	}

	public void putLocation(Docker docker, Docker.Side side, Docker.VerticalLocation verticalLocation) {
		this.putLocation(docker.getId(), new Docker.Location(side, verticalLocation));
	}

	@Nullable
	public Docker.Location getLocation(String id) {
		return this.dockerLocations.value().get(id);
	}

	public Map<String, Docker.Location> getLocations(Docker.Side side) {
		return this.dockerLocations.value().entrySet().stream().filter((entry) -> entry.getValue().side() == side).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

	public static Map<Docker, Docker.Location> getDefaultLocations(DockerManager manager) {
		Map<Docker, Docker.Location> locations = new HashMap<>();

		// left
		locations.put(manager.getDocker(ObfuscatedClassesDocker.class), new Docker.Location(Docker.Side.LEFT, Docker.VerticalLocation.TOP));
		locations.put(manager.getDocker(AllClassesDocker.class), new Docker.Location(Docker.Side.LEFT, Docker.VerticalLocation.TOP));
		locations.put(manager.getDocker(DeobfuscatedClassesDocker.class), new Docker.Location(Docker.Side.LEFT, Docker.VerticalLocation.BOTTOM));

		// right
		locations.put(manager.getDocker(StructureDocker.class), new Docker.Location(Docker.Side.RIGHT, Docker.VerticalLocation.TOP));
		locations.put(manager.getDocker(InheritanceTreeDocker.class), new Docker.Location(Docker.Side.RIGHT, Docker.VerticalLocation.TOP));
		locations.put(manager.getDocker(ImplementationsTreeDocker.class), new Docker.Location(Docker.Side.RIGHT, Docker.VerticalLocation.TOP));
		locations.put(manager.getDocker(CallsTreeDocker.class), new Docker.Location(Docker.Side.RIGHT, Docker.VerticalLocation.TOP));

		locations.put(manager.getDocker(CollabDocker.class), new Docker.Location(Docker.Side.RIGHT, Docker.VerticalLocation.BOTTOM));
		locations.put(manager.getDocker(NotificationsDocker.class), new Docker.Location(Docker.Side.RIGHT, Docker.VerticalLocation.BOTTOM));

		return locations;
	}
}
