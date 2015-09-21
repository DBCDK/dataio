/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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