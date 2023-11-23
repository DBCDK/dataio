package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HarvestSelectorRequestTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void marshalling_unmarshalling() throws JSONBException {
        final HarvestSelectorRequest request = new HarvestSelectorRequest("dataset = 42");
        final String marshalled = jsonbContext.marshall(request);
        final HarvestSelectorRequest unmarshalled = jsonbContext.unmarshall(marshalled, HarvestSelectorRequest.class);
        assertThat(unmarshalled.getSelector(), is(new HarvestTaskSelector("dataset", "42")));
    }
}
