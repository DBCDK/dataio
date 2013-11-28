package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    JDBCUtil.class,
})
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
        assertThat(taskStatus.getTaskStatus(), is(ESTaskPackageUtil.TaskStatusCode.COMPLETE));
    }
}
