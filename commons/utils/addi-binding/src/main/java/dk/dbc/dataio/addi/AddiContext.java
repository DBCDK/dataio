package dk.dbc.dataio.addi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.bindings.EsReferenceData;
import dk.dbc.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class is an abstraction for managing AddiRecord metadata to Java bindings.
 * This class is thread safe.
 */
public class AddiContext {
    private final ObjectMapper xmlMapper;

    public AddiContext() {
        xmlMapper = new XmlMapper();
    }

    /**
     * Extracts ES reference data from given Addi record
     *
     * @param addiRecord addi record from which to extract ES reference data
     * @return ES reference data
     * @throws AddiException if given null-valued Addi record or on failure to extract ES reference data
     */
    public EsReferenceData getEsReferenceData(AddiRecord addiRecord) throws AddiException {
        try {
            InvariantUtil.checkNotNullOrThrow(addiRecord, "addiRecord");
            /* Apparently not all Stax implementations are able to create XMLStreamReader or XMLEventReader
               from a byte array, so for now we need to wrap as a string. This also means that we currently
               only support Addi XML metadata in UTF-8 encoding. */
            return xmlMapper.readValue(new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), EsReferenceData.class);
        } catch (IOException | RuntimeException e) {
            throw new AddiException(e);
        }
    }
}
