package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Sink unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final SinkContent CONTENT = SinkContentTest.newSinkContentInstance();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new Sink(ID, VERSION, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsBelowThreshold_throws() {
        new Sink(Sink.ID_VERSION_LOWER_THRESHOLD, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsBelowThreshold_throws() {
        new Sink(ID, Sink.ID_VERSION_LOWER_THRESHOLD, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Sink instance = new Sink(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    public static Sink newSinkInstance() {
        return new Sink(ID, VERSION, CONTENT);
    }
}
