package dk.dbc.dataio.sink.util;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThomasBerg on 16/10/15.
 */
public class AddiUtil {

    /**
     * Extracts addi-records from given chunk result.
     * <p>
     * Records in chunk result items are assumed to be base64 encoded.
     *
     * @param chunk Object containing base64 encoded addi-record items
     * @return list of AddiRecord objects.
     * @throws IOException           if an error occurs during reading of the addi-data.
     * @throws IllegalStateException if any contained addi-records are invalid.
     * @throws NumberFormatException if any contained records are not
     *                               addi-format or not base64 encoded.
     */
    public static List<AddiRecord> getAddiRecordsFromChunk(Chunk chunk) throws IllegalStateException, NumberFormatException, IOException {
        final List<AddiRecord> addiRecords = new ArrayList<>();
        for (ChunkItem chunkItem : chunk) {
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
            addiRecords.add(addiReader.getNextRecord());
            if (addiReader.getNextRecord() != null) {
                throw new IllegalStateException(String.format("More than one Addi in record in: [jobId, chunkId] [%d, %d]", chunk.getJobId(), chunk.getChunkId()));
            }
        }
        return addiRecords;
    }

    /**
     * Extracts addi-record(s) from given chunk item
     *
     * @param chunkItem Object containing addi-record data
     * @return list of AddiRecord objects
     * @throws NullPointerException  if given any null-valued argument
     * @throws IOException           if an error occurs during reading of the addi-data
     * @throws IllegalStateException if contained addi-record is invalid
     * @throws NumberFormatException if contained record is not addi-format
     */
    public static List<AddiRecord> getAddiRecordsFromChunkItem(ChunkItem chunkItem) throws NullPointerException, IllegalStateException, NumberFormatException, IOException {
        if (chunkItem.getData().length == 0) {
            throw new IllegalStateException("No data in ChunkItem");
        }
        final List<AddiRecord> addiRecords = new ArrayList<>();
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        AddiRecord nextRecord = addiReader.getNextRecord();
        while (nextRecord != null) {
            addiRecords.add(nextRecord);
            nextRecord = addiReader.getNextRecord();
        }
        return addiRecords;
    }
}
