package dk.dbc.dataio.addi;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MetaDataTest {
    @Test
    public void getInfoJson_nodeIsMissing_returnsEmptyJson() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                "",
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getInfoJson(), is("{}"));
    }

    @Test
    public void getInfoJson_nodeExists_returnsSinkDirectives() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getInfoJson(),
                is("{\"submitter\":\"820040\",\"format\":\"katalog\",\"language\":\"dan\",\"contentFrom\":\"820040\"}"));
    }

    @Test
    public void getSinkDirectives_nodeIsMissing_returnsNull() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                "",
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getSinkDirectives(), is(nullValue()));
    }

    @Test
    public void getSinkDirectives_nodeExists_returnsSinkDirectives() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getSinkDirectives(), is(notNullValue()));
    }

    @Test
    public void getUpdateSinkDirectives_nodeIsMissing_returnsNull() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                "");
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getUpdateSinkDirectives(), is(nullValue()));
    }

    @Test
    public void getUpdateSinkDirectives_nodeExists_returnsSinkDirectives() throws IOException {
        final String metadataString = String.format(AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE,
                AddiContextTest.ES_DIRECTIVES,
                AddiContextTest.SINK_DIRECTIVES,
                AddiContextTest.UPDATE_SINK_DIRECTIVES);
        final MetaData metaData = MetaData.fromXml(metadataString.getBytes(StandardCharsets.UTF_8));
        assertThat(metaData.getUpdateSinkDirectives(), is(notNullValue()));
    }
}
