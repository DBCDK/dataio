package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TickleAttributesTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void unknownFieldsIgnoredDuringUnmarshalling() throws JSONBException {
        TickleAttributes tickleAttributes = jsonbContext.unmarshall(
                "{\"submitter\":42, \"deleted\": true, \"unknownKey\": \"value\"}", TickleAttributes.class);
        assertThat("agencyId", tickleAttributes.getAgencyId(), is(42));
        assertThat("is deleted", tickleAttributes.isDeleted(), is(true));
    }
}
