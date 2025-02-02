package de.firemage.autograder.core.integrated;

import de.firemage.autograder.core.CodePosition;
import de.firemage.autograder.core.InCodeProblem;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.Check;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.nio.file.Path;

public class IntegratedInCodeProblem extends InCodeProblem {
    private final CtElement element;

    public IntegratedInCodeProblem(Check check, CtElement element, LocalizedMessage explanation,
                                   ProblemType problemType, Path root) {
        super(check, mapSourceToCode(element, root), explanation, problemType);

        this.element = element;
    }

    public static CodePosition mapSourceToCode(CtElement element, Path root) {
        SourcePosition position = element.getPosition();
        File file = position.getFile();
        if (file == null) {
            // Try to find the path in the parent class (if it exists)
            CtType<?> parent = element.getParent(CtType.class);
            if (parent != null) {
                file = parent.getPosition().getFile();
            } else {
                throw new IllegalStateException("Cannot resolve the source file");
            }
        }

        if (element instanceof CtType<?>) {
            return new CodePosition(
                root.relativize(file.toPath()),
                position.getLine(),
                position.getLine(),
                position.getColumn(),
                position.getColumn()
            );
        } else {
            return new CodePosition(
                root.relativize(file.toPath()),
                position.getLine(),
                position.getEndLine(),
                position.getColumn(),
                position.getEndColumn()
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
            "IntegratedInCodeProblem { element: '%s', position: '%s' }", element, getPosition()
        );
    }
}
