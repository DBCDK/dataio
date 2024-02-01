package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;

public class JavaDiffGenerator implements DiffGenerator{
    @Override
    public String getDiff(Kind kind, byte[] current, byte[] next) throws DiffGeneratorException, InvalidMessageException {
        return kind.diff(current, next);
    }
}
