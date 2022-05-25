package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.util.Collections;
import java.util.Set;

public class SequenceAnalysisData {
    private final Set<String> data;

    @JsonCreator
    public SequenceAnalysisData(@JsonProperty("data") Set<String> data) throws NullPointerException {
        this.data = Collections.unmodifiableSet(InvariantUtil.checkNotNullOrThrow(data, "data"));
    }

    public Set<String> getData() {
        return data;
    }
}
