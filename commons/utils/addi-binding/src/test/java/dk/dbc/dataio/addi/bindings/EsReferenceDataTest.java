package dk.dbc.dataio.addi.bindings;

import org.junit.Test;

import static dk.dbc.dataio.addi.AddiContextTest.ES_DIRECTIVES;
import static dk.dbc.dataio.addi.AddiContextTest.ES_REFERENCE_DATA_XML_TEMPLATE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EsReferenceDataTest {
    @Test
    public void toXmlString() {
        final EsReferenceData esReferenceData = new EsReferenceData()
                .withEsDirectives(new EsDirectives()
                        .withSubmitter("820040")
                        .withFormat("katalog")
                        .withLanguage("dan")
                        .withContentFrom("820040"));

        assertThat(esReferenceData.toXmlString(), is(String.format(ES_REFERENCE_DATA_XML_TEMPLATE, ES_DIRECTIVES, "", "")));
    }
}
