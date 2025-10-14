package dk.dbc.dataio.harvester.utils.datafileverifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.JsonReader;
import dk.dbc.marc.reader.MarcReaderException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DM3CollectionExpectation extends JSonExpectation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public final Set<MarcExchangeRecordExpectation> records;

    public DM3CollectionExpectation(MarcExchangeRecordExpectation... records) {
        this.records = Stream.of(records).collect(Collectors.toSet());
    }

    @Override
    public void verify(String json) {
        Set<MarcExchangeRecordExpectation> actualRecords = splitRecords(json).stream()
                .map(Object::toString)
                .map(this::toMarcRecord)
                .map(MarcExchangeRecordExpectation::new)
                .collect(Collectors.toSet());
        for (MarcExchangeRecordExpectation expectation : records) {
            assertThat(expectation.toString(), actualRecords.remove(expectation), is(true));
        }
        assertThat("All records accounted for", actualRecords.isEmpty(), is(true));
    }

    private List<String> splitRecords(String json) {
        List<String> jsonRecords = new ArrayList<>();
        try {
            JsonNode node = MAPPER.readTree(json);
            if(node.isArray()) {
                for (int i = 0; i < node.size(); i++) {
                    jsonRecords.add(node.get(i).toString());
                }
            } else jsonRecords.add(json);
            return jsonRecords;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private MarcRecord toMarcRecord(String record) {
        try {
            return new JsonReader(new ByteArrayInputStream(record.getBytes(StandardCharsets.UTF_8))).read();
        } catch (MarcReaderException e) {
            throw new IllegalStateException(e);
        }
    }
}
