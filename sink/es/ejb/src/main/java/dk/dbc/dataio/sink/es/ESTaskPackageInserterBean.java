package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.apache.commons.codec.binary.Base64;

import javax.ejb.Stateless;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class ESTaskPackageInserterBean {
    private static final String ES_TASKPACKAGE_CREATOR_FIELD_PREFIX = "dataio: ";

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
    public int insertTaskPackage(Connection esConn, String dbname, EsWorkload esWorkload) throws IllegalStateException, SQLException {
        InvariantUtil.checkNotNullOrThrow(esConn, "esConn");
        InvariantUtil.checkNotNullNotEmptyOrThrow(dbname, "dbname");
        InvariantUtil.checkNotNullOrThrow(esWorkload, "esInFlight");
        final String creator = createCreatorString(esWorkload.getChunkResult().getJobId(), esWorkload.getChunkResult().getChunkId());
        final ESUtil.AddiListInsertionResult insertionResult =
                ESUtil.insertAddiList(esConn, esWorkload.getAddiRecords(), dbname, esWorkload.getChunkResult().getEncoding(), creator);
        validateTaskPackageState(insertionResult, esWorkload);
        return insertionResult.getTargetReference();
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
    public List<AddiRecord> getAddiRecordsFromChunk(ChunkResult chunkResult) throws IllegalStateException, NumberFormatException, IOException {
        final List<AddiRecord> addiRecords = new ArrayList<>();
        for (String result : chunkResult.getResults()) {
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(decodeBase64(result, chunkResult.getEncoding())
                            .getBytes(chunkResult.getEncoding())));
            addiRecords.add(addiReader.getNextRecord());
            if (addiReader.getNextRecord() != null) {
                throw new IllegalStateException(String.format("More than one Addi in record in: [jobId, chunkId] [%d, %d]",
                        chunkResult.getJobId(), chunkResult.getChunkId()));
            }
        }
        return addiRecords;
    }

    private String createCreatorString(long jobId, long chunkId) {
        final StringBuilder sb = new StringBuilder();
        sb.append(ES_TASKPACKAGE_CREATOR_FIELD_PREFIX);
        sb.append("[ jobid: ").append(jobId);
        sb.append(" , chunkId: ").append(chunkId).append(" ]");
        return sb.toString();
    }

    private void validateTaskPackageState(ESUtil.AddiListInsertionResult insertionResult, EsWorkload esWorkload) throws IllegalStateException {
        final int recordSlots = esWorkload.getAddiRecords().size();
        if (recordSlots != insertionResult.getNumberOfInsertedRecords()) {
            throw new IllegalStateException(String.format("The number of records in the chunk and the number of records in the taskpackage differ. Chunk size: %d  TaskPackage size: %d",
                    recordSlots, insertionResult.getNumberOfInsertedRecords()));
        }
    }

    private String decodeBase64(String dataToDecode, Charset charset) {
        return new String(Base64.decodeBase64(dataToDecode), charset);
    }
}
