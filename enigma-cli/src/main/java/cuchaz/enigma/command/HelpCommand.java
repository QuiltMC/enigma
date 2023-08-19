package cuchaz.enigma.command;

import org.tinylog.Logger;

import java.util.Collection;

public class HelpCommand extends Command {
	protected HelpCommand() {
		super("help");
	}

	@Override
	public void run(String... args) throws Exception {
		StringBuilder help = new StringBuilder();
		Collection<Command> commands = Main.getCommands().values();

		help.append("Supported commands:").append("\n");
		for (Command command : commands) {
			help.append("- ").append(command.name).append("\n");
			// todo iota please help!
			//help.append("\t").append(command.description.append("\n");
		}

		help.append("Run any command with no arguments in order to see all possible arguments for that command.");
		Logger.info(help.toString());
	}
}
