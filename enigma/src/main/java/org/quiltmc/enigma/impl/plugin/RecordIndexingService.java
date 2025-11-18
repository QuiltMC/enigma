package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Indexes records, finding component getters and their corresponding fields.
 *
 * <p> While component fields can be reliably indexed, there can be uncertainty in determining their corresponding
 * getters. Some getters can be definitively determined, some are classified as 'probable getters'
 * (probabilistically determined), and some cannot be determined at all.
 *
 * <p> {@link RecordIndexingService} provides separate methods for accessing getters that are definitive, probabilistic,
 * or either.<br>
 * Either:
 * <ul>
 *     <li> {@link #getComponentGetter(FieldEntry)}
 *     <li> {@link #getComponentField(MethodEntry)}
 *     <li> {@link #streamComponentMethods(ClassEntry)}
 * </ul>
 * Definite:
 * <ul>
 *     <li> {@link #getDefiniteComponentGetter(FieldEntry)}
 *     <li> {@link #getDefiniteComponentField(MethodEntry)}
 *     <li> {@link #streamDefiniteComponentMethods(ClassEntry)}
 * </ul>
 * Probable:
 * <ul>
 *     <li> {@link #getProbableComponentGetter(FieldEntry)}
 *     <li> {@link #getProbableComponentField(MethodEntry)}
 *     <li> {@link #streamProbableComponentMethods(ClassEntry)}
 * </ul>
 */
public class RecordIndexingService implements JarIndexerService {
	public static final String ID = "enigma:record_component_indexer";

	private final RecordIndexingVisitor visitor;

	RecordIndexingService(RecordIndexingVisitor visitor) {
		this.visitor = visitor;
	}

	/**
	 * @return the {@link MethodEntry} representing the getter of the passed {@code componentField},
	 * or {@code null} if the passed {@code componentField} is not a record component field
	 * or if its getter could not be determined; returns both
	 * {@linkplain #getDefiniteComponentGetter(FieldEntry) definitive} and
	 * {@linkplain #getProbableComponentGetter(FieldEntry) probable} getters
	 */
	@Nullable
	public MethodEntry getComponentGetter(FieldEntry componentField) {
		return this.visitor.getComponentGetter(componentField);
	}

	/**
	 * @return the {@link FieldEntry} representing the field of the passed {@code componentGetter},
	 * or {@code null} if the passed {@code componentGetter} is not a record component getter
	 * or if its field could not be determined; returns both
	 * {@linkplain #getDefiniteComponentField(MethodEntry) definitive} and
	 * {@linkplain #getProbableComponentField(MethodEntry) probable} fields
	 */
	@Nullable
	public FieldEntry getComponentField(MethodEntry componentGetter) {
		return this.visitor.getComponentField(componentGetter);
	}

	/**
	 * @return the definitive {@link MethodEntry} representing the getter of the passed {@code componentField},
	 * or {@code null} if the passed {@code componentField} is not a record component field
	 * or if its getter could not be definitively determined
	 */
	@Nullable
	public MethodEntry getDefiniteComponentGetter(FieldEntry componentField) {
		return this.visitor.getDefiniteComponentGetter(componentField);
	}

	/**
	 * @return the definitive {@link FieldEntry} representing the field of the passed {@code componentGetter},
	 * or {@code null} if the passed {@code componentGetter} is not a record component getter
	 * or if its field could not be definitively determined
	 */
	@Nullable
	public FieldEntry getDefiniteComponentField(MethodEntry componentGetter) {
		return this.visitor.getDefiniteComponentField(componentGetter);
	}

	/**
	 * @return the probable {@link MethodEntry} representing the getter of the passed {@code componentField},
	 * or {@code null} if the passed {@code componentField} is not a record component field
	 * or if its getter was not probabilistically determined;
	 * does not include {@linkplain #getDefiniteComponentGetter(FieldEntry) definitive} getters
	 */
	@Nullable
	public MethodEntry getProbableComponentGetter(FieldEntry componentField) {
		return this.visitor.getProbableComponentGetter(componentField);
	}

	/**
	 * @return the probably {@link FieldEntry} representing the field of the passed {@code componentGetter},
	 * or {@code null} if the passed {@code componentGetter} is not a record component getter
	 * or if its field was not probabilistically determined;
	 * does not include {@linkplain #getDefiniteComponentField(MethodEntry) definitive} fields
	 */
	@Nullable
	public FieldEntry getProbableComponentField(MethodEntry componentGetter) {
		return this.visitor.getProbableComponentField(componentGetter);
	}

	/**
	 * @return a {@link Stream} of component fields of the passed {@code recordEntry};
	 * there's no uncertainty in getter field determination, so all fields are always included;
	 * if the passed {@code recordEntry} does not represent a record, the stream is empty
	 */
	public Stream<FieldEntry> streamComponentFields(ClassEntry recordEntry) {
		return this.visitor.streamComponentFields(recordEntry);
	}

	/**
	 * @return a {@link Stream} of component getter methods of the passed {@code recordEntry};
	 * includes both {@linkplain #streamDefiniteComponentMethods(ClassEntry) definitive} and
	 * {@linkplain #streamProbableComponentMethods(ClassEntry) probable} getters;
	 * if the passed {@code recordEntry} does not represent a record, the stream is empty
	 */
	public Stream<MethodEntry> streamComponentMethods(ClassEntry recordEntry) {
		return this.visitor.streamComponentMethods(recordEntry);
	}

	/**
	 * @return a {@link Stream} of definitive component getter methods of the passed {@code recordEntry};
	 * if the passed {@code recordEntry} does not represent a record, the stream is empty
	 */
	public Stream<MethodEntry> streamDefiniteComponentMethods(ClassEntry recordEntry) {
		return this.visitor.streamDefiniteComponentMethods(recordEntry);
	}

	/**
	 * @return a {@link Stream} of probable component getter methods of the passed {@code recordEntry};
	 * does not include {@linkplain #streamDefiniteComponentMethods(ClassEntry) definitive} getters;
	 * if the passed {@code recordEntry} does not represent a record, the stream is empty
	 */
	public Stream<MethodEntry> streamProbableComponentMethods(ClassEntry recordEntry) {
		return this.visitor.streamProbableComponentMethods(recordEntry);
	}

	@Override
	public void acceptJar(Set<String> scope, ProjectClassProvider classProvider, JarIndex jarIndex) {
		for (String className : scope) {
			ClassNode node = classProvider.get(className);
			if (node != null) {
				node.accept(this.visitor);
			}
		}
	}

	@Override
	public String getId() {
		return ID;
	}
}
