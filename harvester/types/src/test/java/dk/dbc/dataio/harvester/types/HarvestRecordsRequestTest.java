package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HarvestRecordsRequestTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void marshalling_unmarshalling() throws JSONBException {
        HarvestRecordsRequest request =
                new HarvestRecordsRequest(new ArrayList<>())
                        .withBasedOnJob(42);

        String marshalled = jsonbContext.marshall(request);
        HarvestRecordsRequest unmarshalledToSub = jsonbContext.unmarshall(marshalled, HarvestRecordsRequest.class);
        HarvestRequest unmarshalledToSuper = jsonbContext.unmarshall(marshalled, HarvestRequest.class);
        assertThat(unmarshalledToSuper instanceof HarvestRecordsRequest, is(true));
    }
}
