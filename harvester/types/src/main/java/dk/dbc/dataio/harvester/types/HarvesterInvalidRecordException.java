package dk.dbc.dataio.harvester.types;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class HarvesterInvalidRecordException extends HarvesterException {
    private static final long serialVersionUID = -3732918573811236155L;

    public HarvesterInvalidRecordException(String message) {
        super(message);
    }

    public HarvesterInvalidRecordException(String message, Exception cause) {
        super(message, cause);
    }
}
