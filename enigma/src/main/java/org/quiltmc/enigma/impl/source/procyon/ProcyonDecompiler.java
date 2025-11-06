package org.quiltmc.enigma.impl.source.procyon;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.java.BraceStyle;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.impl.source.procyon.transformer.AddJavadocsAstTransform;
import org.quiltmc.enigma.impl.source.procyon.transformer.DropImportAstTransform;
import org.quiltmc.enigma.impl.source.procyon.transformer.DropVarModifiersAstTransform;
import org.quiltmc.enigma.impl.source.procyon.transformer.InvalidIdentifierFix;
import org.quiltmc.enigma.impl.source.procyon.transformer.Java8Generics;
import org.quiltmc.enigma.impl.source.procyon.transformer.ObfuscatedEnumSwitchRewriterTransform;
import org.quiltmc.enigma.impl.source.procyon.transformer.RemoveObjectCasts;
import org.quiltmc.enigma.impl.source.procyon.transformer.VarargsFixer;
import org.quiltmc.enigma.util.AsmUtil;

public class ProcyonDecompiler implements Decompiler {
	private final SourceSettings settings;
	private final DecompilerSettings decompilerSettings;
	private final MetadataSystem metadataSystem;

	public ProcyonDecompiler(ClassProvider classProvider, SourceSettings settings) {
		ITypeLoader typeLoader = (name, buffer) -> {
			ClassNode node = classProvider.get(name);

			if (node == null) {
				return false;
			}

			byte[] data = AsmUtil.nodeToBytes(node);
			buffer.reset(data.length);
			System.arraycopy(data, 0, buffer.array(), buffer.position(), data.length);
			buffer.position(0);
			return true;
		};

		this.metadataSystem = new MetadataSystem(typeLoader);
		this.metadataSystem.setEagerMethodLoadingEnabled(true);

		this.decompilerSettings = DecompilerSettings.javaDefaults();
		this.decompilerSettings.setMergeVariables(getSystemPropertyAsBoolean("enigma.mergeVariables", true));
		this.decompilerSettings.setForceExplicitImports(getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
		this.decompilerSettings.setForceExplicitTypeArguments(getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
		this.decompilerSettings.setShowDebugLineNumbers(getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
		this.decompilerSettings.setShowSyntheticMembers(getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));
		this.decompilerSettings.setTypeLoader(typeLoader);

		JavaFormattingOptions formattingOptions = this.decompilerSettings.getJavaFormattingOptions();
		formattingOptions.ClassBraceStyle = BraceStyle.EndOfLine;
		formattingOptions.InterfaceBraceStyle = BraceStyle.EndOfLine;
		formattingOptions.EnumBraceStyle = BraceStyle.EndOfLine;

		this.settings = settings;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		TypeReference type = this.metadataSystem.lookupType(className);
		if (type == null) {
			throw new Error(String.format("Unable to find desc: %s", className));
		}

		TypeDefinition resolvedType = type.resolve();

		DecompilerContext context = new DecompilerContext();
		context.setCurrentType(resolvedType);
		context.setSettings(this.decompilerSettings);

		AstBuilder builder = new AstBuilder(context);
		builder.addType(resolvedType);
		builder.runTransformations(null);
		CompilationUnit source = builder.getCompilationUnit();

		new ObfuscatedEnumSwitchRewriterTransform(context).run(source);
		new VarargsFixer(context).run(source);
		new RemoveObjectCasts(context).run(source);
		new Java8Generics().run(source);
		new InvalidIdentifierFix().run(source);
		if (this.settings.removeImports()) DropImportAstTransform.INSTANCE.run(source);
		if (this.settings.removeVariableFinal()) DropVarModifiersAstTransform.INSTANCE.run(source);
		source.acceptVisitor(new InsertParenthesesVisitor(), null);

		if (remapper != null) {
			new AddJavadocsAstTransform(remapper).run(source);
		}

		return new ProcyonSource(source, this.decompilerSettings);
	}

	private static boolean getSystemPropertyAsBoolean(String property, boolean defValue) {
		String value = System.getProperty(property);
		return value == null ? defValue : Boolean.parseBoolean(value);
	}
}
