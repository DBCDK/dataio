package dk.dbc.dataio.common.utils.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * count the number of bytes read through the stream
 */
public class ByteCountingInputStream extends InputStream {
    private long count = 0;
    private long marked = -1;
    private InputStream is;

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        int r = is.read();
        if (r > 0) {
            count++;
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int r = is.read(b, off, len);
        if (r > 0) {
            count += r;
        }
        return r;
    }

    @Override
    public long skip(long skipped) throws IOException {
        long l = is.skip(skipped);
        if (l > 0) {
            count += l;
        }
        return l;
    }

    @Override
    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
        marked = count;
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
        count = marked;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    /**
     * get the actual number of bytes read
     *
     * @return a long, the number of bytes read
     */
    public long getBytesRead() {
        return count;
    }

    public ByteCountingInputStream(InputStream is) {
        this.is = is;
    }
}
