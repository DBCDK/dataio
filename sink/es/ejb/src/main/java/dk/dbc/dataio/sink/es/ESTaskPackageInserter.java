package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ESTaskPackageInserter {

    private static final String ES_TASKPACKAGE_CREATOR_FIELD_PREFIX = "dataio: ";

    private final int targetReference;
    private final long jobId;
    private final long chunkId;

    /**
     * Inserts the ChunkResult in the ES-base with the given dbname.
     * After successful insertion, the object will contain the jobId, the chunkId and a targetreference.
     *
     * @param esConn Database connection to the ES-base
     * @param dbname ES internal databasename in which the taskpackage shall be associated.
     * @param chunkResult Object containing the addi-records to insert into the ES-base.
     * @throws java.sql.SQLException if a database error occurs.
     * @throws java.io.IOException if an error occurs during reading of the addi-data.
     */
    public ESTaskPackageInserter(Connection esConn, String dbname, ChunkResult chunkResult) throws IllegalStateException, IOException, SQLException{
        this.jobId = chunkResult.getJobId();
        this.chunkId = chunkResult.getChunkId();
        String creator = createCreatorString(jobId, chunkId);
        List<AddiRecord> addiRecords = getAddiRecordsFromChunk(chunkResult);
        ESUtil.AddiListInsertionResult insertionResult = ESUtil.insertAddiList(esConn, addiRecords, dbname, chunkResult.getEncoding(), creator);
        validateTaskPackageState(insertionResult, chunkResult);
        this.targetReference = insertionResult.getTargetReference();
    }

    public int getTargetReference() {
        return targetReference;
    }

    public long getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    private String createCreatorString(long jobId, long chunkId) {
        StringBuilder sb = new StringBuilder();
        sb.append(ES_TASKPACKAGE_CREATOR_FIELD_PREFIX);
        sb.append("[ jobid: ").append(jobId);
        sb.append(" , chunkId: ").append(chunkId).append(" ]");
        return sb.toString();
    }

    private List<AddiRecord> getAddiRecordsFromChunk(ChunkResult chunkResult) throws IOException {
        List<AddiRecord> addiRecords = new ArrayList<>();
        for(String result : chunkResult.getResults()) {
            AddiReader addiReader = new AddiReader(new ByteArrayInputStream(result.getBytes(chunkResult.getEncoding())));
            // todo: ensure that there is only one AddiRecord per result.
            addiRecords.add(addiReader.getNextRecord());
        }
        return addiRecords;
    }

    private void validateTaskPackageState(ESUtil.AddiListInsertionResult insertionResult, ChunkResult chunkResult) throws IllegalStateException {
        int chunkResultSize = chunkResult.getResults().size();
        if(chunkResultSize != insertionResult.getNumberOfInsertedRecords()) {
            String errMsg = String.format("The number of records in the chunk and the number of records in the taskpackage differ. Chunk size: %d  TaskPackage size: %d",
                    chunkResultSize, insertionResult.getNumberOfInsertedRecords());
            throw new IllegalStateException(errMsg);
        }
    }
}
