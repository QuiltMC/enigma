package org.quiltmc.enigma;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFormat;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestTranslator {
	public static final Path JAR = TestUtil.obfJar("translation");

	private static Enigma enigma;
	private static EnigmaProject project;
	private static EntryTree<EntryMapping> mappings;
	private static Translator deobfuscator;

	@BeforeAll
	public static void beforeClass() throws Exception {
		enigma = Enigma.create();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		mappings = MappingFormat.ENIGMA_FILE.read(
				TestUtil.getResource("/translation.mappings"),
				ProgressListener.createEmpty());
		project.setMappings(mappings, ProgressListener.createEmpty());
		deobfuscator = project.getRemapper().getDeobfuscator();
	}

	@Test
	public void basicClasses() {
		this.assertMapping(TestEntryFactory.newClass("a"), TestEntryFactory.newClass("deobf/A_Basic"));
		this.assertMapping(TestEntryFactory.newClass("b"), TestEntryFactory.newClass("deobf/B_BaseClass"));
		this.assertMapping(TestEntryFactory.newClass("c"), TestEntryFactory.newClass("deobf/C_SubClass"));
	}

	@Test
	public void basicFields() {
		this.assertMapping(TestEntryFactory.newField("a", "a", "I"), TestEntryFactory.newField("deobf/A_Basic", "f1", "I"));
		this.assertMapping(TestEntryFactory.newField("a", "a", "F"), TestEntryFactory.newField("deobf/A_Basic", "f2", "F"));
		this.assertMapping(TestEntryFactory.newField("a", "a", "Ljava/lang/String;"), TestEntryFactory.newField("deobf/A_Basic", "f3", "Ljava/lang/String;"));
	}

	@Test
	public void basicMethods() {
		this.assertMapping(TestEntryFactory.newMethod("a", "a", "()V"), TestEntryFactory.newMethod("deobf/A_Basic", "m1", "()V"));
		this.assertMapping(TestEntryFactory.newMethod("a", "a", "()I"), TestEntryFactory.newMethod("deobf/A_Basic", "m2", "()I"));
		this.assertMapping(TestEntryFactory.newMethod("a", "a", "(I)V"), TestEntryFactory.newMethod("deobf/A_Basic", "m3", "(I)V"));
		this.assertMapping(TestEntryFactory.newMethod("a", "a", "(I)I"), TestEntryFactory.newMethod("deobf/A_Basic", "m4", "(I)I"));
	}

	// TODO: basic constructors

	@Test
	public void inheritanceFields() {
		this.assertMapping(TestEntryFactory.newField("b", "a", "I"), TestEntryFactory.newField("deobf/B_BaseClass", "f1", "I"));
		this.assertMapping(TestEntryFactory.newField("b", "a", "C"), TestEntryFactory.newField("deobf/B_BaseClass", "f2", "C"));
		this.assertMapping(TestEntryFactory.newField("c", "b", "I"), TestEntryFactory.newField("deobf/C_SubClass", "f3", "I"));
		this.assertMapping(TestEntryFactory.newField("c", "c", "I"), TestEntryFactory.newField("deobf/C_SubClass", "f4", "I"));
	}

	@Test
	public void inheritanceFieldsShadowing() {
		this.assertMapping(TestEntryFactory.newField("c", "b", "C"), TestEntryFactory.newField("deobf/C_SubClass", "f2", "C"));
	}

	@Test
	public void inheritanceFieldsBySubClass() {
		this.assertMapping(TestEntryFactory.newField("c", "a", "I"), TestEntryFactory.newField("deobf/C_SubClass", "f1", "I"));
		// NOTE: can't reference b.C by subclass since it's shadowed
	}

	@Test
	public void inheritanceMethods() {
		this.assertMapping(TestEntryFactory.newMethod("b", "a", "()I"), TestEntryFactory.newMethod("deobf/B_BaseClass", "m1", "()I"));
		this.assertMapping(TestEntryFactory.newMethod("b", "b", "()I"), TestEntryFactory.newMethod("deobf/B_BaseClass", "m2", "()I"));
		this.assertMapping(TestEntryFactory.newMethod("c", "c", "()I"), TestEntryFactory.newMethod("deobf/C_SubClass", "m3", "()I"));
	}

	@Test
	public void inheritanceMethodsOverrides() {
		this.assertMapping(TestEntryFactory.newMethod("c", "a", "()I"), TestEntryFactory.newMethod("deobf/C_SubClass", "m1", "()I"));
	}

	@Test
	public void inheritanceMethodsBySubClass() {
		this.assertMapping(TestEntryFactory.newMethod("c", "b", "()I"), TestEntryFactory.newMethod("deobf/C_SubClass", "m2", "()I"));
	}

	@Test
	public void innerClasses() {
		// classes
		this.assertMapping(TestEntryFactory.newClass("g"), TestEntryFactory.newClass("deobf/G_OuterClass"));
		this.assertMapping(TestEntryFactory.newClass("g$a"), TestEntryFactory.newClass("deobf/G_OuterClass$A_InnerClass"));
		this.assertMapping(TestEntryFactory.newClass("g$a$a"), TestEntryFactory.newClass("deobf/G_OuterClass$A_InnerClass$A_InnerInnerClass"));
		this.assertMapping(TestEntryFactory.newClass("g$b"), TestEntryFactory.newClass("deobf/G_OuterClass$b"));
		this.assertMapping(TestEntryFactory.newClass("g$b$a"), TestEntryFactory.newClass("deobf/G_OuterClass$b$A_NamedInnerClass"));

		// fields
		this.assertMapping(TestEntryFactory.newField("g$a", "a", "I"), TestEntryFactory.newField("deobf/G_OuterClass$A_InnerClass", "f1", "I"));
		this.assertMapping(TestEntryFactory.newField("g$a", "a", "Ljava/lang/String;"), TestEntryFactory.newField("deobf/G_OuterClass$A_InnerClass", "f2", "Ljava/lang/String;"));
		this.assertMapping(TestEntryFactory.newField("g$a$a", "a", "I"), TestEntryFactory.newField("deobf/G_OuterClass$A_InnerClass$A_InnerInnerClass", "f3", "I"));
		this.assertMapping(TestEntryFactory.newField("g$b$a", "a", "I"), TestEntryFactory.newField("deobf/G_OuterClass$b$A_NamedInnerClass", "f4", "I"));

		// methods
		this.assertMapping(TestEntryFactory.newMethod("g$a", "a", "()V"), TestEntryFactory.newMethod("deobf/G_OuterClass$A_InnerClass", "m1", "()V"));
		this.assertMapping(TestEntryFactory.newMethod("g$a$a", "a", "()V"), TestEntryFactory.newMethod("deobf/G_OuterClass$A_InnerClass$A_InnerInnerClass", "m2", "()V"));
	}

	@Test
	public void namelessClass() {
		this.assertMapping(TestEntryFactory.newClass("h"), TestEntryFactory.newClass("h"));
	}

	@Test
	public void testGenerics() {
		// classes
		this.assertMapping(TestEntryFactory.newClass("i"), TestEntryFactory.newClass("deobf/I_Generics"));
		this.assertMapping(TestEntryFactory.newClass("i$a"), TestEntryFactory.newClass("deobf/I_Generics$A_Type"));
		this.assertMapping(TestEntryFactory.newClass("i$b"), TestEntryFactory.newClass("deobf/I_Generics$B_Generic"));

		// fields
		this.assertMapping(TestEntryFactory.newField("i", "a", "Ljava/util/List;"), TestEntryFactory.newField("deobf/I_Generics", "f1", "Ljava/util/List;"));
		this.assertMapping(TestEntryFactory.newField("i", "b", "Ljava/util/List;"), TestEntryFactory.newField("deobf/I_Generics", "f2", "Ljava/util/List;"));
		this.assertMapping(TestEntryFactory.newField("i", "a", "Ljava/util/Map;"), TestEntryFactory.newField("deobf/I_Generics", "f3", "Ljava/util/Map;"));
		this.assertMapping(TestEntryFactory.newField("i$b", "a", "Ljava/lang/Object;"), TestEntryFactory.newField("deobf/I_Generics$B_Generic", "f4", "Ljava/lang/Object;"));
		this.assertMapping(TestEntryFactory.newField("i", "a", "Li$b;"), TestEntryFactory.newField("deobf/I_Generics", "f5", "Ldeobf/I_Generics$B_Generic;"));
		this.assertMapping(TestEntryFactory.newField("i", "b", "Li$b;"), TestEntryFactory.newField("deobf/I_Generics", "f6", "Ldeobf/I_Generics$B_Generic;"));

		// methods
		this.assertMapping(TestEntryFactory.newMethod("i$b", "a", "()Ljava/lang/Object;"), TestEntryFactory.newMethod("deobf/I_Generics$B_Generic", "m1", "()Ljava/lang/Object;"));
	}

	private void assertMapping(Entry<?> obf, Entry<?> deobf) {
		TranslateResult<? extends Entry<?>> result = deobfuscator.extendedTranslate(obf);
		assertThat(result, is(notNullValue()));
		assertThat(result.getValue(), is(deobf));

		String deobfName = result.getValue().getName();
		if (deobfName != null) {
			assertThat(deobfName, is(deobf.getName()));
		}
	}
}
