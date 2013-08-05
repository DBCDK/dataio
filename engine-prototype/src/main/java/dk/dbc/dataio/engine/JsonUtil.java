package dk.dbc.dataio.engine;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

public class JsonUtil {
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

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

    public static <T> T fromJson(String json, Class<T> tClass) {
        T object = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            object = objectMapper.readValue(json, tClass);
        } catch (IOException e) {
            log.error("Exception caught when trying to unmarshall JSON {} to {} object", json, tClass.getName(), e);
        }
        return object;
    }
}
