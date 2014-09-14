package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EsWorkloadTest {
    @Test(expected = NullPointerException.class)
    public void constructor_chunkResultArgIsNull_throws() {
        new EsWorkload(null, new ArrayList<AddiRecord>(0));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsArgIsNull_throws() {
        new EsWorkload(new SinkChunkResultBuilder().build(), null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final EsWorkload instance = new EsWorkload(new SinkChunkResultBuilder().build(), new ArrayList<AddiRecord>(0));
        assertThat(instance, is(notNullValue()));
    }
}
