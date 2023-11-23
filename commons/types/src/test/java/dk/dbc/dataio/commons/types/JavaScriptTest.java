package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JavaScript unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class JavaScriptTest {
    private static final String MODULE_NAME = "module";
    private static final String JAVASCRIPT = "javascript";

    @Test(expected = NullPointerException.class)
    public void constructor_javascriptArgIsNull_throws() {
        new JavaScript(null, MODULE_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_javascriptArgIsEmpty_throws() {
        new JavaScript("", MODULE_NAME);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_moduleNameArgIsNull_throws() {
        new JavaScript(JAVASCRIPT, null);
    }

    @Test
    public void constructor_moduleNameArgIsEmpty_returnsNewInstance() {
        final JavaScript instance = new JavaScript(JAVASCRIPT, "");
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JavaScript instance = new JavaScript(JAVASCRIPT, MODULE_NAME);
        assertThat(instance, is(notNullValue()));
    }

    public static JavaScript newJavaScriptInstance() {
        return new JavaScript(JAVASCRIPT, MODULE_NAME);
    }
}
