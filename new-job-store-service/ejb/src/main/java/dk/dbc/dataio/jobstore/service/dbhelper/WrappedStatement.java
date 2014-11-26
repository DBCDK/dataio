package dk.dbc.dataio.jobstore.service.dbhelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * This abstract class constitutes a wrapper for a java.sql.PreparedStatement
 * enabling use of named parameters in specialised sub classes.
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

    /* Creates PreparedStatement using sqlTemplate and sets parameter values.
     */
    void createStatement() throws SQLException {
        close();
        statement = connection.prepareStatement(sqlTemplate);
        for (BindVariable bindVariable : variables) {
            statement.setObject(bindVariable.getParameterIndex(), bindVariable.getValue());
        }
    }

    public void close() throws SQLException {
        if (statement != null) {
            statement.close();
        }
    }

    /**
     * BindVariable abstraction.
     * <p>
     * Be advised that only the name field is used in hashcode() and
     * equals() methods.
     * </p>
     */
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

            if (!name.equals(that.name)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
