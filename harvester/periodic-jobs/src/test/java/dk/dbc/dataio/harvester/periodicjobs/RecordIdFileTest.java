/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.rawrepo.RecordId;
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

        final List<RecordId> expectedRecordIds = Arrays.asList(
                new RecordId("id1", 123456),
                new RecordId("id2", 123456),
                new RecordId("id3", 123456),
                new RecordId("id4", 123456),
                new RecordId("id5", 123456),
                new RecordId("id6", 987654),
                null,
                new RecordId("id7", 987654),
                new RecordId("id8", 987654),
                new RecordId("id9", 987654),
                new RecordId("id10", 987654));

        int idx = 0;
        try (RecordIdFile recordIdFile = new RecordIdFile(file)) {
            for (RecordId recordId : recordIdFile) {
                assertThat("entry: " + idx, recordId, is(expectedRecordIds.get(idx)));
                idx++;
            }
        }
    }
}