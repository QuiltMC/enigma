package cuchaz.enigma.source.procyon.transformers;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.CastExpression;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

/**
 * Created by Thiakil on 11/07/2018.
 */
public class RemoveObjectCasts implements IAstTransform {
	private final DecompilerContext _context;

	public RemoveObjectCasts(DecompilerContext context) {
		this._context = context;
	}

	@Override
	public void run(AstNode compilationUnit) {
		compilationUnit.acceptVisitor(new Visitor(this._context), null);
	}

	private static final class Visitor extends ContextTrackingVisitor<Void> {
		private Visitor(DecompilerContext context) {
			super(context);
		}

		@Override
		public Void visitCastExpression(CastExpression node, Void data) {
			if (node.getType().toTypeReference().equals(BuiltinTypes.Object)) {
				node.replaceWith(node.getExpression());
			}

			return super.visitCastExpression(node, data);
		}
	}
}
