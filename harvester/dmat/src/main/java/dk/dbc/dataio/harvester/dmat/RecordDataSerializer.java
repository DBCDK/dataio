package dk.dbc.dataio.harvester.dmat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dmat.service.dto.RecordData;

import java.io.IOException;

public class RecordDataSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String recordData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        try {
            jsonGenerator.writeObject(RecordData.fromRaw(recordData));
        } catch (JSONBException e) {
            throw new IOException(String.format("Unable to serialize object %s", recordData));
        }
    }
}
