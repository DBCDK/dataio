package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFileUtil.class);

    final Charset LOCAL_CHARSET;

    public JsonFileUtil(Charset charset) {
        LOCAL_CHARSET = charset;
    }

    public static JsonFileUtil getJsonFileUtil(Charset charset) {
        return new JsonFileUtil(charset);
    }

    public <T> T readObjectFromFile(Path objectPath, Class<T> tClass) throws JobStoreException {
        T object;
        try (BufferedReader br = Files.newBufferedReader(objectPath, LOCAL_CHARSET)) {
            final StringBuilder sb = new StringBuilder();
            String data;
            while ((data = br.readLine()) != null) {
                sb.append(data);
            }
            object = JsonUtil.fromJson(sb.toString(), tClass, MixIns.getMixIns());
        } catch (IOException | JsonException e) {
            final String errorMsg = String.format("Exception caught while reading object from Path: %s", objectPath.toString());
            LOGGER.error(errorMsg, e);
            throw new JobStoreException(errorMsg, e);
        }
        return object;
    }

    public <T> void writeObjectToFile(Path objectPath, T object) throws JobStoreException {
        LOGGER.info("Writing JSON file: {}", objectPath);
        try (BufferedWriter bw = Files.newBufferedWriter(objectPath, LOCAL_CHARSET)) {
            bw.write(JsonUtil.toJson(object));
        } catch (IOException | JsonException e) {
            final String errorMsg = String.format("Exception caught when trying to write object to Path: %s", objectPath.toString());
            LOGGER.error(errorMsg, e);
            throw new JobStoreException(errorMsg, e);
        }
    }
}
