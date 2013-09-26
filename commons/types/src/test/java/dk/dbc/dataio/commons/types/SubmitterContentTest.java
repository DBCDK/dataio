package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * SubmitterContent unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterContentTest {
    private static final long NUMBER = 42L;
    private static final String NAME = "NAME";
    private static final String DESCRIPTION = "description";

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SubmitterContent(NUMBER, null, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SubmitterContent(NUMBER, "", DESCRIPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new SubmitterContent(NUMBER, NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new SubmitterContent(NUMBER, NAME, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_numberArgIsBelowThreshold_throws() {
        new SubmitterContent(SubmitterContent.NUMBER_LOWER_THRESHOLD, NAME, DESCRIPTION);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SubmitterContent instance = new SubmitterContent(NUMBER, NAME, DESCRIPTION);
        assertThat(instance, is(notNullValue()));
    }

    public static SubmitterContent newSubmitterContentInstance() {
        return new SubmitterContent(NUMBER, NAME, DESCRIPTION);
    }
}
