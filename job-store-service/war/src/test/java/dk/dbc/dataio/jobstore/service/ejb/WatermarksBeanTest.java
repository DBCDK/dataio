package dk.dbc.dataio.jobstore.service.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.jobstore.service.entity.WatermarkEntity;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WatermarksBeanTest {
    private static final int SINK_ID = 42;
    private static final String RECORD_KEY = "870970:12345678";

    private WatermarksBean watermarksBean;
    private JSONBContext jsonbContext;

    @BeforeEach
    public void setup() {
        jsonbContext = new JSONBContext();
        watermarksBean = new WatermarksBean();
        watermarksBean.jsonbContext = new JSONBContext();
        watermarksBean.entityManager = mock(EntityManager.class);
    }

    @Test
    public void getWatermark_rowExists_returnsStatusOkResponseWithWatermark() throws JSONBException {
        WatermarkEntity entity = new WatermarkEntity()
                .withKey(new WatermarkEntity.Key(SINK_ID, RECORD_KEY))
                .withJobId(1234567)
                .withChunkId(42)
                .withItemId((short) 3);
        when(watermarksBean.entityManager.find(eq(WatermarkEntity.class), any())).thenReturn(entity);

        Response response = watermarksBean.getWatermark(SINK_ID, RECORD_KEY);
        assertOkResponse(response);

        JsonNode watermark = getWatermarkNode(response);
        assertThat("watermark not null", watermark.isNull(), is(false));
        assertThat("jobId", watermark.get("jobId").asInt(), is(1234567));
        assertThat("chunkId", watermark.get("chunkId").asInt(), is(42));
        assertThat("itemId", watermark.get("itemId").asInt(), is(3));
    }

    @Test
    public void getWatermark_rowNotFound_returnsStatusOkResponseWithNullWatermark() throws JSONBException {
        when(watermarksBean.entityManager.find(eq(WatermarkEntity.class), any())).thenReturn(null);

        Response response = watermarksBean.getWatermark(SINK_ID, RECORD_KEY);
        assertOkResponse(response);

        JsonNode watermark = getWatermarkNode(response);
        assertThat("watermark is null", watermark.isNull(), is(true));
    }

    /*
     * Private methods
     */

    private void assertOkResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
    }

    private JsonNode getWatermarkNode(Response response) throws JSONBException {
        try {
            JsonNode root = jsonbContext.getObjectMapper().readTree((String) response.getEntity());
            return root.get("watermark");
        } catch (Exception e) {
            throw new JSONBException(e);
        }
    }
}
