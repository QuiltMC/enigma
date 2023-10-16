package org.quiltmc.enigma.gui.config;

import org.quiltmc.enigma.gui.docker.Dock;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;
import org.tinylog.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class nonsense {
	public static void setHostedDockers(Docker.Side side, Docker[] dockers) {
		String[] dockerData = new String[]{"", ""};
		for (int i = 0; i < dockers.length; i++) {
			Docker docker = dockers[i];

			if (docker != null) {
				Docker.Location location = Dock.Util.findLocation(docker);
				if (location != null) {
					dockerData[i] = (docker.getId() + PAIR_SEPARATOR + location.verticalLocation());
				}
			}
		}

		swing.data().section(HOSTED_DOCKERS).setArray(side.name(), dockerData);
	}

	public static Optional<Map<Docker, Docker.VerticalLocation>> getHostedDockers(DockerManager manager, Docker.Side side) {
		Optional<String[]> hostedDockers = swing.data().section(HOSTED_DOCKERS).getArray(side.name());

		if (hostedDockers.isEmpty()) {
			return Optional.empty();
		}

		Map<Docker, Docker.VerticalLocation> dockers = new HashMap<>();

		for (String dockInfo : hostedDockers.get()) {
			if (!dockInfo.isBlank()) {
				String[] split = dockInfo.split(PAIR_SEPARATOR);

				try {
					Docker.VerticalLocation location = Docker.VerticalLocation.valueOf(split[1]);
					Docker docker = manager.getDocker(split[0]);

					dockers.put(docker, location);
				} catch (Exception e) {
					Logger.error("failed to read docker state for {}, ignoring! ({})", dockInfo, e.getMessage());
				}
			}
		}

		return Optional.of(dockers);
	}

	public static Docker.Location getButtonLocation(Docker docker) {
		String location = swing.data().section(DOCKER_BUTTON_LOCATIONS).setIfAbsentString(docker.getId(), docker.getPreferredButtonLocation().toString());

		try {
			return Docker.Location.parse(location);
		} catch (Exception e) {
			Logger.error("invalid docker button location: {}, ignoring!", location);
			setDockerButtonLocation(docker, docker.getPreferredButtonLocation());
			return docker.getPreferredButtonLocation();
		}
	}
}
