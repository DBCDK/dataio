package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RecordIdFileTest {
    @Test
    public void iterator() {
        final BinaryFileFsImpl file = new BinaryFileFsImpl(
                Paths.get("src/test/resources/record-ids.txt"));

        final List<RecordIdDTO> expectedRecordIds = Arrays.asList(
                new RecordIdDTO("id1", 123456),
                new RecordIdDTO("id2", 123456),
                new RecordIdDTO("id3", 123456),
                new RecordIdDTO("id4", 123456),
                new RecordIdDTO("id5", 123456),
                new RecordIdDTO("id6", 987654),
                null,
                new RecordIdDTO("id7", 987654),
                new RecordIdDTO("id8", 987654),
                new RecordIdDTO("id9", 987654),
                new RecordIdDTO("id10", 987654));

        int idx = 0;
        try (RecordIdFile recordIdFile = new RecordIdFile(file)) {
            for (RecordIdDTO recordId : recordIdFile) {
                assertThat("entry: " + idx, recordId, is(expectedRecordIds.get(idx)));
                idx++;
            }
        }
    }
}
