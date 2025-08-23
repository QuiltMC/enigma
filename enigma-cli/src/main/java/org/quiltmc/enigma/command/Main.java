package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableMap;
import org.quiltmc.enigma.api.Enigma;
import org.tinylog.Logger;

import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

public class Main {
	private static final ImmutableMap<String, Command<?, ?>> COMMANDS = Stream
			.of(
				DeobfuscateCommand.INSTANCE,
				DecompileCommand.INSTANCE,
				ConvertMappingsCommand.INSTANCE,
				ComposeMappingsCommand.INSTANCE,
				InvertMappingsCommand.INSTANCE,
				CheckMappingsCommand.INSTANCE,
				MapSpecializedMethodsCommand.INSTANCE,
				InsertProposedMappingsCommand.INSTANCE,
				DropInvalidMappingsCommand.INSTANCE,
				FillClassMappingsCommand.INSTANCE,
				HelpCommand.INSTANCE,
				PrintStatsCommand.INSTANCE,
				SearchMappingsCommand.INSTANCE
			)
			.collect(toImmutableMap(Command::getName, Function.identity()));

	public static void main(String... args) {
		try {
			// process the command
			if (args.length < 1) {
				throw new IllegalArgumentException("Requires a command");
			}

			String command = args[0].toLowerCase(Locale.ROOT);

			Command<?, ?> cmd = COMMANDS.get(command);
			if (cmd == null) {
				throw new IllegalArgumentException("Command not recognized: " + command);
			}

			String[] cmdArgs = new String[args.length - 1];
			System.arraycopy(args, 1, cmdArgs, 0, args.length - 1);

			try {
				cmd.run(cmdArgs);
			} catch (Exception ex) {
				throw new CommandErrorHelpException(cmd, ex);
			}
		} catch (Command.HelpException ex) {
			Logger.error(ex);
			logEnigmaInfo();
			Logger.info("Command {} has encountered an error! Usage:", ex.getCommand().getName());
			StringBuilder help = new StringBuilder();
			ex.getCommand().appendHelp(help);
			Logger.info(help.toString());
			System.exit(1);
		} catch (IllegalArgumentException ex) {
			Logger.error(ex);
			printHelp();
			System.exit(1);
		}
	}

	public static ImmutableMap<String, Command<?, ?>> getCommands() {
		return COMMANDS;
	}

	private static void printHelp() {
		logEnigmaInfo();

		StringBuilder help = new StringBuilder();
		help.append("""
				Usage:
				\tjava -jar enigma.jar <command> <args>
				\twhere <command> is one of:""");

		for (Command<?, ?> command : COMMANDS.values()) {
			command.appendHelp(help);
		}
	}

	private static void logEnigmaInfo() {
		Logger.info("{} - {}", Enigma.NAME, Enigma.VERSION);
	}

	private static final class CommandErrorHelpException extends Command.HelpException {
		final Command<?, ?> command;

		CommandErrorHelpException(Command<?, ?> command, Throwable cause) {
			super(cause);
			this.command = command;
		}

		@Override
		public Command<?, ?> getCommand() {
			return this.command;
		}
	}
}
