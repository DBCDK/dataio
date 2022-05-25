package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * GatekeeperDestination unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class GatekeeperDestinationTest {
    private static final long ID = 42L;
    private static final String SUBMITTER_NUMBER = "123456";
    private static final String DESTINATION = "destination";
    private static final String PACKAGING = "lin";
    private static final String FORMAT = "marc2";

    @Test(expected = NullPointerException.class)
    public void constructor_submitterNumberArgIsNull_throws() {
        new GatekeeperDestination(ID, null, DESTINATION, PACKAGING, FORMAT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_submitterNumberArgIsEmpty_throws() {
        new GatekeeperDestination(ID, "", DESTINATION, PACKAGING, FORMAT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, null, PACKAGING, FORMAT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_destinationArgIsEmpty_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, "", PACKAGING, FORMAT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_packagingArgIsNull_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, null, FORMAT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_packagingArgIsEmpty_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, "", FORMAT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_formatArgIsNull_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_formatArgIsEmpty_throws() {
        new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final GatekeeperDestination instance = new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, FORMAT);
        assertThat(instance, is(notNullValue()));
    }

    public static GatekeeperDestination newGatekeeperDestinationInstance() {
        return new GatekeeperDestination(ID, SUBMITTER_NUMBER, DESTINATION, PACKAGING, FORMAT);
    }
}
