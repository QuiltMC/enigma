package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.gui.Gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DockerManager {
	private final Map<Class<? extends Docker>, Docker> dockers = new LinkedHashMap<>();
	private final Map<String, Class<? extends Docker>> dockerClasses = new HashMap<>();

	private final Dock rightDock;
	private final Dock leftDock;

	public DockerManager(Gui gui) {
		this.rightDock = new Dock(gui, Docker.Side.RIGHT);
		this.leftDock = new Dock(gui, Docker.Side.LEFT);
	}

	/**
	 * Gets the {@link Dock} instance positioned on the right side of the screen.
	 * @return the right dock
	 */
	public Dock getRightDock() {
		return this.rightDock;
	}

	/**
	 * Gets the {@link Dock} instance positioned on the left side of the screen.
	 * @return the left dock
	 */
	public Dock getLeftDock() {
		return this.leftDock;
	}

	/**
	 * {@return a map of all currently active dockers, keyed by their locations}
	 */
	public Map<Docker.Location, Docker> getActiveDockers() {
		Map<Docker.Location, Docker> activeDockers = new HashMap<>();

		activeDockers.putAll(this.leftDock.getHostedDockers().entrySet().stream().collect(Collectors.toMap(
				entry -> new Docker.Location(Docker.Side.LEFT, entry.getKey()),
				Map.Entry::getValue
		)));

		activeDockers.putAll(this.rightDock.getHostedDockers().entrySet().stream().collect(Collectors.toMap(
				entry -> new Docker.Location(Docker.Side.RIGHT, entry.getKey()),
				Map.Entry::getValue
		)));

		return activeDockers;
	}

	/**
	 * Hosts a docker, making it visible, in the location provided.
	 * @param docker the docker to be hosted
	 * @param location the location to place it
	 */
	public void host(Docker docker, Docker.Location location) {
		this.host(docker, location.side(), location.verticalLocation());
	}

	/**
	 * Hosts a docker, making it visible, in the location provided.
	 * @param docker the docker to be hosted
	 * @param side the side to place it on
	 * @param location the vertical location to place it
	 */
	public void host(Docker docker, Docker.Side side, Docker.VerticalLocation location) {
		if (side == Docker.Side.LEFT) {
			this.leftDock.host(docker, location);
		} else {
			this.rightDock.host(docker, location);
		}
	}

	/**
	 * Restores the state of both docks to the version saved in the config.
	 * @see Dock#restoreState(DockerManager)
	 */
	public void restoreStateFromConfig() {
		this.leftDock.restoreState(this);
		this.rightDock.restoreState(this);
	}

	/**
	 * Registers a new docker to be available in the GUI.
	 * @param docker the docker to be registered
	 */
	public void registerDocker(Docker docker) {
		this.dockers.put(docker.getClass(), docker);
		this.dockerClasses.put(docker.getId(), docker.getClass());
	}

	/**
	 * Gets a docker by its class.
	 * @param clazz the class of the docker to get
	 * @return the docker
	 */
	@SuppressWarnings("unchecked")
	public <T extends Docker> T getDocker(Class<T> clazz) {
		Docker panel = this.dockers.get(clazz);
		if (panel != null) {
			return (T) panel;
		} else {
			throw new IllegalArgumentException("no docker registered for class " + clazz);
		}
	}

	/**
	 * Gets a docker by its id.
	 * @param id the id of the docker to get
	 * @return the docker
	 */
	public Docker getDocker(String id) {
		if (!this.dockerClasses.containsKey(id)) {
			throw new IllegalArgumentException("no docker registered for id " + id);
		}

		return this.getDocker(this.dockerClasses.get(id));
	}

	/**
	 * Gets all currently registered dockers in a nice neat collection.
	 * @return the complete collection of dockers
	 */
	public Collection<Docker> getDockers() {
		return this.dockers.values();
	}
}
