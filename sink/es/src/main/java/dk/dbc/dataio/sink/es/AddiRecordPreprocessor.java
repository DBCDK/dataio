package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.AddiContext;
import dk.dbc.dataio.addi.AddiException;
import dk.dbc.dataio.addi.bindings.EsReferenceData;
import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AddiRecordPreprocessor {
    private final AddiContext addiContext = new AddiContext();

    /**
     * This method pre-processes an addi record according to the following rules:
     *
     * If metadata contains sink-processing element with attribute encodeAs2709 set to true, the content data is converted to iso2709.
     * DBCTrackingId is added as attribute to metadata info element if a non-null tracking ID value is given.
     * All dataIO specific elements are stripped from the metadata of the returned addi record.
     *
     * @param addiRecord Addi record to pre-process
     * @param trackingId tracking ID
     * @return pre-processed Addi record
     * @throws IllegalArgumentException on invalid metadata or content
     */
    public AddiRecord execute(AddiRecord addiRecord, String trackingId) throws IllegalArgumentException {
        try {
            byte[] content = addiRecord.getContentData();
            final EsReferenceData esReferenceData = addiContext.getEsReferenceData(addiRecord);
            if (esReferenceData.sinkDirectives != null && esReferenceData.sinkDirectives.encodeAs2709) {
                content = Iso2709Packer.create2709FromMarcXChangeRecord(
                        JaxpUtil.toDocument(addiRecord.getContentData()), new DanMarc2Charset());
            }
            esReferenceData.esDirectives.withTrackingId(trackingId);
            return new AddiRecord(esReferenceData.toXmlString().getBytes(StandardCharsets.UTF_8), content);
        } catch (AddiException | IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
