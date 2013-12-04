package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ESTaskPackageUtil {

    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ESTaskPackageUtil.class);
    private static final String ES_TASKPACKAGE_CREATOR_FIELD_PREFIX = "dataio: ";

    public static List<TaskStatus> findCompletionStatusForTaskpackages(Connection conn, List<Integer> targetreferences) throws SQLException {
        LOGGER.entry();
        try {
            // Tested through integrationtests
            final String retrieveStatement = "select targetreference, taskstatus from taskpackage where targetreference in ("
                    + commaSeparatedQuestionMarks(targetreferences.size()) + ")";
            List<TaskStatus> taskStatusList = new ArrayList<>();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = JDBCUtil.query(conn, retrieveStatement, targetreferences.toArray());
                rs = ps.getResultSet();
                while (rs.next()) {
                    int targetreference = rs.getInt(1);
                    int statusCode = rs.getInt(2);
                    taskStatusList.add(new TaskStatus(statusCode, targetreference));
                }
            } catch (SQLException ex) {
                LOGGER.warn("SQLException caught while trying to find completion status for taskpackages with retrieve statement: {}", retrieveStatement, ex);
                throw ex;
            } finally {
                JDBCUtil.closeResultSet(rs);
                JDBCUtil.closeStatement(ps);
            }
            return taskStatusList;
        } finally {
            LOGGER.exit();
        }
    }

    // Tested through integrationtests
    public static void deleteTaskpackages(Connection conn, List<Integer> targetReferences) throws SQLException {
        LOGGER.entry();
        try {
            if(! targetReferences.isEmpty()) {
                String deleteStatement = "delete from taskpackage where targetreference in (" +
			commaSeparatedQuestionMarks(targetReferences.size()) + ")";
                LOGGER.trace(deleteStatement);
	        LOGGER.info("targetRefs to delete: {}", Arrays.toString(targetReferences.toArray()));
                PreparedStatement ps = JDBCUtil.query(conn, deleteStatement, targetReferences.toArray());
                JDBCUtil.closeStatement(ps);
            }
        } catch(SQLException ex) {
            LOGGER.warn("SQLException caught while deleting taskpackages: ", ex);
        } finally {
            LOGGER.exit();
        }
    }

    private static String commaSeparatedQuestionMarks(int size) {
        LOGGER.entry();
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append(i < size - 1 ? "?, " : "?");
            }
            return sb.toString();
        } finally {
            LOGGER.exit();
        }
    }

    private static String decodeBase64(String dataToDecode, Charset charset) {
        return new String(Base64.decodeBase64(dataToDecode), charset);
    }

    /**
     * Extracts addi-records from given chunk result.
     *
     * Records in chunk result are assumed to be base64 encoded.
     *
     * @param chunkResult Object containing base64 encoded addi-records
     *
     * @return list of AddiRecord objects.
     *
     * @throws IOException if an error occurs during reading of the addi-data.
     * @throws IllegalStateException if any contained addi-records are invalid.
     * @throws NumberFormatException if any contained records are not addi-format or not base64 encoded.
     */
    public static List<AddiRecord> getAddiRecordsFromChunk(ChunkResult chunkResult) throws IllegalStateException, NumberFormatException, IOException {
        final List<AddiRecord> addiRecords = new ArrayList<>();
        for (String result : chunkResult.getResults()) {
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(decodeBase64(result, chunkResult.getEncoding()).getBytes(chunkResult.getEncoding())));
            addiRecords.add(addiReader.getNextRecord());
            if (addiReader.getNextRecord() != null) {
                throw new IllegalStateException(String.format("More than one Addi in record in: [jobId, chunkId] [%d, %d]", chunkResult.getJobId(), chunkResult.getChunkId()));
            }
        }
        return addiRecords;
    }

    /**
     * Inserts the addi-records contained in given workload into the ES-base with the given dbname.
     *
     * @param esConn Database connection to the ES-base.
     * @param dbname ES internal database name in which the task package shall be associated.
     * @param esWorkload Object containing both the addi-records to insert into the ES-base and the originating chunk result.
     *
     * @throws SQLException if a database error occurs.
     * @throws IllegalStateException if the number of records in the chunk and the task package differ.
     */
    public static int insertTaskPackage(Connection esConn, String dbname, EsWorkload esWorkload) throws IllegalStateException, SQLException {
        InvariantUtil.checkNotNullOrThrow(esConn, "esConn");
        InvariantUtil.checkNotNullNotEmptyOrThrow(dbname, "dbname");
        InvariantUtil.checkNotNullOrThrow(esWorkload, "esInFlight");
        final String creator = createCreatorString(esWorkload.getChunkResult().getJobId(), esWorkload.getChunkResult().getChunkId());
        final ESUtil.AddiListInsertionResult insertionResult = ESUtil.insertAddiList(esConn, esWorkload.getAddiRecords(), dbname, esWorkload.getChunkResult().getEncoding(), creator);
        validateTaskPackageState(insertionResult, esWorkload);
        return insertionResult.getTargetReference();
    }

    private static void validateTaskPackageState(ESUtil.AddiListInsertionResult insertionResult, EsWorkload esWorkload) throws IllegalStateException {
        final int recordSlots = esWorkload.getAddiRecords().size();
        if (recordSlots != insertionResult.getNumberOfInsertedRecords()) {
            throw new IllegalStateException(String.format("The number of records in the chunk and the number of records in the taskpackage differ. Chunk size: %d  TaskPackage size: %d", recordSlots, insertionResult.getNumberOfInsertedRecords()));
        }
    }

    private static String createCreatorString(long jobId, long chunkId) {
        final StringBuilder sb = new StringBuilder();
        sb.append(ES_TASKPACKAGE_CREATOR_FIELD_PREFIX);
        sb.append("[ jobid: ").append(jobId);
        sb.append(" , chunkId: ").append(chunkId).append(" ]");
        return sb.toString();
    }

    public static class TaskStatus {

        private final Code taskStatus;
        private final int targetreference;

        public TaskStatus(int taskStatus, int targetreference) {
            this.taskStatus = Code.getCode(taskStatus);
            this.targetreference = targetreference;
        }

        public Code getTaskStatus() {
            return taskStatus;
        }

        public int getTargetReference() {
            return targetreference;
        }

        public static enum Code {

            PENDING, ACTIVE, COMPLETE, ABORTED;

            private Code() {
            }

            private static Code getCode(int i) {
                switch (i) {
                    case 0:
                        return PENDING;
                    case 1:
                        return ACTIVE;
                    case 2:
                        return COMPLETE;
                    case 3:
                        return ABORTED;
                    default:
                        throw new IllegalArgumentException(i + " is not a valid taskstatus code.");
                }
            }
        }
    }
}
