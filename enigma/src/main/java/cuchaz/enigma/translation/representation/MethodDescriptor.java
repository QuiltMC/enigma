package cuchaz.enigma.translation.representation;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class MethodDescriptor implements Translatable {
	private final List<ArgumentDescriptor> argumentDescs;
	private TypeDescriptor returnDesc;

	public MethodDescriptor(String desc) {
		try {
			this.argumentDescs = new ArrayList<>();
			int i = 0;
			while (i < desc.length()) {
				char c = desc.charAt(i);
				if (c == '(') {
					assert (this.argumentDescs.isEmpty());
					assert (this.returnDesc == null);
					i++;
				} else if (c == ')') {
					i++;
					break;
				} else {
					String type = TypeDescriptor.parseFirst(desc.substring(i));
					this.argumentDescs.add(new ArgumentDescriptor(type, ParameterAccessFlags.DEFAULT));
					i += type.length();
				}
			}

			this.returnDesc = new TypeDescriptor(TypeDescriptor.parseFirst(desc.substring(i)));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to parse method descriptor: " + desc, ex);
		}
	}

	public MethodDescriptor(List<ArgumentDescriptor> argumentDescs, TypeDescriptor returnDesc) {
		this.argumentDescs = argumentDescs;
		this.returnDesc = returnDesc;
	}

	public List<ArgumentDescriptor> getArgumentDescs() {
		return this.argumentDescs;
	}

	public List<TypeDescriptor> getTypeDescs() {
		return new ArrayList<>(this.argumentDescs);
	}

	public TypeDescriptor getReturnDesc() {
		return this.returnDesc;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		for (TypeDescriptor desc : this.argumentDescs) {
			buf.append(desc);
		}

		buf.append(")");
		buf.append(this.returnDesc);
		return buf.toString();
	}

	public Iterable<TypeDescriptor> types() {
		List<TypeDescriptor> descs = new ArrayList<>(this.argumentDescs);
		descs.add(this.returnDesc);
		return descs;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodDescriptor descriptor && this.equals(descriptor);
	}

	public boolean equals(MethodDescriptor other) {
		return this.argumentDescs.equals(other.argumentDescs) && this.returnDesc.equals(other.returnDesc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.argumentDescs.hashCode(), this.returnDesc.hashCode());
	}

	public boolean hasClass(ClassEntry classEntry) {
		for (TypeDescriptor desc : this.types()) {
			if (desc.containsType() && desc.getTypeEntry().equals(classEntry)) {
				return true;
			}
		}

		return false;
	}

	public MethodDescriptor remap(UnaryOperator<String> remapper) {
		List<ArgumentDescriptor> argumentDescriptors = new ArrayList<>(this.argumentDescs.size());
		for (ArgumentDescriptor desc : this.argumentDescs) {
			argumentDescriptors.add(new ArgumentDescriptor(desc.remap(remapper).desc, desc.getAccess()));
		}

		return new MethodDescriptor(argumentDescriptors, this.returnDesc.remap(remapper));
	}

	@Override
	public TranslateResult<MethodDescriptor> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		List<ArgumentDescriptor> translatedArguments = new ArrayList<>(this.argumentDescs.size());
		for (ArgumentDescriptor argument : this.argumentDescs) {
			translatedArguments.add(translator.translate(argument));
		}

		return TranslateResult.ungrouped(new MethodDescriptor(translatedArguments, translator.translate(this.returnDesc)));
	}

	public boolean canConflictWith(MethodDescriptor descriptor) {
		return descriptor.argumentDescs.equals(this.argumentDescs);
	}
}
