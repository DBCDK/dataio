package dk.dbc.dataio.jobstore.service.dbhelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * This abstract class constitutes a specialised wrapper for a java.sql.PreparedStatement
 * making is possible to utilise named binding variables (on the form :name)
 * in SQL statements.
 */
public abstract class WrappedStatement {
    final Set<BindVariable> variables = new HashSet<>();
    Connection connection = null;
    PreparedStatement statement = null;
    String sqlTemplate = null;

    /* Binds given value to given name with a given parameter designation
     */
    WrappedStatement bind(String variableName, Object value, int parameterIndex) {
        variables.add(new BindVariable(variableName, value, parameterIndex));
        return this;
    }

    /* Creates PreparedStatement using sqlTemplate substituting
       all named parameters on the form :name with '?' and sets
       parameter values.
     */
    void createStatement() throws SQLException {
        close();
        String sql = sqlTemplate;
        for (BindVariable bindVariable : variables) {
            sql = sql.replaceAll(String.format(":%s", bindVariable.getName()), "?");
        }
        statement = connection.prepareStatement(sql);
        for (BindVariable bindVariable : variables) {
            statement.setObject(bindVariable.getParameterIndex(), bindVariable.getValue());
        }
    }

    public void close() throws SQLException {
        if (statement != null) {
            statement.close();
        }
    }

    public static class BindVariable {
        private final String name;
        private final int parameterIndex;
        private final Object value;

        public BindVariable(String name, Object value, int parameterIndex) {
            this.name = name;
            this.parameterIndex = parameterIndex;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BindVariable that = (BindVariable) o;

            if (parameterIndex != that.parameterIndex) {
                return false;
            }
            if (!name.equals(that.name)) {
                return false;
            }
            if (!value.equals(that.value)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + parameterIndex;
            result = 31 * result + value.hashCode();
            return result;
        }
    }
}
