package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ESTaskPackageUtilIT {
    private static Logger LOGGER = LoggerFactory.getLogger(ESTaskPackageUtilIT.class);

    private static String ES_DATABASE_NAME;
    private static final String ADDI_OK = "1\na\n1\nb\n";
    private static final int USER_ID = 3; // ja7
    private static final ESUtil.PackageType PACKAGE_TYPE = ESUtil.PackageType.DATABASE_UPDATE;
    private static final ESUtil.Action ACTION = ESUtil.Action.INSERT;

    @BeforeClass
    public static void setUpClass() {
        ES_DATABASE_NAME = System.getProperty("es.dbname");
    }

    @Before
    public void createEsDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.getEsConnection()) {
            ESUtil.createDatabaseIfNotExisting(connection, ES_DATABASE_NAME);
        }
    }

    @After
    public void removeEsDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.getEsConnection()) {
            ESUtil.deleteTaskpackages(connection, ES_DATABASE_NAME);
            ESUtil.deleteDatabase(connection, ES_DATABASE_NAME);
        }
    }

    @Test
    public void getChunkForTaskPackage() throws SQLException, ClassNotFoundException, IOException {
        final List<ChunkItem> items = new ArrayList<>(7);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("3")).build());  // OK multiple
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(3).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // queued
        items.add(new ChunkItemBuilder().setId(4).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // in process
        items.add(new ChunkItemBuilder().setId(5).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // failed with diagnostic
        items.add(new ChunkItemBuilder().setId(6).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // OK single
        final ExternalChunk placeholderChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();

        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1a")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1b")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1c")
                .addAddiRecordWithQueued(ADDI_OK)
                .addAddiRecordWithInprocess(ADDI_OK)
                .addAddiRecordWithFailed(ADDI_OK, "failed")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:6")
                .createInsertAndSetStatus();

        final ExternalChunk chunk = ESTaskPackageUtil.getChunkForTaskPackage(ITUtil.getEsConnection(), targetReference, placeholderChunk);
        final Iterator<ChunkItem> iterator = chunk.iterator();
        ChunkItem next = iterator.next();
        assertThat("ChunkItem0.getStatus()", next.getStatus(), is(ChunkItem.Status.IGNORE));
        next = iterator.next();
        assertThat("ChunkItem1.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1a"));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1b"));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1c"));
        next = iterator.next();
        assertThat("ChunkItem2.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        next = iterator.next();
        assertThat("ChunkItem3.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem3 content", StringUtil.asString(next.getData()), containsString("queued"));
        next = iterator.next();
        assertThat("ChunkItem4.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem4 content", StringUtil.asString(next.getData()), containsString("in process"));
        next = iterator.next();
        assertThat("ChunkItem5.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem5 content", StringUtil.asString(next.getData()), containsString("failed"));
        next = iterator.next();
        assertThat("ChunkItem6.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem6 content", StringUtil.asString(next.getData()), containsString("pid:6"));
    }

    @Test
    public void getChunkForTaskPackage_expectedItemContentIsMissingInEs_failsItem() throws SQLException, ClassNotFoundException, IOException {
        final List<ChunkItem> items = new ArrayList<>(2);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("3")).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());
        final ExternalChunk placeholderChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();

        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, "pid:0a")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:0b")
                .createInsertAndSetStatus();

        final ExternalChunk chunk = ESTaskPackageUtil.getChunkForTaskPackage(ITUtil.getEsConnection(), targetReference, placeholderChunk);
        final Iterator<ChunkItem> iterator = chunk.iterator();
        ChunkItem next = iterator.next();
        assertThat("ChunkItem0.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        next = iterator.next();
        assertThat("ChunkItem1.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void findCompletionStatusForTaskpackages_MAX_WHERE_IN_SIZE_exeeded_allTaskPackagesFound()
            throws SQLException, ClassNotFoundException, IOException {
        ESTaskPackageUtil.MAX_WHERE_IN_SIZE = 6;

        final String pidFormat = "PID:%d";
        List<Integer> targetReferences = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            targetReferences.add(new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                    .addAddiRecordWithSuccess(ADDI_OK, String.format(pidFormat, i))
                    .createInsertAndSetStatus());
        }

        final List<ESTaskPackageUtil.TaskStatus> completionStatusForTaskpackages =
                ESTaskPackageUtil.findCompletionStatusForTaskpackages(ITUtil.getEsConnection(), targetReferences);

        assertThat(completionStatusForTaskpackages, is(notNullValue()));
        assertThat(completionStatusForTaskpackages.size(), is(targetReferences.size()));
        for (int i = 0; i < targetReferences.size(); i++) {
            assertThat(completionStatusForTaskpackages.get(i).getTargetReference(), is(targetReferences.get(i)));
            assertThat(completionStatusForTaskpackages.get(i).getTaskStatus(), is(ESTaskPackageUtil.TaskStatus.Code.PENDING));
        }
    }

    @Test
    public void deleteTaskpackages_MAX_WHERE_IN_SIZE_exeeded_allTaskPackagesDeleted()
            throws SQLException, ClassNotFoundException, IOException {
        ESTaskPackageUtil.MAX_WHERE_IN_SIZE = 6;

        final String pidFormat = "PID:%d";
        List<Integer> targetReferences = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            targetReferences.add(new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                    .addAddiRecordWithSuccess(ADDI_OK, String.format(pidFormat, i))
                    .createInsertAndSetStatus());
        }

        ESTaskPackageUtil.deleteTaskpackages(ITUtil.getEsConnection(), targetReferences);

        final List<ESTaskPackageUtil.TaskStatus> completionStatusForTaskpackages =
                ESTaskPackageUtil.findCompletionStatusForTaskpackages(ITUtil.getEsConnection(), targetReferences);

        assertThat(completionStatusForTaskpackages, is(notNullValue()));
        assertThat(completionStatusForTaskpackages.size(), is(0));
    }

    private static class TPCreator {

        private enum RecordStatus {
            SUCCESS, QUEUED, INPROCESS, FAILED
        }

        private final Connection conn;
        private final String dbname;

        private List<String> addis = new ArrayList<>();
        private List<RecordStatus> recordStatuses = new ArrayList<>();
        private List<String> messages = new ArrayList<>();

        public TPCreator(Connection conn, String dbname) {
            this.conn = conn;
            this.dbname = dbname;
        }

        public TPCreator addAddiRecordWithSuccess(String addi, String message) {
            if (addi == null || message == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithSuccess can not be null!");
            }
            addis.add(addi);
            recordStatuses.add(RecordStatus.SUCCESS);
            messages.add(message);
            return this;
        }

        private TPCreator addAddiRecordWithQueued(String addi) {
            if (addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithQueued can not be null!");
            }
            addis.add(addi);
            recordStatuses.add(RecordStatus.QUEUED);
            messages.add("");
            return this;
        }

        private TPCreator addAddiRecordWithInprocess(String addi) {
            if (addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithInprocess can not be null!");
            }
            addis.add(addi);
            recordStatuses.add(RecordStatus.INPROCESS);
            messages.add("");
            return this;
        }

        private TPCreator addAddiRecordWithFailed(String addi, String message) {
            if (addi == null || message == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithFailed can not be null!");
            }
            addis.add(addi);
            recordStatuses.add(RecordStatus.FAILED);
            messages.add(message);
            return this;
        }

        public int createInsertAndSetStatus() throws IllegalStateException, NumberFormatException, IOException, SQLException {
            int targetReference = createTPAndInsertAddis();
            for (int i = 0; i < recordStatuses.size(); i++) {
                setStatusAndMessage(targetReference, i, recordStatuses.get(i), messages.get(i));
            }
            return targetReference;
        }

        private int createTPAndInsertAddis() throws IllegalStateException, NumberFormatException, IOException, SQLException {
            List<ChunkItem> chunkItems = createChunkItemList();
            ExternalChunk processedChunk = createProcessedChunk(chunkItems);
            final ExternalChunk deliveredChunk = new ExternalChunk(
                    processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
            for(ChunkItem item : processedChunk) {
                deliveredChunk.insertItem(item);
            }
            deliveredChunk.setEncoding(processedChunk.getEncoding());
            EsWorkload esWorkload = new EsWorkload(deliveredChunk, ESTaskPackageUtil.getAddiRecordsFromChunk(processedChunk),
                    USER_ID, PACKAGE_TYPE, ACTION);
            return ESTaskPackageUtil.insertTaskPackage(conn, dbname, esWorkload);
        }

        private List<ChunkItem> createChunkItemList() {
            List<ChunkItem> chunkItems = new ArrayList<>(addis.size());
            int id = 0;
            for (String addi : addis) {
                // This is not the place to test state of incoming chunk items, therefore: assuming all incoming ChunkItems are successfull.
                ChunkItem ci = new ChunkItemBuilder().setId(id++).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes(addi)).build();
                chunkItems.add(ci);
            }
            return chunkItems;
        }

        private ExternalChunk createProcessedChunk(List<ChunkItem> chunkItems) {
            return new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                    .setItems(chunkItems)
                    .build();
        }

        private void setStatusAndMessage(int targetReference, int lbnr, RecordStatus status, String message) throws SQLException {
            switch (status) {
                case SUCCESS:
                    updateRecordStatus(targetReference, lbnr, 1);
                    updateRecordId(targetReference, lbnr, message);
                    break;
                case QUEUED:
                    updateRecordStatus(targetReference, lbnr, 2);
                    break;
                case INPROCESS:
                    updateRecordStatus(targetReference, lbnr, 3);
                    break;
                case FAILED:
                    updateRecordStatus(targetReference, lbnr, 4);
                    setFailureDiagnostic(targetReference, lbnr, message);
                    break;
            }
        }

        private void updateRecordStatus(int targetReference, int lbnr, int status) throws SQLException {
            final String selectStmt = "SELECT recordstatus FROM taskpackagerecordstructure WHERE targetreference = ? AND lbnr = ? FOR UPDATE OF recordstatus";
            final String updateStmt = "UPDATE taskpackagerecordstructure SET recordstatus = ? WHERE targetreference = ? AND lbnr = ?";
            JDBCUtil.update(conn, selectStmt, targetReference, lbnr);
            JDBCUtil.update(conn, updateStmt, status, targetReference, lbnr);
        }

        private void updateRecordId(int targetReference, int lbnr, String recordId) throws SQLException {
            final String selectStmt = "SELECT record_id FROM taskpackagerecordstructure WHERE targetreference = ? AND lbnr = ? FOR UPDATE OF record_id";
            final String updateStmt = "UPDATE taskpackagerecordstructure SET record_id = ? WHERE targetreference = ? AND lbnr = ?";
            JDBCUtil.update(conn, selectStmt, targetReference, lbnr);
            JDBCUtil.update(conn, updateStmt, recordId, targetReference, lbnr);
        }

        // This method is in many ways stolen from opensearch-commons/trunk/harvester/es/src/main/java/dk/dbc/opensearch/commons/harvester/es/ESHarvesterDAO.java
        protected void setFailureDiagnostic(int targetReference, int lbnr, String failureDiagnostic) throws SQLException {
            final String stmt1 = "select diagIdSeq.nextval from dual";
            final String stmt2 = "INSERT INTO diagnostics (id, lbnr, diagnosticSetId, condition, addInfo) VALUES ( ?, ?, ?, ?, ? )";
            final String stmt3 = "SELECT recordOrSurDiag2 FROM taskpackagerecordstructure WHERE targetreference = ? AND lbnr = ? FOR UPDATE OF recordOrSurDiag2";
            final String stmt4 = "UPDATE taskpackagerecordstructure SET recordOrSurDiag2 = ? WHERE targetreference = ? AND lbnr = ?";

            try {

                PreparedStatement ps = null;
                int diagnosticId = -1;
                try {
                    ps = JDBCUtil.query(conn, stmt1);
                    ResultSet rs = ps.getResultSet();
                    if (rs.next()) {
                        diagnosticId = rs.getInt(1);
                    } else {
                        // no diagnosticId retrieved - this is an error
                        String errMsg = "Could not retrieve a new diagnosticId from database";
                        LOGGER.error(errMsg);
                        throw new SQLException(errMsg);
                    }
                } finally {
                    JDBCUtil.closeStatement(ps);
                }

                // Setting diagnostic with some hardcoded values.
                JDBCUtil.update(conn, stmt2, diagnosticId, 1, "'10.100.1.1'", 100, failureDiagnostic);
                JDBCUtil.update(conn, stmt3, targetReference, lbnr);
                JDBCUtil.update(conn, stmt4, diagnosticId, targetReference, lbnr);
            } catch (SQLException e) {
                LOGGER.error("SQLException caught when trying to set failurediagnostic with the following values: targetRef: [{}] lbnr: [{}] failureDiagnostic: [{}]",
                        targetReference, lbnr, failureDiagnostic, e);
                throw e;
            }
        }
    }
}
