package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.sink.es.entity.es.DiagnosticsEntity;
import dk.dbc.dataio.sink.es.entity.es.SuppliedRecordsEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.util.AddiUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ESTaskPackageUtilTest {
    private static final long JOB_ID = 11L;
    private static final long CHUNK_ID = 17L;
    private static final Charset ENCODING = Charset.defaultCharset();
    private static String ES_DATABASE_NAME = "dbname";
    private static final String ADDI_OK = "1\na\n1\nb\n";
    private static final String TRACKING_ID = "rr:1234io:5353";

    @Test(expected = IllegalStateException.class)
    public void getAddiRecordsFromChunk_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final Chunk processedChunk = newProcessedChunk(addiWithTwoRecords);
        AddiUtil.getAddiRecordsFromChunk(processedChunk);
    }

    @Test(expected = IOException.class)
    public void getAddiRecordsFromChunk_notAddi_throws() throws Exception {
        final String notAddi = "string";
        final Chunk processedChunk = newProcessedChunk(notAddi);
        AddiUtil.getAddiRecordsFromChunk(processedChunk);
    }

    @Test
    public void getAddiRecordsFromChunk_singleSimpleRecordInChunk_happyPath() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final Chunk processedChunk = newProcessedChunk(simpleAddiString);

        final List<AddiRecord> addiRecordsFromChunk = AddiUtil.getAddiRecordsFromChunk(processedChunk);
        assertThat(addiRecordsFromChunk.size(), is(1));
    }

    @Test(expected = NullPointerException.class)
    public void getAddiRecordsFromChunkItem_chunkItemArgIsNull_throws() throws Exception {
        AddiUtil.getAddiRecordsFromChunkItem(null);
    }

    @Test
    public void getAddiRecordsFromChunkItem_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final ChunkItem chunkItem = newChunkItem(addiWithTwoRecords);
        final List<AddiRecord> addiRecords = AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
        assertThat("Number of Addi records returned", addiRecords.size(), is(2));
        assertThat("first Addi record", addiRecords.get(0), is(notNullValue()));
        assertThat("second Addi record", addiRecords.get(1), is(notNullValue()));
    }

    @Test(expected = IOException.class)
    public void getAddiRecordsFromChunkItem_chunkItemArgContainsNonAddiData_throws() throws Exception {
        final ChunkItem chunkItem = newChunkItem("non-addi");
        AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
    }

    @Test
    public void getAddiRecordsFromChunkItem_chunkItemArgContainsValidAddi_returnsAddiRecordInstance() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkItem chunkItem = newChunkItem(simpleAddiString);
        final List<AddiRecord> addiRecords = AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
        assertThat("Number of Addi records returned", addiRecords.size(), is(1));
        assertThat("Addi record", addiRecords.get(0), is(notNullValue()));
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


    private ChunkItem newChunkItem(String record) {
        return new ChunkItemBuilder()
                .setId(0L)
                .setData(StringUtil.asBytes(record))
                .build();
    }

    private Chunk newProcessedChunk(String record) {
        Chunk processedChunk = new Chunk(JOB_ID, CHUNK_ID, Chunk.Type.PROCESSED);
        processedChunk.insertItem(newChunkItem(record));
        processedChunk.setEncoding(ENCODING);
        return processedChunk;
    }



    /// -- added
    @Test
    public void getChunkForTaskPackage() throws SQLException, ClassNotFoundException, IOException {
        final List<ChunkItem> items = new ArrayList<>(7);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).setTrackingId(TRACKING_ID).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("3")).setTrackingId(TRACKING_ID).build());  // OK multiple
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.FAILURE).setTrackingId(TRACKING_ID).build());
        items.add(new ChunkItemBuilder().setId(3).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());  // queued
        items.add(new ChunkItemBuilder().setId(4).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());  // in process
        items.add(new ChunkItemBuilder().setId(5).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());  // failed with diagnostic
        items.add(new ChunkItemBuilder().setId(6).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());  // OK single
        items.add(new ChunkItemBuilder().setId(7).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());  // OK single
        items.add(new ChunkItemBuilder().setId(8).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());
        final Chunk placeholderChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(items).build();

        TaskSpecificUpdateEntity taskPackage = new TPCreator(ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1a")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1b")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1c")
                .addAddiRecordWithQueued(ADDI_OK)
                .addAddiRecordWithInprocess(ADDI_OK)
                .addAddiRecordWithFailed(ADDI_OK, "failed")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:6")
                .addAddiRecordWithFailed(ADDI_OK, "delete nonexisting record")
                .addAddiRecordWithFailed(ADDI_OK, "reference in 014 00 a to 26907268 unknown")
                .createTaskpackageEntity();

        final Chunk chunk = ESTaskPackageUtil.getChunkForTaskPackage( taskPackage, placeholderChunk);
        final Iterator<ChunkItem> iterator = chunk.iterator();
        ChunkItem next = iterator.next();
        assertThat("ChunkItem0.getStatus()", next.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("ChunkItem0.getDiagnostics", next.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem0.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem1.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1a"));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1b"));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1c"));
        assertThat("ChunkItem1.getDiagnostics", next.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem1.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem2.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem2.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem3.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem3 content", StringUtil.asString(next.getData()), containsString("queued"));
        assertThat("ChunkItem3.getDiagnostics", next.getDiagnostics().size(), is(1));
        assertThat("ChunkItem3.getDiagnostics.stacktrace", next.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem3.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem4.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem4 content", StringUtil.asString(next.getData()), containsString("in process"));
        assertThat("ChunkItem4.getDiagnostics", next.getDiagnostics().size(), is(1));
        assertThat("ChunkItem4.getDiagnostics.stacktrace", next.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem4.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem5.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem5 content", StringUtil.asString(next.getData()), containsString("failed"));
        assertThat("ChunkItem5.getDiagnostics", next.getDiagnostics().size(), is(1));
        assertThat("ChunkItem5.getDiagnostics.stacktrace", next.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem5.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem6.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem6 content", StringUtil.asString(next.getData()), containsString("pid:6"));
        assertThat("ChunkItem6.getDiagnostics", next.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem6.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem7.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem7 content", StringUtil.asString(next.getData()), containsString("delete nonexisting record"));
        assertThat("ChunkItem7.getDiagnostics", next.getDiagnostics().get(0), is(new Diagnostic(Diagnostic.Level.WARNING, "delete nonexisting record")));
        assertThat("ChunkItem7.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem8.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem8 content", StringUtil.asString(next.getData()), containsString("reference in 014 00 a to 26907268 unknown"));
        assertThat("ChunkItem8.getDiagnostics", next.getDiagnostics().get(0), is(new Diagnostic(Diagnostic.Level.WARNING, "reference in 014 00 a to 26907268 unknown")));
        assertThat("ChunkItem8.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
    }

    @Test
    public void getChunkForTaskPackage_expectedItemContentIsMissingInEs_failsItem() throws SQLException, ClassNotFoundException, IOException {
        final List<ChunkItem> items = new ArrayList<>(2);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("3")).setTrackingId(TRACKING_ID).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).setTrackingId(TRACKING_ID).build());
        final Chunk placeholderChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(items).build();

        TaskSpecificUpdateEntity taskPackage = new TPCreator(ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, "pid:0a")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:0b")
                .createTaskpackageEntity();

        final Chunk chunk = ESTaskPackageUtil.getChunkForTaskPackage( taskPackage, placeholderChunk);
        final Iterator<ChunkItem> iterator = chunk.iterator();
        ChunkItem next = iterator.next();
        assertThat("ChunkItem0.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem0.getDiagnostics", next.getDiagnostics().size(), is(1));
        assertThat("ChunkItem0.getDiagnostics.stacktrace", next.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("ChunkItem0.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
        next = iterator.next();
        assertThat("ChunkItem1.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem1.getDiagnostics", next.getDiagnostics().size(), is(1));
        assertThat("ChunkItem1.getDiagnostics.stacktrace", next.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("ChunkItem1.TRACKING_ID", next.getTrackingId(), is(TRACKING_ID));
    }

    private static class TPCreator {

        private TaskSpecificUpdateEntity taskPackage=new TaskSpecificUpdateEntity();
        private List<SuppliedRecordsEntity> records=new ArrayList<>();
        private List<TaskPackageRecordStructureEntity> taskPackageRecordStructures = new ArrayList<>();


        public TPCreator(String dbname) {
            taskPackage.setDatabasename(dbname);
            taskPackage.setTargetreference( 1 );
        }

        public TPCreator addAddiRecordWithSuccess(String addi, String record_id) {
            if (addi == null || record_id == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithSuccess can not be null!");
            }

            int lbnr=records.size();
            createRecordStructure(lbnr, record_id, TaskPackageRecordStructureEntity.RecordStatus.SUCCESS);

            createSuppliedRecord(lbnr, addi);

            return this;
        }


        private TPCreator addAddiRecordWithQueued(String addi) {
            if (addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithQueued can not be null!");
            }

            int lbnr=records.size();
            createRecordStructure(lbnr, "", TaskPackageRecordStructureEntity.RecordStatus.QUEUED);
            createSuppliedRecord(lbnr, addi);

            return this;
        }

        private TPCreator addAddiRecordWithInprocess(String addi) {
            if (addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithInprocess can not be null!");
            }
            int lbnr=records.size();
            createRecordStructure(lbnr, "", TaskPackageRecordStructureEntity.RecordStatus.IN_PROCESS);
            createSuppliedRecord(lbnr, addi);

            return this;
        }

        private TPCreator addAddiRecordWithFailed(String addi, String message) {
            if (addi == null || message == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithFailed can not be null!");
            }
            int lbnr=records.size();
            createRecordStructure_withDiag(lbnr, "", TaskPackageRecordStructureEntity.RecordStatus.FAILURE, message);
            createSuppliedRecord(lbnr, addi);

            // missing Set failoure diagnostics

            return this;
        }

        public TaskSpecificUpdateEntity createTaskpackageEntity() throws IllegalStateException, NumberFormatException, IOException, SQLException {
            taskPackage.setSuppliedRecords(records);
            taskPackage.setTaskpackageRecordStructures(taskPackageRecordStructures);

            return taskPackage;
        }

        private void createSuppliedRecord(int lbnr, String addi) {
            SuppliedRecordsEntity suppliedRecord=new SuppliedRecordsEntity();
            suppliedRecord.lbnr=lbnr;
            suppliedRecord.metaData = addi;
            suppliedRecord.record = "Missing".getBytes();
            records.add( suppliedRecord );
        }

        private void createRecordStructure(int lbnr, String record_id, TaskPackageRecordStructureEntity.RecordStatus recordStatus ) {
            TaskPackageRecordStructureEntity recordStructure=new TaskPackageRecordStructureEntity();
            recordStructure.lbnr = lbnr;
            recordStructure.recordStatus= recordStatus;
            recordStructure.record_id = record_id;
            taskPackageRecordStructures.add(recordStructure);
        }

        private void createRecordStructure_withDiag(int lbnr, String record_id, TaskPackageRecordStructureEntity.RecordStatus recordStatus, String message ) {
            TaskPackageRecordStructureEntity recordStructure=new TaskPackageRecordStructureEntity();
            recordStructure.lbnr = lbnr;
            recordStructure.recordStatus= recordStatus;
            recordStructure.record_id = record_id;
            recordStructure.diagnosticId = 1;
            List<DiagnosticsEntity> diags = new ArrayList<>();
            diags.add( new DiagnosticsEntity(0, message));


            recordStructure.setDiagnosticsEntities( diags );
            taskPackageRecordStructures.add( recordStructure);
        }
    }

}
