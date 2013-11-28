package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.sink.SinkException;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    ESTaskPackageUtil.class,
    })
public class EsConnectorBeanTest {
    final EsConnectorBean esConnectorBean = mock(EsConnectorBean.class);
    final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();

    @Before
    public void setUp() {
        try {
            when(esConnectorBean.insertEsTaskPackage(any(EsWorkload.class))).thenCallRealMethod();
            esConnectorBean.configuration = configuration;
            mockStatic(ESTaskPackageUtil.class);
        } catch (SinkException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(expected = SinkException.class)
    public void insertEsTaskPackage_getConnectionThrowsNamingException_throws() throws SinkException, SQLException, NamingException {
        when(esConnectorBean.getConnection()).thenThrow(new NamingException());
        esConnectorBean.insertEsTaskPackage(null);
    }

    @Test(expected = SinkException.class)
    public void insertEsTaskPackage_getConnectionThrowsSQLException_throws() throws SinkException, SQLException, NamingException {
        when(esConnectorBean.getConnection()).thenThrow(new SQLException());
        esConnectorBean.insertEsTaskPackage(null);
    }

    @Test
    public void insertEsTaskPackage_taskPackageIsInserted_returnsTargetReference() throws SinkException, SQLException, NamingException {
        final int expectedTargetReference = 42;
        final Connection connection = mock(Connection.class);
        when(esConnectorBean.getConnection()).thenReturn(connection);
        when(ESTaskPackageUtil.insertTaskPackage(eq(connection), any(String.class), any(EsWorkload.class)))
                .thenReturn(expectedTargetReference);

        assertThat(esConnectorBean.insertEsTaskPackage(null), is(expectedTargetReference));
    }
}
