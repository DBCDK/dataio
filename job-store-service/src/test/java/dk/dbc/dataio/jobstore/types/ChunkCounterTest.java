package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChunkCounterTest {
    @Test
    public void constructor_returnsNewInstance() {
        final ChunkCounter instance = new ChunkCounter();
        assertThat(instance.getTotal(), is(0L));
        assertThat(instance.getItemResultCounter().getTotal(), is(0L));
    }

    @Test
    public void incrementTotal_incrementsCount() {
        final ChunkCounter instance = new ChunkCounter();
        instance.incrementTotal();
        assertThat(instance.getTotal(), is(1L));
    }
}
