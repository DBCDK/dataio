package dk.dbc.dataio.harvester.dmatdm3;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.rawrepo.dto.RecordEntryDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ResourceFetcher {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static String getResourceAsStringFromFile(String id, String path) throws IOException {
        return  Files.readString(
                Path.of(Objects.requireNonNull(ResourceFetcher.class.getResource(path + id + ".json"))
                        .getPath()));
    }

    public static DMatRecord getDmatRecordFromFile(String id) throws IOException {
            return mapper.readValue(getResourceAsStringFromFile(id, "/dmatrecords/"), DMatRecord.class);
    }

    public static List<RecordEntryDTO> getRecordEntryDTOsFromFile(String id) throws IOException {
        return mapper.readValue(getResourceAsStringFromFile(id, "/recordcollections/"),
                mapper.getTypeFactory().constructCollectionType(List.class, RecordEntryDTO.class));

    }

}
