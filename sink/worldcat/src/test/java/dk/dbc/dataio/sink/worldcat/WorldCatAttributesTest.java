package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WorldCatAttributesTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void canBeMarshalledAndUnmarshalled() throws JSONBException {
        final WorldCatAttributes worldCatAttributes = new WorldCatAttributes()
                .withPid("testPid")
                .withOcn("testOcn")
                .withHoldings(Arrays.asList(
                        new Holding().withSymbol("ABCDE").withAction(Holding.Action.INSERT),
                        new Holding().withSymbol("FGHIJ").withAction(Holding.Action.DELETE)
                ));

        final WorldCatAttributes unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(worldCatAttributes), WorldCatAttributes.class);
        assertThat(unmarshalled, is(worldCatAttributes));
    }
}
