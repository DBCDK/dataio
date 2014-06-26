package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ESTaskPackageUtilIT {

    private static Logger LOGGER = LoggerFactory.getLogger(ESTaskPackageUtilIT.class);

    private static String ES_DATABASE_NAME;
    private static final String ADDI_OK = "1\na\n1\nb\n";

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

    @Test(expected = NullPointerException.class)
    public void testGetSinkResultItemsForTaskPackage_nullParameterConnection_throws() throws SQLException {
        ESTaskPackageUtil.getSinkResultItemsForTaskPackage(null, 1);
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SingleAddiWithSuccess_isSuccessAndHasPid() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        String pid = "PID:1";
        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, pid)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(ITUtil.getEsConnection(), targetReference);
        assertThat(items.size(), is(1));

        ChunkItem ci = items.get(0);
        assertThat(ci.getId(), is(0L));
        assertThat(ci.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(ci.getData(), is(pid));
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SingleAddiWithQueued_isFailed() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithQueued(ADDI_OK)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(ITUtil.getEsConnection(), targetReference);
        assertThat(items.size(), is(1));

        ChunkItem ci = items.get(0);
        assertThat(ci.getId(), is(0L));
        assertThat(ci.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(ci.getData().toLowerCase().contains("queued"), is(true)); // a little hacky - but the failure message should contain some info about what happend.
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SingleAddiWithInProcess_isFailed() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithInprocess(ADDI_OK)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(ITUtil.getEsConnection(), targetReference);
        assertThat(items.size(), is(1));

        ChunkItem ci = items.get(0);
        assertThat(ci.getId(), is(0L));
        assertThat(ci.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(ci.getData().toLowerCase().contains("inprocess"), is(true)); // a little hacky - but the failure message should contain some info about what happend.
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SingleAddiWithFailure_isFailedAndHasFailureMessage() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        String failureMessage = "Some Error Occured On The Other Side Of ES!";
        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithFailed(ADDI_OK, failureMessage)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(ITUtil.getEsConnection(), targetReference);
        assertThat(items.size(), is(1));

        ChunkItem ci = items.get(0);
        assertThat(ci.getId(), is(0L));
        assertThat(ci.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(ci.getData(), is(failureMessage));
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SeverealAddiWithFailureAndSuccess() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        String failureMessage0 = "Error 0";
        String failureMessage1 = "Error 1";
        String failureMessage2 = "Error 2";
        String pid0 = "PID:0";
        String pid1 = "PID:1";
        String pid2 = "PID:2";
        String pid3 = "PID:3";
        int targetReference = new TPCreator(ITUtil.getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, pid0)
                .addAddiRecordWithFailed(ADDI_OK, failureMessage0)
                .addAddiRecordWithSuccess(ADDI_OK, pid1)
                .addAddiRecordWithFailed(ADDI_OK, failureMessage1)
                .addAddiRecordWithSuccess(ADDI_OK, pid2)
                .addAddiRecordWithFailed(ADDI_OK, failureMessage2)
                .addAddiRecordWithSuccess(ADDI_OK, pid3)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(ITUtil.getEsConnection(), targetReference);
        assertThat(items.size(), is(7));

        ChunkItem ci0 = items.get(0);
        assertThat(ci0.getId(), is(0L));
        assertThat(ci0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(ci0.getData(), is(pid0));
        ChunkItem ci1 = items.get(1);
        assertThat(ci1.getId(), is(1L));
        assertThat(ci1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(ci1.getData(), is(failureMessage0));
        ChunkItem ci2 = items.get(2);
        assertThat(ci2.getId(), is(2L));
        assertThat(ci2.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(ci2.getData(), is(pid1));
        ChunkItem ci3 = items.get(3);
        assertThat(ci3.getId(), is(3L));
        assertThat(ci3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(ci3.getData(), is(failureMessage1));
        ChunkItem ci4 = items.get(4);
        assertThat(ci4.getId(), is(4L));
        assertThat(ci4.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(ci4.getData(), is(pid2));
        ChunkItem ci5 = items.get(5);
        assertThat(ci5.getId(), is(5L));
        assertThat(ci5.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(ci5.getData(), is(failureMessage2));
        ChunkItem ci6 = items.get(6);
        assertThat(ci6.getId(), is(6L));
        assertThat(ci6.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(ci6.getData(), is(pid3));
    }

    private static class TPCreator {

        private enum RecordStatus {
            SUCCESS, QUEUED, INPROCESS, FAILED
        };

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
            ChunkResult chunkResult = createChunkResult(chunkItems);
            EsWorkload esWorkload = new EsWorkload(chunkResult, ESTaskPackageUtil.getAddiRecordsFromChunk(chunkResult));
            return ESTaskPackageUtil.insertTaskPackage(conn, dbname, esWorkload);
        }

        private List<ChunkItem> createChunkItemList() {
            List<ChunkItem> chunkItems = new ArrayList<>(addis.size());
            for (String addi : addis) {
                // This is not the place to test state of incoming chunk items, therefore: assuming all incoming ChunkItems are successfull.
                ChunkItem ci = new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.SUCCESS).setData(Base64Util.base64encode(addi)).build();
                chunkItems.add(ci);
            }
            return chunkItems;
        }

        private ChunkResult createChunkResult(List<ChunkItem> chunkItems) {
            return new ChunkResultBuilder()
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
                        new Object[]{targetReference, lbnr, failureDiagnostic, e});
                throw e;
            }
        }
    }
}
