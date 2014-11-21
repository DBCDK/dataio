package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JDBCUtil.class,
    ESUtil.class,})
public class ESTaskPackageUtilTest {
    private static final long JOB_ID = 11L;
    private static final long CHUNK_ID = 17L;
    private static final Charset ENCODING = Charset.defaultCharset();
    private static final String DB_NAME = "dbname";

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
        when(mockRs.getInt(eq(1))).thenReturn(targetReference);
        when(mockRs.getInt(eq(2))).thenReturn(2);

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
        final ChunkItem item = new ChunkItemBuilder()
                .setData(simpleAddiString)
                .build();
        final ChunkResult chunkResult = new ChunkResult(JOB_ID, CHUNK_ID, ENCODING, Arrays.asList(item));
        ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult);
    }

    @Test
    public void getAddiRecordsFromChunk_singleSimpleRecordInChunk_happyPath() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkResult chunkResult = newChunkResult(simpleAddiString);
        final List<AddiRecord> addiRecordsFromChunk = ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult);
        assertThat(addiRecordsFromChunk.size(), is(1));
    }

    @Test(expected = NullPointerException.class)
    public void getAddiRecordFromChunkItem_chunkItemArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.getAddiRecordFromChunkItem(null, StandardCharsets.UTF_8);
    }

    @Test(expected = NullPointerException.class)
    public void getAddiRecordFromChunkItem_encodingArgIsNull_throws() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkItem chunkItem = newChunkItem(simpleAddiString);
        ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, null);
    }

    @Test(expected = IllegalStateException.class)
    public void getAddiRecordFromChunkItem_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final ChunkItem chunkItem = newChunkItem(addiWithTwoRecords);
        ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, StandardCharsets.UTF_8);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordFromChunkItem_chunkItemArgContainsNonAddiData_throws() throws Exception {
        final ChunkItem chunkItem = newChunkItem("non-addi");
        ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, StandardCharsets.UTF_8);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordFromChunkItem_chunkItemArgContainsNonBase64EncodedAddi_throws() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setData(simpleAddiString)
                .build();
        ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, StandardCharsets.UTF_8);
    }

    @Test
    public void getAddiRecordFromChunkItem_chunkItemArgContainsValidAddi_returnsAddiRecordInstance() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkItem chunkItem = newChunkItem(simpleAddiString);
        final AddiRecord addiRecord = ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, StandardCharsets.UTF_8);
        assertThat(addiRecord, is(notNullValue()));
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

    @Test(expected = NullPointerException.class)
    public void chopUp_listArgIsNull_throws() {
        ESTaskPackageUtil.chopUp(null, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void chopUp_sublistSizeArgIsZero_throws() {
        ESTaskPackageUtil.chopUp(Collections.emptyList(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void chopUp_sublistSizeArgIsLessThanZero_throws() {
        ESTaskPackageUtil.chopUp(Collections.emptyList(), -1);
    }

    @Test
    public void chopUp_listArgIsEmpty_returnEmptyList() {
        final List<List<Object>> lists = ESTaskPackageUtil.chopUp(Collections.emptyList(), 42);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.isEmpty(), is(true));
    }

    @Test
    public void chopUp_sublistSizeArgIsLargerThanActualListSize_returnsSinglePart() {
        final List<Integer> integers = Arrays.asList(1, 2, 3);
        final List<List<Integer>> lists = ESTaskPackageUtil.chopUp(integers, 42);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(1));
        assertThat(lists.get(0), is(integers));
    }

    @Test
    public void chopUp_sublistSizeArgEqualsActualListSize_returnsSinglePart() {
        final List<Integer> integers = Arrays.asList(1, 2, 3);
        final List<List<Integer>> lists = ESTaskPackageUtil.chopUp(integers, integers.size());
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(1));
        assertThat(lists.get(0), is(integers));
    }

    @Test
    public void chopUp_sublistSizeArgIsLessThatActualListSize_returnsMultipleParts() {
        final List<Integer> integers = Arrays.asList(1, 2, 3, 4);

        List<List<Integer>> lists = ESTaskPackageUtil.chopUp(integers, 2);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(2));
        assertThat(lists.get(0), is(integers.subList(0, 2)));
        assertThat(lists.get(1), is(integers.subList(2, 4)));

        lists = ESTaskPackageUtil.chopUp(integers, 3);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(2));
        assertThat(lists.get(0), is(integers.subList(0, 3)));
        assertThat(lists.get(1), is(integers.subList(3, 4)));
    }

    private EsWorkload newEsWorkload(String record) throws IOException {
        return new EsWorkload(new SinkChunkResultBuilder().build(),
                Arrays.asList(newAddiRecordFromString(record)));
    }

    private ChunkItem newChunkItem(String record) {
        return new ChunkItemBuilder()
                .setData(encodeBase64(record))
                .build();
    }

    private ChunkResult newChunkResult(String record) {
        return new ChunkResult(JOB_ID, CHUNK_ID, ENCODING, Arrays.asList(
                newChunkItem(record)));
    }

    private AddiRecord newAddiRecordFromString(String record) throws IOException {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(record.getBytes(ENCODING)));
        return addiReader.getNextRecord();
    }

    private String encodeBase64(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes(ENCODING));
    }
}
