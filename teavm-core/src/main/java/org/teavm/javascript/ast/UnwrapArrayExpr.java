package org.teavm.javascript.ast;

import java.util.Map;
import org.teavm.model.instructions.ArrayElementType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class UnwrapArrayExpr extends Expr {
    private ArrayElementType elementType;
    private Expr array;

    public UnwrapArrayExpr(ArrayElementType elementType) {
        this.elementType = elementType;
    }

    public ArrayElementType getElementType() {
        return elementType;
    }

    public Expr getArray() {
        return array;
    }

    public void setArray(Expr array) {
        this.array = array;
    }

    @Override
    public void acceptVisitor(ExprVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected UnwrapArrayExpr clone(Map<Expr, Expr> cache) {
        UnwrapArrayExpr copy = (UnwrapArrayExpr)cache.get(this);
        if (copy == null) {
            copy = new UnwrapArrayExpr(elementType);
            copy.array = array != null ? array.clone(cache) : null;
            cache.put(copy, copy);
        }
        return copy;
    }
}
