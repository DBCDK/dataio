package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PriorityTest {
    @Test
    public void factory() {
        assertThat("LOW", Priority.of(-42), is(Priority.LOW));
        assertThat("LOW max", Priority.of(1), is(Priority.LOW));
        assertThat("NORMAL min", Priority.of(2), is(Priority.NORMAL));
        assertThat("NORMAL max", Priority.of(6), is(Priority.NORMAL));
        assertThat("HIGH min", Priority.of(7), is(Priority.HIGH));
        assertThat("HIGH", Priority.of(42), is(Priority.HIGH));
    }
}
