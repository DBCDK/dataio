package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PingResponse DTO class.
 */
public class PingResponse implements Serializable {
    private static final long serialVersionUID = -1277715212626501064L;

    private final Status status;
    private final List<String> log;

    /**
     * Class constructor
     *
     * @param status ping result
     * @param log ping messages
     *
     * @throws NullPointerException if given null-valued status or log argument
     */
    @JsonCreator
    public PingResponse(@JsonProperty("status") PingResponse.Status status,
                        @JsonProperty("log") List<String> log) {

        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
        this.log = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(log, "log"));
    }

    public List<String> getLog() {
        return new ArrayList<>(log);
    }

    public Status getStatus() {
        return status;
    }

    public enum Status { OK, FAILED }
}
