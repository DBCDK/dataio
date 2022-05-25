package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.marc.Marc8Charset;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CharacterEncodingSchemeTest {
    private final Charset marc8 = new Marc8Charset();
    @Test
    public void charsetOf_normalizesName() {
        assertThat("latin1", CharacterEncodingScheme.charsetOf("latin1"), is(StandardCharsets.ISO_8859_1));
        assertThat("LATIN-1", CharacterEncodingScheme.charsetOf("LATIN-1"), is(StandardCharsets.ISO_8859_1));
        assertThat("ISO-8859-1", CharacterEncodingScheme.charsetOf("ISO-8859-1"), is(StandardCharsets.ISO_8859_1));
        assertThat("utf8", CharacterEncodingScheme.charsetOf("UTF-8"), is(StandardCharsets.UTF_8));
        assertThat("UTF-8", CharacterEncodingScheme.charsetOf("UTF-8"), is(StandardCharsets.UTF_8));
        assertThat("marc-8", CharacterEncodingScheme.charsetOf("marc-8"), is(marc8));
        assertThat("MARC8", CharacterEncodingScheme.charsetOf("MARC8"), is(marc8));
    }

    @Test
    public void charsetOf_throwsWhenUnableToResolveName() {
        assertThat(() -> CharacterEncodingScheme.charsetOf("unknown"), isThrowing(InvalidEncodingException.class));
    }
}
