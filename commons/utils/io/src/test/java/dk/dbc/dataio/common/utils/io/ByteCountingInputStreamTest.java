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
        assertThat(byteCountingInputStream.getBytesRead(), is((long) bytes.length));
    }

    @Test
    public void read_readWithBuffer() throws IOException {
        byte[] bytes = byteString.getBytes(StandardCharsets.UTF_8);
        ByteCountingInputStream byteCountingInputStream = new ByteCountingInputStream(asInputStream(byteString));
        byteCountingInputStream.read(new byte[bytes.length]);
        assertThat(byteCountingInputStream.getBytesRead(), is((long) bytes.length));
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
