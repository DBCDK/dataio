package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.lang.XmlUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Class used for verification of harvester data files containing addi records
 */
public class AddiFileVerifier {
    private final XmlUtil xmlUtil;
    private final JSONBContext jsonbContext;

    public AddiFileVerifier() {
        xmlUtil = new XmlUtil();
        jsonbContext = new JSONBContext();
    }

    /**
     * Verifies content of given addi file against given list of
     * expectations throwing assertion error unless all expectations can be met
     * @param dataFile harvester data file containing addi records
     * @param addiMetaDataList expectations for addi records meta data
     * @param expectations expectations for addi records content
     */
    public void verify(File dataFile, List<AddiMetaData> addiMetaDataList, List<? extends Expectation> expectations) {
        try {
            final AddiReader addiReader = new AddiReader(new BufferedInputStream(new FileInputStream(dataFile)));
            int recordNo = 0;
            while (addiReader.hasNext()) {
                final AddiRecord addiRecord = addiReader.next();
                final AddiMetaData addiMetaData = jsonbContext.unmarshall(new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);
                assertAddiMetadata(recordNo, addiMetaData, addiMetaDataList.get(recordNo));
                if (addiMetaData.diagnostic() == null) {
                    final Object expectation = expectations.get(recordNo);
                    if (expectation instanceof XmlExpectation) {
                        final Document document = xmlUtil.toDocument(addiRecord.getContentData());
                        ((XmlExpectation) expectation).verify(document.getDocumentElement());
                    } else {
                        ((Expectation) expectation).verify(addiRecord.getContentData());
                    }
                }
                recordNo++;
            }
            assertThat("Number of records in addi file", recordNo, is(addiMetaDataList.size()));
        } catch (IOException | SAXException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private void assertAddiMetadata(int recordNo, AddiMetaData actual, AddiMetaData expected) {
        final String prefix = String.format("metadata(%d)", recordNo);
        if (actual == null || expected == null) {
            assertThat(prefix, actual, is(expected));
        }

        assertThat(prefix + ".submitterNumber", actual.submitterNumber(), is(expected.submitterNumber()));
        assertThat(prefix + ".format", actual.format(), is(expected.format()));
        assertThat(prefix + ".bibliographicRecordId", actual.bibliographicRecordId(), is(expected.bibliographicRecordId()));
        assertThat(prefix + ".trackingId", actual.trackingId(), is(expected.trackingId()));
        assertThat(prefix + ".isDeleted", actual.isDeleted(), is(expected.isDeleted()));
        assertThat(prefix + ".enrichmentTrail", actual.enrichmentTrail(), is(expected.enrichmentTrail()));
        assertThat(prefix + ".diagnostic", actual.diagnostic(), is(expected.diagnostic()));
        if (expected.creationDate() != null) {
            assertThat(prefix + ".creationDate", actual.creationDate(), is(expected.creationDate()));
        } else {
            assertThat(prefix + ".creationDate", actual.creationDate(), is(notNullValue()));
        }
        if (expected.libraryRules() != null) {
            assertThat(prefix + ".libraryRules", actual.libraryRules(), is(expected.libraryRules()));
        } else {
            assertThat(prefix + ".libraryRules", actual.libraryRules(), is(new AddiMetaData.LibraryRules()));
        }
    }
}
