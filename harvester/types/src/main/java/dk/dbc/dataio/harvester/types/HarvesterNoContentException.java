package dk.dbc.dataio.harvester.types;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class HarvesterNoContentException extends HarvesterException {
    public HarvesterNoContentException(String message) {
        super(message);
    }

    public HarvesterNoContentException(String message, Exception cause) {
        super(message, cause);
    }
}
