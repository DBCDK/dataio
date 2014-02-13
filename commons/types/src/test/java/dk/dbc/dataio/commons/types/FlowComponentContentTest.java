package dk.dbc.dataio.commons.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * FlowComponentContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentContentTest {
    private static final String NAME = "name";
    private static final String SVN_PROJECT = "svnproject";
    private static final long SVN_REVISION = 1L;
    private static final String JAVA_SCRIPT_NAME = "javascriptname";
    private static final String INVOCATION_METHOD = "method";
    private static final List<JavaScript> JAVASCRIPTS = Arrays.asList(JavaScriptTest.newJavaScriptInstance());

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new FlowComponentContent(null, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new FlowComponentContent("", SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_svnProjectArgIsNull_throws() {
        new FlowComponentContent(NAME, null, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_svnProjectArgIsEmpty_throws() {
        new FlowComponentContent(NAME, "", SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_svnRevisionArgIsZero_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT, 0, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_javaScriptNameArgIsNull_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, null, JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_javaScriptNameArgIsEmpty_throws() {
        new FlowComponentContent("", SVN_PROJECT, SVN_REVISION, "", JAVASCRIPTS, INVOCATION_METHOD);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_javascriptsArgIsNull_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, null, INVOCATION_METHOD);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_invocationMethodArgIsNull_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_invocationMethodArgIsEmpty_returnsNewInstance() {
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, "");
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_javascriptsArgIsEmpty_returnsNewInstance() {
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, new ArrayList<JavaScript>(0), INVOCATION_METHOD);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfJavascriptsList() {
        final List<JavaScript> javaScripts = new ArrayList<>();
        javaScripts.add(JavaScriptTest.newJavaScriptInstance());
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, javaScripts, INVOCATION_METHOD);
        assertThat(instance.getJavascripts().size(), is(1));
        javaScripts.add(null);
        final List<JavaScript> returnedJavascripts = instance.getJavascripts();
        assertThat(returnedJavascripts.size(), is(1));
        returnedJavascripts.add(null);
        assertThat(instance.getJavascripts().size(), is(1));
    }

    public static FlowComponentContent newFlowComponentContentInstance() {
        return new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD);
    }
}
