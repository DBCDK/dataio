package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.lang.XmlUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Class used for verification of harvester data files containing addi records
 */
public class AddiFileVerifier {
    private final XmlUtil xmlUtil;
    private final JSONBContext jsonbContext;

    public AddiFileVerifier() throws ParserConfigurationException {
        xmlUtil = new XmlUtil();
        jsonbContext = new JSONBContext();
    }

    /**
     * Verifies content of given addi file against given list of
     * expectations throwing assertion error unless all expectations can be met
     * @param dataFile harvester data file containing addi records
     * @param xmlExpectations expectations for addi records xml content
     * @throws IOException if unable to read harvester data file
     * @throws SAXException if unable to parse addi record content as XML
     */
    public void verify(File dataFile, List<AddiMetaData> addiMetaDataList, List<XmlExpectation> xmlExpectations) throws IOException, SAXException, JSONBException {
        final AddiReader addiReader = new AddiReader(new BufferedInputStream(new FileInputStream(dataFile)));
        int recordNo = 0;
        while (addiReader.hasNext()) {
            final AddiRecord addiRecord = addiReader.next();
            final AddiMetaData addiMetaData = jsonbContext.unmarshall(new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);
            assertThat(addiMetaData, is(addiMetaDataList.get(recordNo)));
            if (!addiMetaData.diagnostic().isPresent()) {
                final Document document = xmlUtil.toDocument(addiRecord.getContentData());
                xmlExpectations.get(recordNo).verify(document.getDocumentElement());
            }
            recordNo++;
        }
        assertThat("Number of records in addi file", recordNo, is(addiMetaDataList.size()));
    }
}
