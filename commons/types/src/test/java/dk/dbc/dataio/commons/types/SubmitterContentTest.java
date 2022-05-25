package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * SubmitterContent unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterContentTest {
    private static final long NUMBER = 42L;
    private static final String NAME = "NAME";
    private static final String DESCRIPTION = "description";

    @Test(expected = IllegalArgumentException.class)
    public void constructor_numberArgIsLessThanLowerBound_throws() {
        new SubmitterContent(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, NAME, DESCRIPTION, Priority.NORMAL, true);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new SubmitterContent(NUMBER, null, DESCRIPTION, Priority.NORMAL, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new SubmitterContent(NUMBER, "", DESCRIPTION, Priority.NORMAL, true);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new SubmitterContent(NUMBER, NAME, null, Priority.NORMAL, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new SubmitterContent(NUMBER, NAME, "", Priority.NORMAL, true);
    }

    @Test
    public void constructor_priorityArgIsNull_priorityIsNull() {
        final SubmitterContent content = new SubmitterContent(NUMBER, NAME, DESCRIPTION, null, true);
        assertThat(content.getPriority(), is(nullValue()));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final SubmitterContent content = new SubmitterContent(NUMBER, NAME, DESCRIPTION, Priority.NORMAL, true);
        assertThat(content, is(notNullValue()));
        assertThat(content.getNumber(), is(NUMBER));
        assertThat(content.getName(), is(NAME));
        assertThat(content.getDescription(), is(DESCRIPTION));
        assertThat(content.getPriority(), is(Priority.NORMAL));
        assertThat(content.isEnabled(), is(true));
    }

    public static SubmitterContent newSubmitterContentInstance() {
        return new SubmitterContent(NUMBER, NAME, DESCRIPTION, Priority.NORMAL, true);
    }
}
