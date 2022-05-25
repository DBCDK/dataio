package dk.dbc.dataio.common.utils.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Bounded {@link InputStream} which is only allowed to read a bounded
 * number of bytes from the {@link InputStream} from which it is created.
 */
public class BoundedInputStream extends InputStream {
    private final InputStream is;
    private final long bound;
    private long bytesRead = 0;

    public BoundedInputStream(InputStream is, long bound) {
        this.is = is;
        this.bound = bound;
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be larger than zero");
        }
    }

    @Override
    public int read() throws IOException {
        if (bytesRead == bound) {
            return -1;
        }
        if (bytesRead > bound) {
            throw new IllegalStateException("Number of bytes read exceeds bound");
        }
        int r = is.read();
        if (r >= 0) {
            bytesRead++;
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bytesRead == bound) {
            return -1;
        }
        if (bytesRead > bound) {
            throw new IllegalStateException("Number of bytes read exceeds bound");
        }
        if (bytesRead + len > bound) {
            len = (int) (bound - bytesRead);
        }
        int r = is.read(b, off, len);
        if (r > 0) {
            bytesRead += r;
        }
        return r;
    }

    @Override
    public long skip(long skipped) throws IOException {
        if (bytesRead == bound) {
            return -1;
        }
        if (bytesRead > bound) {
            throw new IllegalStateException("Number of bytes read exceeds bound");
        }
        if (bytesRead + skipped > bound) {
            skipped = bound - bytesRead;
        }
        long l = is.skip(skipped);
        if (l > 0) {
            bytesRead += l;
        }
        return l;
    }
}
