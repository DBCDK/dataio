package dk.dbc.dataio.sink.worldcat;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UnicodeNormalizationFormDecomposedTest {
    @Test
    public void normalizations() {
        assertThat("ø", getUnicodeValuesOfString(UnicodeNormalizationFormDecomposed.of("ø")), is("\\u00f8"));
        assertThat("ä", getUnicodeValuesOfString(UnicodeNormalizationFormDecomposed.of("ä")), is("\\u0061\\u0308"));
    }

    private String getUnicodeValuesOfString(String str) {
        final StringBuilder buffer = new StringBuilder();
        for (char c : str.toCharArray()) {
            buffer.append(String.format("\\u%04x", (int) c));
        }
        return buffer.toString();
    }
}
