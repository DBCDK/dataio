package dk.dbc.dataio.cli.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;

public interface DiffGenerator {
    String getDiff(Kind kind, byte[] current, byte[] next) throws DiffGeneratorException, InvalidMessageException;
}
