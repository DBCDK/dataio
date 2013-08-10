package dk.dbc.dataio.engine;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class JsonUtil {
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);
    private static Map<Class<?>, Class<?>> mixIns = new HashMap<>();
    static {
        mixIns.put(Flow.class, FlowJsonMixIn.class);
        mixIns.put(FlowContent.class, FlowContentJsonMixIn.class);
        mixIns.put(FlowComponent.class, FlowComponentJsonMixIn.class);
        mixIns.put(FlowComponentContent.class, FlowComponentContentJsonMixIn.class);
        mixIns.put(Chunk.class, ChunkJsonMixIn.class);
        mixIns.put(ProcessChunkResult.class, ProcessChunkResultJsonMixIn.class);
        mixIns.put(JavaScript.class, JavaScriptJsonMixIn.class);
    }

    private JsonUtil() { }

    public static String toJson(Object object) {
        final StringWriter stringWriter = new StringWriter();
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(stringWriter, object);
        } catch (IOException e) {
            log.error("Exception caught when trying to marshall {} object to JSON", object.getClass().getName(), e);
        }
        return stringWriter.toString();
    }

    public static <T> T fromJson(String json, Class<T> tClass, Map<Class<?>, Class<?>> mixIns) {
        T object = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (mixIns != null) {
                for (Map.Entry<Class<?>, Class<?>> e : mixIns.entrySet()) {
                    objectMapper.getDeserializationConfig().addMixInAnnotations(e.getKey(), e.getValue());
                }
            }
            object = objectMapper.readValue(json, tClass);
        } catch (IOException e) {
            log.error("Exception caught when trying to unmarshall JSON {} to {} object", json, tClass.getName(), e);
        }
        return object;
    }

    public static Map<Class<?>, Class<?>> getMixIns() {
        return mixIns;
    }
}
