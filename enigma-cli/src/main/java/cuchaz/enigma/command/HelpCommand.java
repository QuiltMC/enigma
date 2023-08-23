package cuchaz.enigma.command;

import org.tinylog.Logger;

import java.util.Collection;

public class HelpCommand extends Command {
	protected HelpCommand() {
		super();
	}

	@Override
	public void run(String... args) throws Exception {
		StringBuilder help = new StringBuilder();
		Collection<Command> commands = Main.getCommands().values();

		help.append("Supported commands:").append("\n");
		for (Command command : commands) {
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
