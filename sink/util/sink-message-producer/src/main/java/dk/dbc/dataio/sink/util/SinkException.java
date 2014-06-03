package dk.dbc.dataio.sink.util;

import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class SinkException extends ServiceException {

    public SinkException(String message) {
        super(message);
    }

    public SinkException(String message, Exception cause) {
        super(message, cause);
    }
}
