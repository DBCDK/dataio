package dk.dbc.dataio.commons.utils.invariant;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * InvariantUtil unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class InvariantUtilTest {
    private final String parameterName = "name";

    @Test(expected = NullPointerException.class)
    public void checkNotNullOrThrow_objectArgIsNull_throws() {
        InvariantUtil.checkNotNullOrThrow(null, parameterName);
    }

    @Test
    public void checkNotNullOrThrow_objectArgIsNonNull_returnsObject() {
        final Object object = new Object();
        final Object returnedObject = InvariantUtil.checkNotNullOrThrow(object, parameterName);
        assertThat(returnedObject, is(object));
    }

    @Test(expected = NullPointerException.class)
    public void checkNotNullNotEmptyOrThrow_stringObjectArgIsNull_throws() {
        InvariantUtil.checkNotNullNotEmptyOrThrow(null, parameterName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkNotNullNotEmptyOrThrow_stringObjectArgIsEmpty_throws() {
        InvariantUtil.checkNotNullNotEmptyOrThrow("", parameterName);
    }

    @Test
    public void checkNotNullNotEmptyOrThrow_stringObjectArgIsNonEmpty_returnsObject() {
        final String object = "string";
        final String returnedObject = InvariantUtil.checkNotNullOrThrow(object, parameterName);
        assertThat(returnedObject, is(object));
    }
}
