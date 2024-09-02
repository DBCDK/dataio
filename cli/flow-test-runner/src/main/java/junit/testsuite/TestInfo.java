package junit.testsuite;

public interface TestInfo {
    TestInfo withContent(String content);

    TestInfo withType(String type);

    TestInfo withMessage(String message);
}
