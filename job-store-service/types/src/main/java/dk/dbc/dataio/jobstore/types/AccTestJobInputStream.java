package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.invariant.InvariantUtil;

public class AccTestJobInputStream extends JobInputStream {

    private final Flow flow;
    private final RecordSplitterConstants.RecordSplitter typeOfDataPartitioner;

    /**
     * @param jobSpecification      the jobSpecification
     * @param flow                  the flow to use
     * @param typeOfDataPartitioner the type of data partitioner to use
     * @throws NullPointerException if given null-valued argument
     */
    public AccTestJobInputStream(
            @JsonProperty("jobSpecification") JobSpecification jobSpecification,
            @JsonProperty("flow") Flow flow,
            @JsonProperty("typeOfDataPartitioner") RecordSplitterConstants.RecordSplitter typeOfDataPartitioner) throws NullPointerException {

        super(jobSpecification);
        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.typeOfDataPartitioner = InvariantUtil.checkNotNullOrThrow(typeOfDataPartitioner, "typeOfDataPartitioner");
    }

    public Flow getFlow() {
        return flow;
    }

    public RecordSplitterConstants.RecordSplitter getTypeOfDataPartitioner() {
        return typeOfDataPartitioner;
    }
}
