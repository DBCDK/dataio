package dk.dbc.dataio.common.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * This class wraps checked IOExceptions from ByteArrayOutputStream
 * class methods as UncheckedIOException, so that it can be used in
 * lambda expressions.
 */
public class UncheckedByteArrayOutputStream extends ByteArrayOutputStream {
    @Override
    public void write(byte[] b) {
        try {
            super.write(b);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
