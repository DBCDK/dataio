package dk.dbc.dataio.jobstore.service.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProcessorShardTest {

    @Test
    public void toString_returns_expectedProcessorShard() {
        final ProcessorShard accTestProcessorShard = new ProcessorShard(ProcessorShard.Type.ACCTEST);
        assertThat(accTestProcessorShard.toString(), is("acctest"));

        final ProcessorShard businessProcessorShard = new ProcessorShard(ProcessorShard.Type.BUSINESS);
        assertThat(businessProcessorShard.toString(), is("business"));
    }
}
