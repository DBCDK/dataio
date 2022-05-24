package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
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
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Class used for verification of harvester data files containing addi records
 */
public class AddiFileVerifier {
    private final JSONBContext jsonbContext;

    public AddiFileVerifier() {
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
                        final Document document = JaxpUtil.toDocument(addiRecord.getContentData());
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
        assertThat(prefix + ".diagnostic", actual.diagnostic(), is(selectEqualsOperand(
                actual.diagnostic(), expected.diagnostic())));
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

    // Since we like the nice messages produced by assertThat()
    // but are unable to consistently reproduce the exact stack
    // traces that are part of a normal Diagnostics.equals test,
    // this method executes a custom comparison and returns an
    // operand for the assertThat call which based on the result
    // of the internal comparison is guaranteed to either produce
    // a diff or not.
    private Diagnostic selectEqualsOperand(Diagnostic actual, Diagnostic expected) {
        if (actual == null && expected == null) {
            return null;
        }
        if (actual == null || expected == null) {
            return expected;
        }
        if (actual.getLevel() != expected.getLevel()) return expected;
        if (actual.getMessage() != null || expected.getMessage() != null) {
            // one or both messages are not null, so
            if (actual.getMessage() != null && expected.getMessage() != null) {
                // if both are not null,
                // determine if expected message is contained in actual
                if (!actual.getMessage().contains(expected.getMessage())) {
                    return expected;
                }
            } else {
                // if only one is not null
                return expected;
            }
        }
        return actual;
    }
}
