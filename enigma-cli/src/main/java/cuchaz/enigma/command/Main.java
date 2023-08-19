package cuchaz.enigma.command;

import cuchaz.enigma.Enigma;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Main {
	private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

	public static void main(String... args) {
		try {
			// process the command
			if (args.length < 1) {
				throw new IllegalArgumentException("Requires a command");
			}

			String command = args[0].toLowerCase(Locale.ROOT);

			Command cmd = COMMANDS.get(command);
			if (cmd == null) {
				throw new IllegalArgumentException("Command not recognized: " + command);
			}

			if (!cmd.checkArgumentCount(args.length - 1)) {
				throw new CommandHelpException(cmd);
			}

			String[] cmdArgs = new String[args.length - 1];
			System.arraycopy(args, 1, cmdArgs, 0, args.length - 1);

			try {
				cmd.run(cmdArgs);
			} catch (Exception ex) {
				throw new CommandHelpException(cmd, ex);
			}
		} catch (CommandHelpException ex) {
			Logger.error(ex);
			logEnigmaInfo();
			Logger.info("Command {} has encountered an error! Usage:", ex.command.getName());
			StringBuilder help = new StringBuilder();
			appendHelp(ex.command, help);
			Logger.info(help.toString());
			System.exit(1);
		} catch (IllegalArgumentException ex) {
			Logger.error(ex);
			printHelp();
			System.exit(1);
		}
	}

	public static Map<String, Command> getCommands() {
		return COMMANDS;
	}

	private static void printHelp() {
		logEnigmaInfo();

		StringBuilder help = new StringBuilder();
		help.append("""
				Usage:
				\tjava -cp enigma.jar cuchaz.enigma.command.CommandMain <command> <args>
				\twhere <command> is one of:""");

		for (Command command : COMMANDS.values()) {
			appendHelp(command, help);
		}
	}

	private static void appendHelp(Command command, StringBuilder builder) {
		builder.append(String.format("\t\t%s %s", command.getName(), command.getUsage())).append("\n");

		if (!command.requiredArguments.isEmpty()) {
			builder.append("Arguments:").append("\n");
			int argIndex = 0;
			for (int j = 0; j < command.requiredArguments.size(); j++) {
				Argument argument = command.requiredArguments.get(j);
				appendHelp(argument, argIndex, builder);
				argIndex++;
			}

			if (!command.optionalArguments.isEmpty()) {
				builder.append("\n").append("Optional arguments:").append("\n");
				for (int i = 0; i < command.optionalArguments.size(); i++) {
					Argument argument = command.optionalArguments.get(i);
					appendHelp(argument, argIndex, builder);
					argIndex++;
				}
			}
		}
	}

	private static void appendHelp(Argument argument, int index, StringBuilder builder) {
		builder.append(String.format("Argument %s: %s", index, argument.getDisplayForm())).append("\n");
		builder.append(argument.getExplanation()).append("\n");
	}

	private static void register(Command command) {
		Command old = COMMANDS.put(command.getName(), command);
		if (old != null) {
			Logger.warn("Command {} with name {} has been substituted by {}", old, command.getName(), command);
		}
	}

	private static void logEnigmaInfo() {
		Logger.info("{} - {}", Enigma.NAME, Enigma.VERSION);
	}

	static {
		register(new DeobfuscateCommand());
		register(new DecompileCommand());
		register(new ConvertMappingsCommand());
		register(new ComposeMappingsCommand());
		register(new InvertMappingsCommand());
		register(new CheckMappingsCommand());
		register(new MapSpecializedMethodsCommand());
		register(new InsertProposedMappingsCommand());
		register(new DropInvalidMappingsCommand());
		register(new FillClassMappingsCommand());
		register(new HelpCommand());
	}

	private static final class CommandHelpException extends IllegalArgumentException {
		final Command command;

		CommandHelpException(Command command) {
			this.command = command;
		}

		CommandHelpException(Command command, Throwable cause) {
			super(cause);
			this.command = command;
		}
	}
}
