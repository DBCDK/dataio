package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiWriter;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ChunkResult;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.mockito.Mockito.any;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.hamcrest.CoreMatchers.is;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ESUtil.class
})
public class ESTaskPackageInserterTest {

    @Test
    public void happyPathWithSingleSimpleRecordInChunk() throws Exception {
        Connection esConn = mock(Connection.class);
        mockStatic(ESUtil.class);
        String simpleAddiString = "1\na\n1\nb\n";
        ChunkResult chunkResult = new ChunkResult(11L, 17L, Charset.defaultCharset(), Arrays.asList(simpleAddiString));
        ESUtil.AddiListInsertionResult addiListInsertionResult = new ESUtil.AddiListInsertionResult(12345, 1);
        when(ESUtil.insertAddiList(any(Connection.class), any(ArrayList.class), any(String.class), any(Charset.class), any(String.class))).thenReturn(addiListInsertionResult);
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(esConn, "DBName", chunkResult);
        assertThat(inserter.getJobId(), is(11L));
        assertThat(inserter.getChunkId(), is(17L));
        assertThat(inserter.getTargetReference(), is(12345));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_connectionArgIsNull_throws() throws Exception {
        ChunkResult chunkResult = new ChunkResult(11L, 17L, Charset.defaultCharset(), Collections.EMPTY_LIST);
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(null, "DBName", chunkResult);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dbnameArgIsNull_throws() throws Exception {
        Connection esConn = mock(Connection.class);
        ChunkResult chunkResult = new ChunkResult(11L, 17L, Charset.defaultCharset(), Collections.EMPTY_LIST);
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(esConn, null, chunkResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_dbnameArgIsEmpty_throws() throws Exception {
        Connection esConn = mock(Connection.class);
        ChunkResult chunkResult = new ChunkResult(11L, 17L, Charset.defaultCharset(), Collections.EMPTY_LIST);
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(esConn, "", chunkResult);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_chunkResultArgIsNull_throws() throws Exception {
        Connection esConn = mock(Connection.class);
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(esConn, "DBName", null);
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_twoAddiInOneRecord_throws() throws Exception {
        Connection esConn = mock(Connection.class);
        String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        ChunkResult chunkResult = new ChunkResult(11L, 17L, Charset.defaultCharset(), Arrays.asList(addiWithTwoRecords));
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(esConn, "DBName", chunkResult);
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_mismatchBetweenNumberOfInsertedRecordsAndRecordsInChunk_throws() throws Exception {
        Connection esConn = mock(Connection.class);
        mockStatic(ESUtil.class);
        String simpleAddiString = "1\na\n1\nb\n";
        ChunkResult chunkResult = new ChunkResult(11L, 17L, Charset.defaultCharset(), Arrays.asList(simpleAddiString));
        ESUtil.AddiListInsertionResult addiListInsertionResult = new ESUtil.AddiListInsertionResult(12345, 0);
        when(ESUtil.insertAddiList(any(Connection.class), any(ArrayList.class), any(String.class), any(Charset.class), any(String.class))).thenReturn(addiListInsertionResult);
        ESTaskPackageInserter inserter = new ESTaskPackageInserter(esConn, "DBName", chunkResult);
    }
}
