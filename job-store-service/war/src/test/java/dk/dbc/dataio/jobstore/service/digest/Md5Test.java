package dk.dbc.dataio.jobstore.service.digest;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Md5Test {
    @Test(expected = NullPointerException.class)
    public void getMd5HexDigest_dataArgIsNull_throws() {
        Md5.asHex(null);
    }

    @Test
    public void getMd5HexDigest_dataArgIsEmpty_returnsDigestAsHexString() {
        // Expected value procured doing %> echo -n "" | md5sum
        final String expectedHex = "d41d8cd98f00b204e9800998ecf8427e";
        String hex = Md5.asHex(new byte[]{});
        assertThat(hex, is(expectedHex));
    }

    @Test
    public void getMd5HexDigest_dataArgIsBasedOnPureAscii_returnsDigestAsHexString() {
        // Expected value procured doing %> echo -n "test data" | md5sum
        final String expectedHex = "eb733a00c0c9d336e65691a37ab54293";
        String hex = Md5.asHex("test data".getBytes(StandardCharsets.UTF_8));
        assertThat(hex, is(expectedHex));
    }

    @Test
    public void getMd5HexDigest_dataArgIsBasedOnUtf8Data_returnsDigestAsHexString() throws Exception {
        // Expected value procured doing %> echo -n "test æøå" | md5sum
        final String expectedHex = "e7a5a6d6f045c0b0dde36d1adf89e60a";
        String hex = Md5.asHex("test æøå".getBytes(StandardCharsets.UTF_8));
        assertThat(hex, is(expectedHex));
    }
}
