package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.jobprocessor.util.Base64Util;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertThat;

public class ESTaskPackageUtilIT {

    private static String ES_DATABASE_NAME;
    private static final String ADDI_OK = "1\na\n1\nb\n";

    @BeforeClass
    public static void setUpClass() {
        ES_DATABASE_NAME = System.getProperty("es.dbname");
    }

    @Before
    public void createEsDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = getEsConnection()) {
            ESUtil.createDatabaseIfNotExisting(connection, ES_DATABASE_NAME);
        }
    }

    @After
    public void removeEsDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = getEsConnection()) {
            ESUtil.deleteTaskpackages(connection, ES_DATABASE_NAME);
            ESUtil.deleteDatabase(connection, ES_DATABASE_NAME);
        }
    }

    @Test(expected=NullPointerException.class)
    public void testGetSinkResultItemsForTaskPackage_nullParameterConnection_throws() throws SQLException {
        ESTaskPackageUtil.getSinkResultItemsForTaskPackage(null, 1);
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SingleAddiWithSuccess_isSuccessAndHasPid() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        String pid = "PID:1";
        int targetReference = new TPCreator(getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, pid)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(getEsConnection(), targetReference);
        assertThat(items.size(), is(1));

        ChunkItem ci = items.get(0);
        assertThat(ci.getId(), is(0L));
        assertThat(ci.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(ci.getData(), is(pid));
    }

    @Test
    public void testGetSinkResultItemsForTaskPackage_SingleAddiWithQueued_isFailed() throws IllegalStateException, NumberFormatException, IOException, ClassNotFoundException, SQLException {
        int targetReference = new TPCreator(getEsConnection(), ES_DATABASE_NAME)
                .addAddiRecordWithQueued(ADDI_OK)
                .createInsertAndSetStatus();

        List<ChunkItem> items = ESTaskPackageUtil.getSinkResultItemsForTaskPackage(getEsConnection(), targetReference);
        assertThat(items.size(), is(1));

        ChunkItem ci = items.get(0);
        assertThat(ci.getId(), is(0L));
        assertThat(ci.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    private static class TPCreator {

        enum RecordStatus {SUCCESS, QUEUED, INPROCESS, FAILED};

        final Connection conn;
        final String dbname;

        List<String> addis = new ArrayList<>();
        List<RecordStatus> recordStatuses = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        public TPCreator(Connection conn, String dbname) {
            this.conn = conn;
            this.dbname = dbname;
        }

        public TPCreator addAddiRecordWithSuccess(String addi, String message) {
            if(addi == null || message == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithSuccess can not be null!");
            }
            addis.add(addi);
            recordStatuses.add(RecordStatus.SUCCESS);
            messages.add(message);

            return this;
        }

        private TPCreator addAddiRecordWithQueued(String addi) {
            if(addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithQueued can not be null!");
            }
            addis.add(addi);
            recordStatuses.add(RecordStatus.QUEUED);
            messages.add("");
            return this;
        }

        public int createInsertAndSetStatus() throws IllegalStateException, NumberFormatException, IOException, SQLException {
            int targetReference = createTPAndInsertAddis();
            for(int i=0; i<recordStatuses.size(); i++) {
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
            for(String addi : addis) {
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
            switch(status) {
                case SUCCESS:
                    updateRecordStatus(targetReference, lbnr, 1);
                    updateRecordId(targetReference, lbnr, message);
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
    }

    private void setTPRecordToSuccess(Connection conn, int targetReference, int lbnr, String pid) throws SQLException {
        setTPRecordStatus(conn, targetReference, lbnr, 1, pid);
    }

    private void setTPRecordToQueued(Connection conn, int targetReference, int lbnr) throws SQLException {
        setTPRecordStatus(conn, targetReference, lbnr, 2, "");
    }

    private void setTPRecordToInProcess(Connection conn, int targetReference, int lbnr) throws SQLException {
        setTPRecordStatus(conn, targetReference, lbnr, 3, "");
    }

    private void setTPRecordToFailed(Connection conn, int targetReference, int lbnr, String failMsg) throws SQLException {
        setTPRecordStatus(conn, targetReference, lbnr, 4, failMsg);
    }

    private Connection getEsConnection() throws ClassNotFoundException, SQLException {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        return DriverManager.getConnection(
                "jdbc:oracle:thin:@tora1.dbc.dk:1521/tora1.dbc.dk", "jbn", "jbn");
    }

    private void setTPRecordStatus(Connection conn, int targetReference, int lbnr, int status, String statusMsg) throws SQLException {

        updateRecordStatus(conn, targetReference, lbnr, status);

        /*
        // if neseccary
        updateRecordId(conn, targetReference, lbnr, statusMsg);
        // if necessary
        createAndSetDiagnostic(conn, targetReference, lbnr, statusMsg); // this one should create the diagnostic
        */
    }

    private void updateRecordStatus(Connection conn, int targetReference, int lbnr, int status) throws SQLException {
        final String selectStmt = "SELECT recordstatus FROM taskpackagerecordstructure WHERE targetreference = ? AND lbnr = ? FOR UPDATE OF recordstatus";
        final String updateStmt = "UPDATE taskpackagerecordstructure SET recordstatus = ? WHERE targetreference = ? AND lbnr = ?";

        JDBCUtil.update(conn, selectStmt, targetReference, lbnr);
        JDBCUtil.update(conn, updateStmt, status, targetReference, lbnr);
    }
}
