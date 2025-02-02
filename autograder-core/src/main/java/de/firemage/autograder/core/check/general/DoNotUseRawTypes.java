package de.firemage.autograder.core.check.general;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.dynamic.DynamicAnalysis;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

@ExecutableCheck(reportedProblems = { ProblemType.DO_NOT_USE_RAW_TYPES })
public class DoNotUseRawTypes extends IntegratedCheck {
    public DoNotUseRawTypes() {
        super(new LocalizedMessage("do-not-use-raw-types-desc"));
    }

    private boolean isRawType(CtTypeReference<?> ctTypeReference) {
        CtType<?> declaration = ctTypeReference.getTypeDeclaration();

        if (declaration == null) {
            // reference points to a type not in the class-path
            return false;
        }

        return declaration.getFormalCtTypeParameters().size() != ctTypeReference.getActualTypeArguments().size();
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis, DynamicAnalysis dynamicAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtTypeReference<?>>() {
            @Override
            public void process(CtTypeReference<?> ctTypeReference) {
                // skip references which have no position in source code
                if (!ctTypeReference.getPosition().isValidPosition()) {
                    return;
                }

                if (isRawType(ctTypeReference)) {
                    addLocalProblem(
                        ctTypeReference,
                        new LocalizedMessage("do-not-use-raw-types-exp"),
                        ProblemType.DO_NOT_USE_RAW_TYPES
                    );
                }
            }
        });
    }
}
