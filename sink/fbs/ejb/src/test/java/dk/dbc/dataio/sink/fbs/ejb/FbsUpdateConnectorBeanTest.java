package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.junit.Test;

import javax.ejb.EJBException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FbsUpdateConnectorBeanTest {

    private static final String collection = "collection";
    private static final String trackingId = "trackingId";

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.initializeConnector();
    }

    @Test(expected = FbsUpdateConnectorException.class)
    public void updateMarcExchange_throwsFbsUpdateConnectorException() throws Exception {

        final FbsUpdateConnector fbsUpdateConnector = mock(FbsUpdateConnector.class);
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.fbsUpdateConnector = fbsUpdateConnector;

        when(fbsUpdateConnector.updateMarcExchange(any(String.class), any(String.class)))
                .thenThrow(new FbsUpdateConnectorException("FbsUpdateConnectorException thrown"));
        fbsUpdateConnectorBean.updateMarcExchange(collection, trackingId);
    }

    @Test
    public void updateMarcExchange_returnsUpdateMarcXchangeResult() throws Exception {

        final FbsUpdateConnector fbsUpdateConnector = mock(FbsUpdateConnector.class);
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.fbsUpdateConnector = fbsUpdateConnector;

        UpdateMarcXchangeResult updateMarcXchangeResult = new UpdateMarcXchangeResult();

        when(fbsUpdateConnector.updateMarcExchange(any(String.class), any(String.class)))
                .thenReturn(updateMarcXchangeResult);

        UpdateMarcXchangeResult returnedupdateMarcXchangeResult =
                fbsUpdateConnectorBean.updateMarcExchange(collection, trackingId);

        assertThat(returnedupdateMarcXchangeResult, is(updateMarcXchangeResult));
    }
}