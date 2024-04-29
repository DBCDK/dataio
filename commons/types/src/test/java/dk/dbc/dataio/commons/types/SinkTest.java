package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Sink unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SinkTest {
    private static final int ID = 42;
    private static final long VERSION = 1L;
    private static final SinkContent CONTENT = SinkContentTest.newSinkContentInstance();

    @Test
    public void constructor_contentArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Sink(ID, VERSION, null));
    }

    @Test
    public void constructor_idArgIsLessThanLowerBound_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Sink(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT));
    }

    @Test
    public void constructor_versionArgIsLessThanLowerBound_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Sink(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        Sink instance = new Sink(ID, VERSION, CONTENT);
        assertThat(instance, is(notNullValue()));
    }

    public static Sink newSinkInstance() {
        return new Sink(ID, VERSION, CONTENT);
    }
}
