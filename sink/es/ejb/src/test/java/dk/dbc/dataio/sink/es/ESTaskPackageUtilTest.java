package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkResult;
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
import org.apache.commons.codec.binary.Base64;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JDBCUtil.class,
    ESUtil.class,})
public class ESTaskPackageUtilTest {

    @Test
    public void test() throws SQLException {
        int targetReference = 42;
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);
        mockStatic(JDBCUtil.class);
        when(JDBCUtil.query(any(Connection.class), any(String.class), eq(targetReference))).thenReturn(mockStmt);
        when(mockStmt.getResultSet()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);// true first iteration, false second iteration
        when(mockRs.getInt(eq(1))).thenReturn(2);
        when(mockRs.getInt(eq(2))).thenReturn(targetReference);

        List<ESTaskPackageUtil.TaskStatus> status = ESTaskPackageUtil.findCompletionStatusForTaskpackages(mockConn, Arrays.asList(targetReference));

        assertThat(status.size(), is(1));
        ESTaskPackageUtil.TaskStatus taskStatus = status.get(0);
        assertThat(taskStatus.getTargetReference(), is(42));
        assertThat(taskStatus.getTaskStatus(), is(ESTaskPackageUtil.TaskStatus.Code.COMPLETE));
    }

    @Test(expected = IllegalStateException.class)
    public void getAddiRecordsFromChunk_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final ChunkResult chunkResult = newChunkResult(addiWithTwoRecords);
        ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordsFromChunk_notAddi_throws() throws Exception {
        final String notAddi = "string";
        final ChunkResult chunkResult = newChunkResult(notAddi);
        ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordsFromChunk_recordInChunkNotBase64Encoded_throws() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkResult chunkResult = new ChunkResult(JOB_ID, CHUNK_ID, ENCODING, Arrays.asList(simpleAddiString));
        ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult);
    }

    @Test
    public void getAddiRecordsFromChunk_singleSimpleRecordInChunk_happyPath() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkResult chunkResult = newChunkResult(simpleAddiString);
        final List<AddiRecord> addiRecordsFromChunk = ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult);
        assertThat(addiRecordsFromChunk.size(), is(1));
    }

    @Test
    public void insertTaskPackage_singleSimpleRecordInWorkload_happyPath() throws Exception {
        mockStatic(ESUtil.class);
        final int expectedTargetReference = 12345;
        final Connection esConn = mock(Connection.class);
        final String simpleAddiString = "1\na\n1\nb\n";
        final EsWorkload esWorkload = newEsWorkload(simpleAddiString);
        final ESUtil.AddiListInsertionResult addiListInsertionResult = new ESUtil.AddiListInsertionResult(expectedTargetReference, 1);
        when(ESUtil.insertAddiList(any(Connection.class), any(ArrayList.class), any(String.class), any(Charset.class), any(String.class))).thenReturn(addiListInsertionResult);

        assertThat(ESTaskPackageUtil.insertTaskPackage(esConn, DB_NAME, esWorkload), is(expectedTargetReference));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_connectionArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(null, DB_NAME, newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_dbnameArgIsNull_throws() throws Exception {
        final Connection esConn = mock(Connection.class);
        ESTaskPackageUtil.insertTaskPackage(esConn, null, newEsWorkload(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertTaskPackage_dbnameArgIsEmpty_throws() throws Exception {
        final Connection esConn = mock(Connection.class);
        ESTaskPackageUtil.insertTaskPackage(esConn, "", newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_esWorkloadArgIsNull_throws() throws Exception {
        final Connection esConn = mock(Connection.class);
        ESTaskPackageUtil.insertTaskPackage(esConn, DB_NAME, null);
    }

    @Test(expected = IllegalStateException.class)
    public void insertTaskPackage_mismatchBetweenNumberOfInsertedRecordsAndRecordsInWorkload_throws() throws Exception {
        mockStatic(ESUtil.class);
        final Connection esConn = mock(Connection.class);
        final String simpleAddiString = "1\na\n1\nb\n";
        final EsWorkload esWorkload = newEsWorkload(simpleAddiString);
        final ESUtil.AddiListInsertionResult addiListInsertionResult = new ESUtil.AddiListInsertionResult(12345, 0);
        when(ESUtil.insertAddiList(any(Connection.class), any(ArrayList.class), any(String.class), any(Charset.class), any(String.class))).thenReturn(addiListInsertionResult);

        ESTaskPackageUtil.insertTaskPackage(esConn, DB_NAME, esWorkload);
    }

    @Test
    public void taskStatus_PendingCode() {
        final int targetReference = 42;
        ESTaskPackageUtil.TaskStatus ts = new ESTaskPackageUtil.TaskStatus(0, targetReference);
        assertThat(ts.getTaskStatus(), is(ESTaskPackageUtil.TaskStatus.Code.PENDING));
        assertThat(ts.getTargetReference(), is(targetReference));
    }

    @Test
    public void taskStatus_ActiveCode() {
        final int targetReference = 42;
        ESTaskPackageUtil.TaskStatus ts = new ESTaskPackageUtil.TaskStatus(1, targetReference);
        assertThat(ts.getTaskStatus(), is(ESTaskPackageUtil.TaskStatus.Code.ACTIVE));
        assertThat(ts.getTargetReference(), is(targetReference));
    }

    @Test
    public void taskStatus_CompleteCode() {
        final int targetReference = 42;
        ESTaskPackageUtil.TaskStatus ts = new ESTaskPackageUtil.TaskStatus(2, targetReference);
        assertThat(ts.getTaskStatus(), is(ESTaskPackageUtil.TaskStatus.Code.COMPLETE));
        assertThat(ts.getTargetReference(), is(targetReference));
    }

    @Test
    public void taskStatus_AbortedCode() {
        final int targetReference = 42;
        ESTaskPackageUtil.TaskStatus ts = new ESTaskPackageUtil.TaskStatus(3, targetReference);
        assertThat(ts.getTaskStatus(), is(ESTaskPackageUtil.TaskStatus.Code.ABORTED));
        assertThat(ts.getTargetReference(), is(targetReference));
    }

    @Test(expected = IllegalArgumentException.class)
    public void taskStatus_IllegalCode_throws() {
        final int targetReference = 42;
        new ESTaskPackageUtil.TaskStatus(5, targetReference);
    }


    private EsWorkload newEsWorkload(String record) throws IOException {
        return new EsWorkload(newChunkResult(record), Arrays.asList(newAddiRecordFromString(record)));
    }

    private ChunkResult newChunkResult(String record) {
        return new ChunkResult(JOB_ID, CHUNK_ID, ENCODING, Arrays.asList(encodeBase64(record)));
    }

    private AddiRecord newAddiRecordFromString(String record) throws IOException {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(record.getBytes(ENCODING)));
        return addiReader.getNextRecord();
    }

    private String encodeBase64(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes(ENCODING));
    }

    private static final long JOB_ID = 11L;
    private static final long CHUNK_ID = 17L;
    private static final Charset ENCODING = Charset.defaultCharset();
    private static final String DB_NAME = "dbname";

}
