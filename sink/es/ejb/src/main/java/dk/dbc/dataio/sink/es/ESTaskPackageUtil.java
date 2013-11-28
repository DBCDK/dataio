package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class ESTaskPackageUtil {

    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ESTaskPackageUtil.class);

    public static List<TaskStatus> findCompletionStatusForTaskpackages(Connection conn, List<Integer> taskpackages) {
        LOGGER.entry();
        try {
            // Tested through integrationtests
            final String retrieveStatement = "select targetreference, taskstatus from taskpackage where targetreference in ("
                    + commaSeparatedQuestionMarks(taskpackages.size()) + ")";
            List<TaskStatus> taskStatusList = new ArrayList<>();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = JDBCUtil.query(conn, retrieveStatement, taskpackages.toArray());
                rs = ps.getResultSet();
                while (rs.next()) {
                    int taskpackage = rs.getInt(1);
                    int statusCode = rs.getInt(2);
                    taskStatusList.add(new TaskStatus(taskpackage, statusCode));
                }
            } catch (SQLException ex) {
                // todo:  Fill in the blank!
            } finally {
                JDBCUtil.closeResultSet(rs);
                JDBCUtil.closeStatement(ps);
            }
            return taskStatusList;
        } finally {
            LOGGER.exit();
        }
    }

    // Tested through integrationtests
    public static void deleteTaskpackages(Connection conn, List<Integer> targetReferences) throws SQLException {
        LOGGER.entry();
        try {
            if(! targetReferences.isEmpty()) {
                String deleteStatement = "delete from taskpackage where targetreference in (" +
			commaSeparatedQuestionMarks(targetReferences.size()) + ")";
                LOGGER.trace(deleteStatement);
	        LOGGER.info("targetRefs to delete: {}", Arrays.toString(targetReferences.toArray()));
                PreparedStatement ps = JDBCUtil.query(conn, deleteStatement, targetReferences.toArray());
                JDBCUtil.closeStatement(ps);
            }
        } finally {
            LOGGER.exit();
        }
    }

    public static class TaskStatus {

        private final TaskStatusCode taskStatus;
        private final int targetreference;

        public TaskStatus(int taskStatus, int targetreference) {
            this.taskStatus = TaskStatusCode.getStatusCode(taskStatus);
            this.targetreference = targetreference;
        }

        public TaskStatusCode getTaskStatus() {
            return taskStatus;
        }

        public int getTargetReference() {
            return targetreference;
        }
    }

    public static enum TaskStatusCode {

        PENDING(0), ACTIVE(1), COMPLETE(2), ABORTED(3);

        private final int status;

        private TaskStatusCode(int status) {
            this.status = status;
        }

        public static TaskStatusCode getStatusCode(int i) {
            switch (i) {
                case 0:
                    return PENDING;
                case 1:
                    return ACTIVE;
                case 2:
                    return COMPLETE;
                case 3:
                    return ABORTED;
                default:
                    throw new IllegalArgumentException(i + " is not a valid code for TaskStatusCode.");
            }
        }
    }

    private static String commaSeparatedQuestionMarks(int size) {
        LOGGER.entry();
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append(i < size - 1 ? "?, " : "?");
            }
            return sb.toString();
        } finally {
            LOGGER.exit();
        }
    }
}
