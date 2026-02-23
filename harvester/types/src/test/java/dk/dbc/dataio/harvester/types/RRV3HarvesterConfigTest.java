package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class RRV3HarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    void decodeWithoutSubmitterFilter() throws JSONBException {
        String json =  """
            {
                "id": 42,
                "version": 1,
                "type": "dk.dbc.dataio.harvester.types.RRV3HarvesterConfig",
                "content": {
                    "id": "FBS-test-dm3 - broend-sync",
                    "type": "TEST",
                    "format": "katalog",
                    "harvesterType": "STANDARD"
                }
            }
            """;

        final RRV3HarvesterConfig config = jsonbContext.unmarshall(json, RRV3HarvesterConfig.class);
        assertThat(config.getContent().getSubmitterFilter(), is(nullValue()));
    }

    @Test
    void decodeWithSubmitterFilter() throws JSONBException {
        String json =  """
            {
                "id": 42,
                "version": 1,
                "type": "dk.dbc.dataio.harvester.types.RRV3HarvesterConfig",
                "content": {
                    "id": "FBS-test-dm3 - broend-sync",
                    "type": "TEST",
                    "format": "katalog",
                    "harvesterType": "STANDARD",
                    "submitterFilter": {
                        "type": "SKIP_ALL_EXCEPT",
                        "submitterNumber": [870970]
                    }
                }
            }
            """;

        final RRV3HarvesterConfig config = jsonbContext.unmarshall(json, RRV3HarvesterConfig.class);
        assertThat(config.getContent().getSubmitterFilter(),
                is(new SubmitterFilter(SubmitterFilter.Type.SKIP_ALL_EXCEPT, List.of(870970))));
    }

    @Test
    void getHarvesterToken() {
        final RRV3HarvesterConfig config = new RRV3HarvesterConfig(42, 1, new RRV3HarvesterConfig.Content());
        assertThat(config.getHarvesterToken(), is("raw-repo:42:1"));
    }
}
