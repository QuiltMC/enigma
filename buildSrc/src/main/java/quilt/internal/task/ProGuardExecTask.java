package quilt.internal.task;

import groovy.lang.Closure;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public abstract class ProGuardExecTask extends JavaExec {
	@Input
	public abstract Property<String> getConfFile();

	@Input
	public abstract Property<String> getLibraryJars();

	@InputFile
	public abstract RegularFileProperty getInJar();

	@OutputFile
	public abstract RegularFileProperty getOutJar();

	public ProGuardExecTask() {
		this.getMainClass().convention("proguard.ProGuard");
	}

	@Override
	@NotNull
	public Task configure(@NotNull Closure closure) {
		final Task task = super.configure(closure);

		if (streamProperties().allMatch(Provider::isPresent)) {
			final List<String> currentArgs = getArgs();
			if (currentArgs != null) {
				currentArgs.clear();
			}

			args(
				"\"@" + getConfFile().get() + "\"",
				"-injars", getInJar().get().getAsFile(),
				"-libraryjars", getLibraryJars().get(),
				"-outjars", getOutJar().get().getAsFile()
				// '-printmapping', file("build/test-obf/${name}.txt")
			);
		}

		return task;
	}

	private Stream<Property<?>> streamProperties() {
		return Stream.of(getConfFile(), getLibraryJars(), getInJar(), getOutJar());
	}
}
