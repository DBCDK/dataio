package dk.dbc.dataio.common.utils.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BoundedInputStreamTest {
    private final String byteString = "0123456789";

    @Test
    public void bounds() throws IOException {
        final InputStream inputStream = asInputStream(byteString);

        final BoundedInputStream boundedInputStream1 = new BoundedInputStream(inputStream, 2);
        int i;
        final StringBuilder result = new StringBuilder();
        while ((i = boundedInputStream1.read()) >= 0) {
            result.append((char) i);
        }
        assertThat("1st stream", result.toString(), is("01"));

        final BoundedInputStream boundedInputStream2 = new BoundedInputStream(inputStream, 4);
        final byte[] bytes = new byte[4];
        assertThat("2nd stream first 3 bytes", boundedInputStream2.read(bytes, 0, 3), is(3));
        assertThat("2nd stream next 3 bytes", boundedInputStream2.read(bytes, 3, 3), is(1));
        assertThat("2nd stream", new String(bytes, StandardCharsets.UTF_8), is("2345"));

        final BoundedInputStream boundedInputStream3 = new BoundedInputStream(inputStream, 3);
        assertThat("3rd stream skip", boundedInputStream3.skip(1000), is(3L));

        final BoundedInputStream boundedInputStream4 = new BoundedInputStream(inputStream, 42);
        assertThat("4th stream first byte", (char) boundedInputStream4.read(), is('9'));
        assertThat("4th stream eof", boundedInputStream4.read(), is(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void boundMustBeLargerThanZero() {
        new BoundedInputStream(asInputStream(byteString), 0);
    }

    private InputStream asInputStream(String s) {
        return asInputStream(s, StandardCharsets.UTF_8);
    }

    private InputStream asInputStream(String s, Charset encoding) {
        return new ByteArrayInputStream(s.getBytes(encoding));
    }
}
