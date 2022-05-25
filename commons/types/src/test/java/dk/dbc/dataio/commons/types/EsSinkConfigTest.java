package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class EsSinkConfigTest {

    private static final Integer USER_ID = 0;
    private static final String DATABASE_NAME = "databaseName";

    @Test
    public void constructor_userIdArgIsNull_throws() {
        assertThat(() -> new EsSinkConfig().withUserId(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_databaseArgIsNull_throws() {
        assertThat(() -> new EsSinkConfig().withDatabaseName(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithDefaultValuesSet() {
        final EsSinkConfig esSinkConfig = new EsSinkConfig().withUserId(USER_ID).withDatabaseName(DATABASE_NAME);
        assertThat(esSinkConfig.getUserId(), is(USER_ID));
        assertThat(esSinkConfig.getDatabaseName(), is(DATABASE_NAME));
        assertThat(esSinkConfig.getEsAction(), is("INSERT"));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final EsSinkConfig esSinkConfig = new EsSinkConfig();
        final EsSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(esSinkConfig), EsSinkConfig.class);
        assertThat(unmarshalled, is(esSinkConfig));
    }
}
