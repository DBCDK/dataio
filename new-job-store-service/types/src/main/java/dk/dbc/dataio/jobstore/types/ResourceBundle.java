package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class ResourceBundle {
    private final Flow flow;
    private final Sink sink;
    private final SupplementaryProcessData supplementaryProcessData;

    /**
     * Class constructor
     * @param flow cached within a job entity
     * @param sink cached within a job entity
     * @param supplementaryProcessData retrieved from job specification
     * @throws NullPointerException if given null-valued argument
     */
    @JsonCreator
    public ResourceBundle (@JsonProperty("flow") Flow flow,
                           @JsonProperty("sink") Sink sink,
                           @JsonProperty("supplementaryProcessData") SupplementaryProcessData supplementaryProcessData) throws NullPointerException {

        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.sink = InvariantUtil.checkNotNullOrThrow(sink, "sink");
        this.supplementaryProcessData = InvariantUtil.checkNotNullOrThrow(supplementaryProcessData, "supplementaryProcessData");
    }

    public Flow getFlow() {
        return flow;
    }

    public Sink getSink() {
        return sink;
    }

    public SupplementaryProcessData getSupplementaryProcessData() {
        return supplementaryProcessData;
    }
}
