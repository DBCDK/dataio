package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    public void constructor_javascriptArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JavaScript(null, MODULE_NAME));
    }

    @Test
    public void constructor_javascriptArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new JavaScript("", MODULE_NAME));
    }

    @Test
    public void constructor_moduleNameArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JavaScript(JAVASCRIPT, null));
    }

    @Test
    public void constructor_moduleNameArgIsEmpty_returnsNewInstance() {
        JavaScript instance = new JavaScript(JAVASCRIPT, "");
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        JavaScript instance = new JavaScript(JAVASCRIPT, MODULE_NAME);
        assertThat(instance, is(notNullValue()));
    }

    public static JavaScript newJavaScriptInstance() {
        return new JavaScript(JAVASCRIPT, MODULE_NAME);
    }
}
