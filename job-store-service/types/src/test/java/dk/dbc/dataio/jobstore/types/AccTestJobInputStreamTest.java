package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AccTestJobInputStreamTest {

    private final JobSpecification jobSpecification = new JobSpecification().withFormat("format");
    private final Flow flow = new FlowBuilder().build();
    private final RecordSplitter typeOfDataPartitioner = RecordSplitter.XML;

    @Test
    public void constructor_flowArgIsNull_throws() {
        assertThat(() -> new AccTestJobInputStream(jobSpecification, null, typeOfDataPartitioner),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_typeOfDataPartitionerArgIsNull_throws() {
        assertThat(() -> new AccTestJobInputStream(jobSpecification, flow, null),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returns() {
        AccTestJobInputStream jobInputStream = new AccTestJobInputStream(jobSpecification, flow, typeOfDataPartitioner);
        assertThat(jobInputStream.getFlow(), is(flow));
        assertThat(jobInputStream.getTypeOfDataPartitioner(), is(typeOfDataPartitioner));
        assertThat(jobInputStream.getJobSpecification(), is(jobSpecification));
        assertThat(jobInputStream.getIsEndOfJob(), is(false)); // Default value
        assertThat(jobInputStream.getPartNumber(), is(0));     // Default value
    }
}
