package dk.dbc.dataio.harvester.types;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class HarvesterSourceException extends HarvesterException {
    private static final long serialVersionUID = -6427111960713285427L;

    public HarvesterSourceException(String message, Exception cause) {
        super(message, cause);
    }
}
