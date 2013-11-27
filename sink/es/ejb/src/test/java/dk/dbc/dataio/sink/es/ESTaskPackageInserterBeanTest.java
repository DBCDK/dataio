package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ChunkResult;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ESUtil.class
})
public class ESTaskPackageInserterBeanTest {
    private static final long JOB_ID = 11L;
    private static final long CHUNK_ID = 17L;
    private static final Charset ENCODING = Charset.defaultCharset();
    private static final String DB_NAME = "dbname";

    @Test
    public void insertTaskPackage_singleSimpleRecordInWorkload_happyPath() throws Exception {
        mockStatic(ESUtil.class);
        final int expectedTargetReference = 12345;
        final Connection esConn = mock(Connection.class);
        final String simpleAddiString = "1\na\n1\nb\n";
        final EsWorkload esWorkload = newEsWorkload(simpleAddiString);
        final ESUtil.AddiListInsertionResult addiListInsertionResult = new ESUtil.AddiListInsertionResult(expectedTargetReference, 1);
        when(ESUtil.insertAddiList(any(Connection.class), any(ArrayList.class), any(String.class), any(Charset.class), any(String.class))).thenReturn(addiListInsertionResult);

        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        assertThat(inserter.insertTaskPackage(esConn, DB_NAME, esWorkload), is(expectedTargetReference));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_connectionArgIsNull_throws() throws Exception {
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.insertTaskPackage(null, DB_NAME, newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_dbnameArgIsNull_throws() throws Exception {
        final Connection esConn = mock(Connection.class);
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.insertTaskPackage(esConn, null, newEsWorkload(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertTaskPackage_dbnameArgIsEmpty_throws() throws Exception {
        final Connection esConn = mock(Connection.class);
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.insertTaskPackage(esConn, "", newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_esWorkloadArgIsNull_throws() throws Exception {
        final Connection esConn = mock(Connection.class);
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.insertTaskPackage(esConn, DB_NAME, null);
    }

    @Test(expected = IllegalStateException.class)
    public void insertTaskPackage_mismatchBetweenNumberOfInsertedRecordsAndRecordsInWorkload_throws() throws Exception {
        mockStatic(ESUtil.class);
        final Connection esConn = mock(Connection.class);
        final String simpleAddiString = "1\na\n1\nb\n";
        final EsWorkload esWorkload = newEsWorkload(simpleAddiString);
        final ESUtil.AddiListInsertionResult addiListInsertionResult = new ESUtil.AddiListInsertionResult(12345, 0);
        when(ESUtil.insertAddiList(any(Connection.class), any(ArrayList.class), any(String.class), any(Charset.class), any(String.class))).thenReturn(addiListInsertionResult);

        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.insertTaskPackage(esConn, DB_NAME, esWorkload);
    }

    @Test(expected = IllegalStateException.class)
    public void getAddiRecordsFromChunk_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final ChunkResult chunkResult = newChunkResult(addiWithTwoRecords);
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.getAddiRecordsFromChunk(chunkResult);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordsFromChunk_notAddi_throws() throws Exception {
        final String notAddi = "string";
        final ChunkResult chunkResult = newChunkResult(notAddi);
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.getAddiRecordsFromChunk(chunkResult);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordsFromChunk_recordInChunkNotBase64Encoded_throws() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkResult chunkResult = new ChunkResult(JOB_ID, CHUNK_ID, ENCODING, Arrays.asList(simpleAddiString));
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        inserter.getAddiRecordsFromChunk(chunkResult);
    }

    @Test
    public void getAddiRecordsFromChunk_singleSimpleRecordInChunk_happyPath() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkResult chunkResult = newChunkResult(simpleAddiString);
        final ESTaskPackageInserterBean inserter = new ESTaskPackageInserterBean();
        final List<AddiRecord> addiRecordsFromChunk = inserter.getAddiRecordsFromChunk(chunkResult);
        assertThat(addiRecordsFromChunk.size(), is(1));
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
}
