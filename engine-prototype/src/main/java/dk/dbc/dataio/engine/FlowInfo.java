package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

public class FlowInfo {
    private static final Logger log = LoggerFactory.getLogger(FlowInfo.class);

    private final String name;

    public FlowInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /*
    public void setName(String name) {
        this.name = name;
    }
    */

    public String toJson() {
        final StringWriter stringWriter = new StringWriter();
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(stringWriter, this);
        } catch (IOException e) {
            log.error("Exception caught when trying to marshall FlowInfo object {} to JSON", name, e);
        }
        return stringWriter.toString();
    }

    @JsonCreator
    public static FlowInfo createFlowInfo(@JsonProperty("name") String name) {
        return new FlowInfo(name);
    }

    public static FlowInfo fromJson(String json) {
        FlowInfo flowInfo = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            flowInfo = objectMapper.readValue(json, FlowInfo.class);
        } catch (IOException e) {
            log.error("Exception caught when trying to unmarshall JSON {} to FLowInfo object", json, e);
        }
        return flowInfo;
    }
}
