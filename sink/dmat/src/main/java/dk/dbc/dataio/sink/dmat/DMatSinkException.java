package dk.dbc.dataio.sink.dmat;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;

public class DMatSinkException extends InvalidMessageException {
    public DMatSinkException(String reason) {
        super(reason);
    }
}
