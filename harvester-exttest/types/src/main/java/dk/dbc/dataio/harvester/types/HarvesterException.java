package dk.dbc.dataio.harvester.types;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class HarvesterException extends Exception {
    private static final long serialVersionUID = -8575425492824519340L;

    public HarvesterException(Exception cause) {
        super(cause);
    }

    public HarvesterException(String message) {
        super(message);
    }

    public HarvesterException(String message, Exception cause) {
        super(message, cause);
    }
}
