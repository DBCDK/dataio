package dk.dbc.dataio.harvester.rawrepo;

import org.junit.Test;

import java.sql.SQLException;

public class RawRepoConnectorBeanTest {
    @Test(expected = NullPointerException.class)
    public void fetchRecord_idArgIsNull_throws() throws SQLException {
        new RawRepoConnectorBean().fetchRecord(null);
    }

    @Test(expected = NullPointerException.class)
    public void dequeue_consumerIdArgIsNull_throws() throws SQLException {
        new RawRepoConnectorBean().dequeue(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dequeue_consumerIdArgIsEmpty_throws() throws SQLException {
        new RawRepoConnectorBean().dequeue("");
    }

    @Test(expected = NullPointerException.class)
    public void queueSuccess_queueJobsArgIsNull_throws() throws SQLException {
        new RawRepoConnectorBean().queueSuccess(null);
    }

    @Test(expected = NullPointerException.class)
    public void queueFail_queueJobsArgIsNull_throws() throws SQLException {
        new RawRepoConnectorBean().queueFail(null, "error");
    }
}