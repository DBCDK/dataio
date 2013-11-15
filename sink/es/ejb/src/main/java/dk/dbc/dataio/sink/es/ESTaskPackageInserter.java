package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.types.Pair;
import dk.dbc.dataio.commons.types.ChunkResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
// import dk.dbc.dataio.jobstore.types.ProcessChunkResult;
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
     *
     * @param conn
     * @param dbname
     * @param chunkResult
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    public ESTaskPackageInserter(Connection conn, String dbname, ChunkResult chunkResult) throws SQLException, IOException {
        this.jobId = chunkResult.getJobId();
        this.chunkId = chunkResult.getChunkId();

        String creator = createCreatorString(jobId, chunkId);
        List<AddiRecord> addiRecords = new ArrayList<>();
        for(String result : chunkResult.getResults()) {
            AddiReader addiReader = new AddiReader(new ByteArrayInputStream(result.getBytes(chunkResult.getEncoding())));
            // todo: ensure that there is only one AddiRecord per result.
            addiRecords.add(addiReader.getNextRecord());
        }
        Pair<Integer, Integer> result = ESUtil.insertAddiList(conn, addiRecords, dbname, chunkResult.getEncoding(), creator);
        // todo: Change returnvalue of insertAddiList from Pair to a real object
        this.targetReference = result.getFirst();
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
}
