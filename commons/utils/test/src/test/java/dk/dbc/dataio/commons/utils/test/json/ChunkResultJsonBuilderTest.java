package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

public class ChunkResultJsonBuilderTest {
    @Test
    public void builderProducesValidChunkResultJson() throws JsonException {
        JsonUtil.fromJson(new ChunkResultJsonBuilder().build(), ChunkResult.class, MixIns.getMixIns());
    }
}
