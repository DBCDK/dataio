package dk.dbc.dataio.jobstore.service.dbhelper;

import dk.dbc.dataio.jobstore.service.digest.Md5;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AddEntityStatementTest {
    private final Connection connection = mock(Connection.class);

    @Test
    public void setEntity_entityArgCanNotBeMarshalled_throws() throws JSONBException, SQLException {
        final AddEntityStatement addEntityStatement = newAddEntityStatement();
        try {
            addEntityStatement.setEntity(new Object());
        } catch (JSONBException e) {
        }
    }

    @Test
    public void setEntity_entityArgCanBeMarshalled_setsEntityAndChecksumBindVariables()
            throws JSONBException, SQLException {
        final AddEntityStatement addEntityStatement = newAddEntityStatement();
        final JSONBContext jsonbContext = new JSONBContext();
        final SimpleBean entity = new SimpleBean();
        entity.setValue("someValue");

        // expectations
        final String expectedEntity = jsonbContext.marshall(entity);
        final String expectedChecksum = Md5.asHex(expectedEntity.getBytes(StandardCharsets.UTF_8));
        final HashMap<String, WrappedStatement.BindVariable> expectedBindVariables = new HashMap<>(2);
        expectedBindVariables.put("checksum",
                new WrappedStatement.BindVariable("checksum", expectedChecksum, 1));
        expectedBindVariables.put("entity",
                new WrappedStatement.BindVariable("entity", addEntityStatement.asJsonPgObject(expectedEntity), 2));

        addEntityStatement.setEntity(entity);

        assertThat(addEntityStatement.variables.size(), is(2));
        for (WrappedStatement.BindVariable variable : addEntityStatement.variables) {
            final WrappedStatement.BindVariable expectedVariable = expectedBindVariables.remove(variable.getName());
            assertThat(expectedVariable, is(notNullValue()));
            assertThat("parameterIndex for " + variable.getName(),
                    variable.getParameterIndex(), is(expectedVariable.getParameterIndex()));
            assertThat("value for " + variable.getName(),
                    variable.getValue(), is(expectedVariable.getValue()));
        }
        assertThat(expectedBindVariables.size(), is(0));
    }

    private AddEntityStatement newAddEntityStatement() throws JSONBException, SQLException {
        return new AddEntityStatement(connection, new JSONBContext());
    }

    private static class SimpleBean {
        String value;
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
    }
}