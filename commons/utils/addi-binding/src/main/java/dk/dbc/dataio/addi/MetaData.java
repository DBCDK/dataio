package dk.dbc.dataio.addi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.dataio.addi.bindings.SinkDirectives;
import dk.dbc.dataio.addi.bindings.UpdateSinkDirectives;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class is an abstraction for parsing AddiRecord metadata.
 * This class is thread safe.
 */
public class MetaData {
    private static final ObjectMapper xmlMapper = new XmlMapper();
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private final JsonNode jsonNode;

    private MetaData(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public String toJson() throws JsonProcessingException {
        return jsonMapper.writeValueAsString(jsonNode);
    }

    public String getInfoJson() throws JsonProcessingException {
        final JsonNode info = jsonNode.path("info");
        if (info.getNodeType() != JsonNodeType.MISSING) {
            return jsonMapper.writeValueAsString(info);
        }
        return "{}";
    }

    public SinkDirectives getSinkDirectives() throws JsonProcessingException {
        final JsonNode sinkProcessing = jsonNode.path("sink-processing");
        if (sinkProcessing.getNodeType() != JsonNodeType.MISSING) {
            return jsonMapper.treeToValue(sinkProcessing, SinkDirectives.class);
        }
        return null;
    }

    public UpdateSinkDirectives getUpdateSinkDirectives() throws JsonProcessingException {
        final JsonNode sinkUpdateTemplate = jsonNode.path("sink-update-template");
        if (sinkUpdateTemplate.getNodeType() != JsonNodeType.MISSING) {
            return jsonMapper.treeToValue(sinkUpdateTemplate, UpdateSinkDirectives.class);
        }
        return null;
    }

    public static MetaData fromXml(byte[] xmlString) throws IOException {
        return fromXml(new String(xmlString, StandardCharsets.UTF_8));
    }

    public static MetaData fromXml(String xmlString) throws IOException {
        return new MetaData(xmlMapper.readValue(xmlString, JsonNode.class));
    }
}
