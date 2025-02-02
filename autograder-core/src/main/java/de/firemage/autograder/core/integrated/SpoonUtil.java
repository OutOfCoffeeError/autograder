package de.firemage.autograder.core.integrated;

import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtLiteralImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SpoonUtil {
    private SpoonUtil() {

    }

    public static boolean isString(CtTypeReference<?> type) {
        return type.getQualifiedName().equals("java.lang.String");
    }

    public static Optional<CtTypeReference<?>> isToStringCall(CtExpression<?> expression) {
        if (!SpoonUtil.isString(expression.getType())) {
            return Optional.empty();
        }

        if (expression instanceof CtInvocation<?> invocation &&
            invocation.getExecutable().getSignature().equals("toString()")) {
            return Optional.of(invocation.getTarget().getType());
        } else {
            return Optional.empty();
        }
    }

    public static boolean isNullLiteral(CtExpression<?> expression) {
        return expression instanceof CtLiteral<?> literal && literal.getValue() == null;
    }

    public static boolean isIntegerLiteral(CtExpression<?> expression, int value) {
        return expression instanceof CtLiteral<?> literal && literal.getValue().equals(value);
    }

    public static boolean isStringLiteral(CtExpression<?> expression, String value) {
        return expression instanceof CtLiteral<?> literal && literal.getValue() != null &&
               literal.getValue().equals(value);
    }

    public static Optional<Boolean> tryGetBooleanLiteral(CtExpression<?> expression) {
        if (expression instanceof CtLiteral<?> literal
            && literal.getValue() != null
            && (literal.getType().getQualifiedName().equals("boolean") ||
                literal.getType().getQualifiedName().equals("java.lang.Boolean"))) {

            return Optional.of((Boolean) literal.getValue());
        } else {
            return Optional.empty();
        }
    }

    // equals impl of CtLiteral seems to be broken
    public static boolean areLiteralsEqual(
        CtLiteral<?> left,
        CtLiteral<?> right
    ) {
        if (left == null && right == null) {
            return true;
        } else if (left == null || right == null) {
            return false;
        }

        if (left.getValue() == null) {
            return right.getValue() == null;
        } else if (right.getValue() == null) {
            return false;
        }

        if (left.getValue() instanceof Character l && right.getValue() instanceof Character r) {
            return l.equals(r);
        } else if (left.getValue() instanceof Number l && right.getValue() instanceof Character r) {
            return l.intValue() == (int) r;
        } else if (left.getValue() instanceof Character l && right.getValue() instanceof Number r) {
            return (int) l == r.intValue();
        }

        if (!(left.getValue() instanceof Number valLeft)
            || !(right.getValue() instanceof Number valRight)) {
            return left.getValue() == right.getValue() || left.getValue().equals(right.getValue());
        }

        if (valLeft instanceof Float || valLeft instanceof Double || valRight instanceof Float
            || valRight instanceof Double) {
            return valLeft.doubleValue() == valRight.doubleValue();
        }

        return valLeft.longValue() == valRight.longValue();
    }

    /**
     * Makes a new literal with the given value.
     *
     * @param value the value of the literal
     * @param <T>   the type of the value
     *
     * @return a new literal with the given value, note that the base is not set
     */
    public static <T> CtLiteral<T> makeLiteral(T value) {
        CtLiteral<T> literal = new CtLiteralImpl<>();
        literal.setValue(value);
        return literal;
    }

    /**
     * Returns the default value of the given type.
     *
     * @param ty  a reference to the type
     * @param <T> the type of the value
     *
     * @return the default value of the given type
     */
    public static <T> CtLiteral<T> getDefaultValue(CtTypeReference<T> ty) {
        if (ty.isPrimitive()) {
            return (CtLiteral<T>) Map.ofEntries(
                Map.entry("int", makeLiteral(0)),
                Map.entry("double", makeLiteral(0.0d)),
                Map.entry("float", makeLiteral(0.0f)),
                Map.entry("long", makeLiteral(0L)),
                Map.entry("short", makeLiteral((short) 0)),
                Map.entry("byte", makeLiteral((byte) 0)),
                Map.entry("char", makeLiteral((char) 0)),
                Map.entry("boolean", makeLiteral(false))
            ).get(ty.getSimpleName());
        } else {
            return makeLiteral(null);
        }
    }

    /**
     * Returns the variable from the array access. For example array[0][1] will return array.
     *
     * @param ctArrayAccess the array access
     * @return the variable
     */
    public static CtVariableAccess<?> getVariableFromArray(CtArrayAccess<?, ?> ctArrayAccess) {
        CtExpression<?> array = ctArrayAccess.getTarget();

        if (array instanceof CtVariableAccess<?>) {
            return (CtVariableAccess<?>) array;
        } else if (array instanceof CtArrayAccess<?, ?> access) {
            return getVariableFromArray(access);
        } else {
            throw new IllegalArgumentException("Unable to obtain variable from array access: " + ctArrayAccess);
        }
    }

    public static List<CtStatement> getEffectiveStatements(CtBlock<?> block) {
        return block.getStatements().stream().filter(statement -> !(statement instanceof CtComment)).toList();
    }

    public static CtStatement unwrapStatement(CtStatement statement) {
        if (statement instanceof CtBlock<?> block) {
            List<CtStatement> statements = SpoonUtil.getEffectiveStatements(block);
            if (statements.size() == 1) {
                return statements.get(0);
            }
        }
        return statement;
    }

    public static boolean isGetter(CtMethod<?> method) {
        return method.getSimpleName().startsWith("get")
               && method.getParameters().isEmpty()
               && !method.getType().getSimpleName().equals("void")
               && (method.isAbstract() || getEffectiveStatements(method.getBody()).size() == 1);
    }

    public static boolean isSetter(CtMethod<?> method) {
        return method.getSimpleName().startsWith("set")
               && method.getParameters().size() == 1
               && method.getType().getSimpleName().equals("void")
               && (method.isAbstract() || getEffectiveStatements(method.getBody()).size() == 1);
    }

    public static boolean isPrimitiveNumeric(CtTypeReference<?> type) {
        return type.isPrimitive()
               && !type.getQualifiedName().equals("boolean")
               && !type.getQualifiedName().equals("char");
    }

    public static boolean isVoidMethod(CtMethod<?> method) {
        return method.getType().getQualifiedName().equals("void");
    }

    public static boolean isEqualsMethod(CtMethod<?> method) {
        return method.getSimpleName().equals("equals")
               && method.isPublic()
               && method.getType().getQualifiedName().equals("boolean")
               && method.getParameters().size() == 1
               && method.getParameters().get(0).getType().getQualifiedName().equals("java.lang.Object");
    }

    public static boolean isCompareToMethod(CtMethod<?> method) {
        return method.getSimpleName().equals("compareTo")
               && method.isPublic()
               && method.getType().getQualifiedName().equals("boolean")
               && method.getParameters().size() == 1
               && method.getParameters().get(0).getType().getQualifiedName()
                        .equals(method.getDeclaringType().getQualifiedName());
    }

    public static Optional<CtJavaDoc> getJavadoc(CtElement element) {
        if (element.getComments().isEmpty() || !(element.getComments().get(0) instanceof CtJavaDoc)) {
            // TODO lookup inherited javadoc
            return Optional.empty();
        } else {
            return Optional.of(element.getComments().get(0).asJavaDoc());
        }
    }

    public static boolean isStaticCallTo(CtInvocation<?> invocation, String typeName, String methodName) {
        return invocation.getExecutable().isStatic()
               && invocation.getTarget() instanceof CtTypeAccess<?> access
               && access.getAccessedType().getQualifiedName().equals(typeName)
               && invocation.getExecutable().getSimpleName().equals(methodName);
    }

    public static boolean isEffectivelyFinal(StaticAnalysis staticAnalysis, CtField<?> field) {
        return staticAnalysis.getModel()
                             .filterChildren(e -> e instanceof CtFieldWrite write &&
                                                  write.getVariable().equals(field.getReference()))
                             .first() == null;
    }
}
