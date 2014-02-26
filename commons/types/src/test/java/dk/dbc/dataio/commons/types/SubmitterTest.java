package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Submitter unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final SubmitterContent CONTENT = SubmitterContentTest.newSubmitterContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new Submitter(ID, VERSION, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsBelowThreshold_throws() {
        new Submitter(Constants.PERSISTENCE_ID_LOWER_BOUND, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsBelowThreshold_throws() {
        new Submitter(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Submitter instance = new Submitter(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    public static Submitter newSubmitterInstance() {
        return new Submitter(ID, VERSION, CONTENT);
    }
}
