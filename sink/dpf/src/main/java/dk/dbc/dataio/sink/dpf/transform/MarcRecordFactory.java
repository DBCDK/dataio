package dk.dbc.dataio.sink.dpf.transform;

import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.MarcXchangeV1Writer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class MarcRecordFactory {
    private MarcRecordFactory() {
    }

    public static MarcRecord fromMarcXchange(byte[] bytes) throws MarcReaderException {
        MarcXchangeV1Reader reader = new MarcXchangeV1Reader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
        return reader.read();
    }

    public static byte[] toMarcXchange(MarcRecord marcRecord) {
        MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
        return writer.write(marcRecord, StandardCharsets.UTF_8);
    }
}
