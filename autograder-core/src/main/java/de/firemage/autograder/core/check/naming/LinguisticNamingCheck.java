package de.firemage.autograder.core.check.naming;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.pmd.PMDCheck;
import net.sourceforge.pmd.lang.java.rule.codestyle.LinguisticNamingRule;

@ExecutableCheck(reportedProblems = {ProblemType.CONFUSING_IDENTIFIER})
public class LinguisticNamingCheck extends PMDCheck {
    public LinguisticNamingCheck() {
        super(new LocalizedMessage("linguistic-desc"), new LinguisticNamingRule(), ProblemType.CONFUSING_IDENTIFIER);
    }
}
