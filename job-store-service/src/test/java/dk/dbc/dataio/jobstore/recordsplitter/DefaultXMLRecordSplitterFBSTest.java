package dk.dbc.dataio.jobstore.recordsplitter;

import dk.dbc.dataio.jobstore.types.IllegalDataException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import javax.xml.stream.XMLStreamException;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.xml.sax.SAXException;

public class DefaultXMLRecordSplitterFBSTest {

    private static final String UTF8_CHARSET = "UTF-8";

    /*
     * A specialiezed test for ensuring that we can handle the marcxchange-xml from FBS
     * through the DefaultXMLRecordSplitter.
     */
    @Test
    public void testMarcXchangeXMLWithCollectionTag_accepted() throws IllegalDataException, XMLStreamException, UnsupportedEncodingException, SAXException, IOException {
        final String marcxchange
                = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">"
                + "<marcx:record format=\"danMARC2\" type=\"Bibliographic\">"
                + "<marcx:leader>00000n    2200000   4500</marcx:leader>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">"
                + "<marcx:subfield code=\"a\">41731907</marcx:subfield>"
                + "<marcx:subfield code=\"b\">870970</marcx:subfield>"
                + "<marcx:subfield code=\"c\">19980129</marcx:subfield>"
                + "<marcx:subfield code=\"d\">19980129</marcx:subfield>"
                + "<marcx:subfield code=\"f\">a</marcx:subfield>"
                + "<marcx:subfield code=\"o\">c</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"002\">"
                + "<marcx:subfield code=\"b\">716300</marcx:subfield>"
                + "<marcx:subfield code=\"c\">92131017</marcx:subfield>"
                + "<marcx:subfield code=\"x\">71630092131017</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">"
                + "<marcx:subfield code=\"r\">n</marcx:subfield>"
                + "<marcx:subfield code=\"a\">e</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"008\">"
                + "<marcx:subfield code=\"t\">s</marcx:subfield>"
                + "<marcx:subfield code=\"v\">0</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"009\">"
                + "<marcx:subfield code=\"a\">s</marcx:subfield>"
                + "<marcx:subfield code=\"g\">xc</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"039\">"
                + "<marcx:subfield code=\"a\">und</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"100\">"
                + "<marcx:subfield code=\"a\">David Rose &amp; His Orchestra</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">"
                + "<marcx:subfield code=\"a\">The Stripper &amp; other favourites</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"260\">"
                + "<marcx:subfield code=\"b\">EMPORIO EMPRCD 501</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"531\">"
                + "<marcx:subfield code=\"a\">Indhold:</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"652\">"
                + "<marcx:subfield code=\"m\">xx</marcx:subfield>"
                + "</marcx:datafield>"
                + "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"996\">"
                + "<marcx:subfield code=\"a\">716300</marcx:subfield>"
                + "</marcx:datafield>"
                + "</marcx:record>"
                + "</marcx:collection>";
        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(marcxchange.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertXMLEqual(it.next(), marcxchange);
        assertThat(it.hasNext(), is(false));
    }
}
