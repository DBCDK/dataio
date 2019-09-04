/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.rawrepo.RecordData;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RecordIdFileTest {
    @Test
    public void iterator() {
        final BinaryFileFsImpl file = new BinaryFileFsImpl(
                Paths.get("src/test/resources/record-ids.txt"));

        final List<RecordData.RecordId> expectedRecordIds = Arrays.asList(
                new RecordData.RecordId("id1", 123456),
                new RecordData.RecordId("id2", 123456),
                new RecordData.RecordId("id3", 123456),
                new RecordData.RecordId("id4", 123456),
                new RecordData.RecordId("id5", 123456),
                new RecordData.RecordId("id6", 987654),
                null,
                new RecordData.RecordId("id7", 987654),
                new RecordData.RecordId("id8", 987654),
                new RecordData.RecordId("id9", 987654),
                new RecordData.RecordId("id10", 987654));

        int idx = 0;
        try (RecordIdFile recordIdFile = new RecordIdFile(file)) {
            for (RecordData.RecordId recordId : recordIdFile) {
                assertThat("entry: " + idx, recordId, is(expectedRecordIds.get(idx)));
                idx++;
            }
        }
    }
}