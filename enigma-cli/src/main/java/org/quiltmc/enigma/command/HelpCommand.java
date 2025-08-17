package org.quiltmc.enigma.command;

import org.quiltmc.enigma.command.ArgsParser.Empty;
import org.tinylog.Logger;

import java.util.Collection;

public final class HelpCommand extends Command<Empty, Empty> {
	public static final HelpCommand INSTANCE = new HelpCommand();

	private HelpCommand() {
		super(Empty.PARSER, Empty.PARSER);
	}

	@Override
	void runImpl(Empty required, Empty optional) throws Exception {
		StringBuilder help = new StringBuilder();
		Collection<Command<?, ?>> commands = Main.getCommands().values();

		help.append("Supported commands:").append("\n");
		for (Command<?, ?> command : commands) {
			help.append("- ").append(command.getName()).append("\n");
			help.append("\t").append(command.getDescription()).append("\n");
		}

		help.append("Run any command with no arguments in order to see all possible arguments for that command.");
		Logger.info(help.toString());
	}

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getDescription() {
		return "Provides a list of commands, along with some short descriptions.";
	}
}
