package dk.dbc.dataio.commons.utils.lang;

import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class PrettyPrintTest {

    private final String json = "{\"value\":42}";
    private final String text = "text";
    private final String marcXml =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                    "<marcx:record format=\"danMARC2\"></marcx:record>" +
                    "</marcx:collection>";

    private final Charset encoding = StandardCharsets.UTF_8;

    @Test
    public void asXml_noneXmlInput_returnsOriginalDataAsString() {
        final String prettyPrint = PrettyPrint.asXml(text.getBytes(), encoding);
        assertThat(prettyPrint, is(text));
    }

    @Test
    public void asXml_xmlInput_returnsPrettyPrintedDataAsString() {
        final String expectedPrettyPrint =
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                        "\t<marcx:record format=\"danMARC2\"></marcx:record>\n" +
                        "</marcx:collection>\n";

        final String prettyPrint = PrettyPrint.asXml(marcXml.getBytes(), encoding);
        assertThat(prettyPrint, is(expectedPrettyPrint));
    }

    @Test
    public void asJson_validJsonInput_returnsPrettyPrintedDataAsString() throws JSONBException {
        final String expectedPrettyPrint = "{\n\t\"value\" : 42\n}";

        final String prettyPrint = PrettyPrint.asJson(json.getBytes(), encoding);
        assertThat(prettyPrint, is(expectedPrettyPrint));
    }

    @Test
    public void asJson_invalidJson_throws() {
        try {
            PrettyPrint.asJson(text.getBytes(), encoding);
            fail("expected exception not thrown");
        } catch (JSONBException e) {
        }
    }

    @Test
    public void combinePrintElements_elementsCombined_returnsElementsAsString() throws JSONBException {
        final String prettyPrintJson = PrettyPrint.asJson(json.getBytes(), encoding);
        final String prettyPrintXml = PrettyPrint.asXml(marcXml.getBytes(), encoding);
        final String expectedPrint =
                "{\n" +
                        "\t\"value\" : 42\n" +
                        "}\n\n" +
                        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                        "\t<marcx:record format=\"danMARC2\"></marcx:record>\n" +
                        "</marcx:collection>\n";

        final String combinedPrintElements = PrettyPrint.combinePrintElements(prettyPrintJson, prettyPrintXml);
        assertThat(combinedPrintElements, is(expectedPrint));
    }
}
