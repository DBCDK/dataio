package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.types.Pair;
import java.nio.charset.Charset;
// import dk.dbc.dataio.jobstore.types.ProcessChunkResult;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ESTaskpackageInserter {

    private static final String ES_TASKPACKAGE_CREATOR_FIELD_PREFIX = "dataio: ";

    private final int targetReference;
    private final long jobId;
    private final long chunkId;

    /**
     *
     * @param conn
     * @param dbname
     */
    public ESTaskpackageInserter(Connection conn, String dbname /*, ProcessChunkResult chunkResult*/) throws SQLException {
        this.jobId = 0;
        this.chunkId = 0;

        String creator = createCreatorString(jobId, chunkId);
        // todo: add code:
        // For each result in chunk:
        //   Convert result to AddiRecord and add it to a list
        List<AddiRecord> addiRecords = new ArrayList<>();
        // Insert AddiRecord-list into es using connection
        // todo: Get Charset from chunk
        Pair<Integer, Integer> result = ESUtil.insertAddiList(conn, addiRecords, dbname, Charset.defaultCharset(), creator);
        this.targetReference = result.getFirst();

    }

    private String createCreatorString(long jobId, long chunkId) {
        StringBuilder sb = new StringBuilder();
        sb.append(ES_TASKPACKAGE_CREATOR_FIELD_PREFIX);
        sb.append("[ jobid: ").append(jobId);
        sb.append(" , chunkId: ").append(chunkId).append(" ]");
        return sb.toString();
    }

}
