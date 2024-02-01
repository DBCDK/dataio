package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;

public interface DiffGenerator {
    String getDiff(Kind kind, byte[] current, byte[] next) throws DiffGeneratorException, InvalidMessageException;
}
