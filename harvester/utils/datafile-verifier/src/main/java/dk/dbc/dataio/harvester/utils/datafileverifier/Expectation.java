package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Expectation {
    public final Charset encoding;
    public final byte[] expectedData;

    public Expectation() {
        this((byte[]) null, null);
    }

    public Expectation(String expectedData) {
        this(expectedData.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public Expectation(String expectedData, Charset encoding) {
        this(expectedData.getBytes(encoding), encoding);
    }

    public Expectation(byte[] expectedData) {
        this(expectedData, StandardCharsets.UTF_8);
    }

    public Expectation(byte[] expectedData, Charset encoding) {
        this.expectedData = expectedData;
        this.encoding = encoding;
    }

    public void verify(byte[] data) {
        assertThat("data", StringUtil.asString(data, encoding), is(StringUtil.asString(expectedData, encoding)));
    }
}
