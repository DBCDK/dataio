package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ItemResultCounterTest {
    @Test
    public void constructor_returnsNewInstance() {
        final ItemResultCounter instance = new ItemResultCounter();
        assertThat(instance.getFailure(), is(0L));
        assertThat(instance.getIgnore(), is(0L));
        assertThat(instance.getSuccess(), is(0L));
        assertThat(instance.getTotal(), is(0L));
    }

    @Test
    public void incrementFailure_incrementsCount() {
        final ItemResultCounter instance = new ItemResultCounter();
        instance.incrementFailure();
        assertThat(instance.getFailure(), is(1L));
        assertThat(instance.getIgnore(), is(0L));
        assertThat(instance.getSuccess(), is(0L));
        assertThat(instance.getTotal(), is(1L));
    }

    @Test
    public void incrementIgnore_incrementsCount() {
        final ItemResultCounter instance = new ItemResultCounter();
        instance.incrementIgnore();
        assertThat(instance.getFailure(), is(0L));
        assertThat(instance.getIgnore(), is(1L));
        assertThat(instance.getSuccess(), is(0L));
        assertThat(instance.getTotal(), is(1L));
    }

    @Test
    public void incrementSuccess_incrementsCount() {
        final ItemResultCounter instance = new ItemResultCounter();
        instance.incrementSuccess();
        assertThat(instance.getFailure(), is(0L));
        assertThat(instance.getIgnore(), is(0L));
        assertThat(instance.getSuccess(), is(1L));
        assertThat(instance.getTotal(), is(1L));
    }

    @Test
    public void getTotal_returnsSumOfAllCounts() {
        final ItemResultCounter instance = new ItemResultCounter();
        instance.incrementFailure();
        instance.incrementIgnore();
        instance.incrementSuccess();
        assertThat(instance.getTotal(), is(3L));
    }
}
