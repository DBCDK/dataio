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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteCountingInputStreamTest {

    private final String byteString = "0123456789";

    @Test
    public void read_readWithBufferAndStartOffsetAndMaximumBytesToRead() throws IOException {
        byte[] bytes = byteString.getBytes(StandardCharsets.UTF_8);
        ByteCountingInputStream byteCountingInputStream = new ByteCountingInputStream(asInputStream(byteString));
        byteCountingInputStream.read(new byte[bytes.length], 0, bytes.length);
        assertThat(byteCountingInputStream.getBytesRead(), is(Long.valueOf(bytes.length)));
    }

    @Test
    public void read_readWithBuffer() throws IOException {
        byte[] bytes = byteString.getBytes(StandardCharsets.UTF_8);
        ByteCountingInputStream byteCountingInputStream = new ByteCountingInputStream(asInputStream(byteString));
        byteCountingInputStream.read(new byte[bytes.length]);
        assertThat(byteCountingInputStream.getBytesRead(), is(Long.valueOf(bytes.length)));
    }

    @Test
    public void read_readWithoutInput() throws IOException {
        ByteCountingInputStream byteCountingInputStream = new ByteCountingInputStream(asInputStream(byteString));
        long counter = 0;
        while (byteCountingInputStream.read() != -1) {
            assertThat(byteCountingInputStream.getBytesRead(), is(++counter));
        }
    }

    /*
     * Private methods
     */
    private InputStream asInputStream(String s) {
        return asInputStream(s, StandardCharsets.UTF_8);
    }

    private InputStream asInputStream(String s, Charset encoding) {
        return new ByteArrayInputStream(s.getBytes(encoding));
    }

}
