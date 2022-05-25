package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jdbc.util.JDBCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ESTaskPackageIntegrationTestUtil {

    // Method for setting all records in a tp to success, and also set the tp to success.
    public static void successfullyCompleteTaskpackage(Connection conn, int targetReference) throws SQLException {
        Deque<Integer> lbnrs = getIdentifiersFromTaskPackage(conn, targetReference);
        for (int lbnr : lbnrs) {
            setRecordStatusToSuccess(conn, targetReference, lbnr);
        }
        setTPUpdateStatusToSuccess(conn, targetReference);
        setTPTaskstatusToCompleteAndUpdateSubstatus(conn, targetReference);
    }

    /**
     * Find taskpackages based on a dbname.
     *
     * @param connection
     * @param dbname
     * @return
     * @throws SQLException
     */
    public static List<Integer> findTaskpackagesForDBName(Connection connection, String dbname) throws SQLException {
        final String stmt = "SELECT targetreference FROM taskspecificupdate WHERE databasename = ? order by targetreference";
        List<Integer> taskpackages = new ArrayList<>();

        PreparedStatement ps = null;
        try {
            ps = JDBCUtil.query(connection, stmt, dbname);
            ResultSet rs = ps.getResultSet();
            while (rs.next()) {
                taskpackages.add(rs.getInt(1));
            }
        } finally {
            JDBCUtil.closeStatement(ps);
        }
        return taskpackages;
    }

    /**
     * Retrieves all Identifiers (lbnr) from a taskpackage.
     *
     * @param connection
     * @param targetRef
     * @return A Deque of identifiers
     * @throws SQLException
     */
    public static Deque<Integer> getIdentifiersFromTaskPackage(Connection connection, int targetRef) throws SQLException {
        final String statement = "SELECT lbnr FROM taskpackagerecordstructure WHERE targetreference = ? ORDER BY lbnr";

        PreparedStatement ps = null;
        Deque<Integer> ids = new ArrayDeque<>();
        try {
            ps = JDBCUtil.query(connection, statement, targetRef);
            ResultSet rs = ps.getResultSet();
            while (rs.next()) {
                int lbnr = rs.getInt(1);
                ids.add(lbnr);
            }
        } finally {
            JDBCUtil.closeStatement(ps);
        }
        return ids;
    }

    /**
     * Sets the recordstatus for a record in a taskpackage to success.
     *
     * @param connection
     * @param targetRef
     * @param lbNr
     * @return
     * @throws SQLException
     */
    public static boolean setRecordStatusToSuccess(Connection connection, int targetRef, int lbNr) throws SQLException {
        final String stmt1 = "SELECT recordstatus FROM taskpackagerecordstructure WHERE targetreference = ? AND lbnr = ? FOR UPDATE OF recordstatus";
        final String stmt2 = "UPDATE taskpackagerecordstructure SET recordstatus = ? WHERE targetreference = ? AND lbnr = ?";
        final int RECORD_STATUS_SUCCESS = 1;

        PreparedStatement ps = null;
        boolean result = false;
        try {
            ps = JDBCUtil.query(connection, stmt1, targetRef, lbNr);
            if (ps.getResultSet().next()) {
                result = true;
                JDBCUtil.update(connection, stmt2, RECORD_STATUS_SUCCESS, targetRef, lbNr);
            }
        } finally {
            JDBCUtil.closeStatement(ps);
        }
        return result;
    }

    /**
     * Sets updatestatus for a taskpackage to success
     *
     * @param conn
     * @param targetRef
     * @throws SQLException
     */
    public static void setTPUpdateStatusToSuccess(Connection conn, int targetRef) throws SQLException {
        final String stmt1 = "SELECT updatestatus FROM taskspecificupdate WHERE targetreference = ? FOR UPDATE OF updatestatus";
        final String stmt2 = "UPDATE taskspecificupdate SET updatestatus = ? WHERE targetreference = ?";
        final int UPDATESTATUS_SUCCESS = 1;

        PreparedStatement ps = null;
        try {
            ps = JDBCUtil.query(conn, stmt1, targetRef);
            ResultSet rs = ps.getResultSet();
            if (rs.next()) {
                JDBCUtil.update(conn, stmt2, UPDATESTATUS_SUCCESS, targetRef);
            }
        } finally {
            JDBCUtil.closeStatement(ps);
        }
    }

    /**
     * Sets taskstatus for a taskpackage to complete, and increments substatus.
     * Also sets accessdate since the taskpackage probably never have been in
     * active.
     *
     * @param conn
     * @param targetRef
     * @throws SQLException
     */
    public static void setTPTaskstatusToCompleteAndUpdateSubstatus(Connection conn, int targetRef) throws SQLException {
        // We are setting the accessdate every time since we are not sure if it is allready done.
        String stmt1 = "SELECT taskstatus, substatus, accessdate FROM taskpackage WHERE targetreference = ? FOR UPDATE OF taskstatus, substatus, accessdate";
        String stmt2 = "UPDATE taskpackage SET taskstatus = ?, substatus = substatus+1, accessdate = sysdate WHERE targetreference = ?";
        final int TASKSTATUS_COMPLETE = 2;

        int res = JDBCUtil.update(conn, stmt1, targetRef);
        if (res != 1) {
            throw new IllegalArgumentException("Could not find targetreference: " + targetRef);
        }
        JDBCUtil.update(conn, stmt2, TASKSTATUS_COMPLETE, targetRef);
    }
}
