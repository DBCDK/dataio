package dk.dbc.dataio.jobstore.service.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AttachmentTest {
    @Test
    public void decipherCharset() {
        assertThat("latin1", Attachment.decipherCharset("latin1"), is(StandardCharsets.ISO_8859_1));
        assertThat("utf8", Attachment.decipherCharset("utf8"), is(StandardCharsets.UTF_8));
    }

    @Test
    public void decipherCharset_charsetNameArgIsUnknown_throws() {
        assertThat(() -> Attachment.decipherCharset("not-a-charset"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void decipherFileNameExtensionFromPackaging() {
        assertThat("ISO", Attachment.decipherFileNameExtensionFromPackaging("ISO"), is("iso2709"));
        assertThat("LIN", Attachment.decipherFileNameExtensionFromPackaging("LIN"), is("lin"));
        assertThat("XML", Attachment.decipherFileNameExtensionFromPackaging("XML"), is("xml"));
        assertThat("ADDI-XML", Attachment.decipherFileNameExtensionFromPackaging("ADDI-XML"), is("xml"));
        assertThat("UNKNOWN", Attachment.decipherFileNameExtensionFromPackaging("UNKNOWN"), is("txt"));
    }
}
