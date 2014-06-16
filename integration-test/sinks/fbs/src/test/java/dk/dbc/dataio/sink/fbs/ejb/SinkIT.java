package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SinkIT {
    private final ChunkResult chunkResult = getChunkResult();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @AfterClass
    public static void teardown() {
        InMemoryInitialContextFactory.clear();
    }

    /**
     * Given: a ChunkResult with two items, the first one valid and the second one invalid <br/>
     * When: pushing ChunkResult to FBS web-service endpoint <br/>
     * Then: a SinkChunkResult with two items is returned <br/>
     * And: the first item has status SUCCESS <br/>
     * And: the second item has status FAILURE <br/>
     */
    @Ignore
    @Test
    public void fbsPusherBean_endpointResponds() throws NamingException {
        // When...
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_FBS_WS, System.getProperty("fbs.ws.endpoint"));
        final FbsPusherBean fbsPusherBean = getFbsPusherBean();
        final SinkChunkResult sinkChunkResult = fbsPusherBean.push(chunkResult);

        // Then...
        assertThat(sinkChunkResult.getItems().size(), is(2));
        // And...
        assertThat(sinkChunkResult.getItems().get(0).getStatus(), is(ChunkItem.Status.SUCCESS));
        // And...
        assertThat(sinkChunkResult.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
    }

    private FbsPusherBean getFbsPusherBean() throws NamingException {
        final FbsPusherBean fbsPusherBean = new FbsPusherBean();
        fbsPusherBean.fbsUpdateConnector = getFbsUpdateConnectorBean();
        return fbsPusherBean;
    }

    private FbsUpdateConnectorBean getFbsUpdateConnectorBean() throws NamingException {
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.initializeConnector();
        return fbsUpdateConnectorBean;
    }

    private ChunkResult getChunkResult() {
        final ChunkResult chunkResult = new ChunkResultBuilder().setItems(Collections.<ChunkItem>emptyList()).build();
        chunkResult.addItem(new ChunkItemBuilder().setId(0).setData(Base64Util.base64encode(
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                  "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                      "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                    "</marcx:datafield>" +
                  "</marcx:record>" +
                "</marcx:collection>"
        )).build());
        chunkResult.addItem(new ChunkItemBuilder().setId(1).setData(Base64Util.base64encode(
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                  "<marcx:record format=\"danMARC2\">" +
                  "</marcx:record>" +
                "</marcx:collection>"
        )).build());
        return chunkResult;
    }
}
