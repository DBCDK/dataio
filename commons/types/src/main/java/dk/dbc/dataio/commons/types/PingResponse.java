package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PingResponse DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class PingResponse implements Serializable {
    private static final long serialVersionUID = -1277715212626501064L;

    private /* final */ Status status;
    private /* final */ List<String> log;

    private PingResponse() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param status ping result
     * @param log ping messages
     *
     * @throws NullPointerException if given null-valued status or log argument
     */
    public PingResponse(Status status, List<String> log) {
        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
        this.log = new ArrayList<String>(InvariantUtil.checkNotNullOrThrow(log, "log"));
    }

    public List<String> getLog() {
        return new ArrayList<String>(log);
    }

    public Status getStatus() {
        return status;
    }

    public enum Status { OK, FAILED }
}
