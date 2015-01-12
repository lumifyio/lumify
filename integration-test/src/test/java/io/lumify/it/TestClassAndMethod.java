package io.lumify.it;

import org.junit.rules.TestName;
import org.junit.runner.Description;

/**
 * Annotate a public field on a test class with {@link org.junit.Rule} to capture the test class name and current test
 * method for use in logging, etc.
 */
public class TestClassAndMethod extends TestName {
    private String className;

    @Override
    protected void starting(Description description) {
        super.starting(description);
        className = description.getClassName();
    }

    public String getClassName() {
        return className;
    }
}
