package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EsWorkloadTest {
    private static final int USER_ID = 42;
    private static final ESUtil.PackageType PACKAGE_TYPE = ESUtil.PackageType.DATABASE_UPDATE;
    private static final ESUtil.Action ACTION = ESUtil.Action.INSERT;

    @Test(expected = NullPointerException.class)
    public void constructor_chunkResultArgIsNull_throws() {
        new EsWorkload(null, new ArrayList<AddiRecord>(0), USER_ID, PACKAGE_TYPE, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsArgIsNull_throws() {
        new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), null, USER_ID, PACKAGE_TYPE, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_packageTypeArgIsNull_throws() {
        new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, null, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_actionArgIsNull_throws() {
        new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, PACKAGE_TYPE, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final EsWorkload instance = new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, PACKAGE_TYPE, ACTION);
        assertThat(instance, is(notNullValue()));
    }
}
