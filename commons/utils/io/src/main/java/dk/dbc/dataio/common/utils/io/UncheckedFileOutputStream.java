package dk.dbc.dataio.common.utils.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * This class wraps checked IOExceptions from FileOutputStream
 * class methods as UncheckedIOException, so that it can be used in
 * lambda expressions.
 */
public class UncheckedFileOutputStream extends FileOutputStream {
    public UncheckedFileOutputStream(File file) throws FileNotFoundException {
        super(file);
    }

    @Override
    public void write(byte[] b) {
        try {
            super.write(b);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
