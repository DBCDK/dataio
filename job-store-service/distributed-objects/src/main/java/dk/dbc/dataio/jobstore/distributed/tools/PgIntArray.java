 /*
 * dbc-commons - Common java modules
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of dbc-commons.
 *
 * dbc-commons is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-commons is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dbc-commons.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.distributed.tools;

 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Types;
 import java.util.Arrays;
 import java.util.Map;

/**
 * PostgreSQL int array representation
 * <p>
 * See also https://www.postgresql.org/docs/current/static/intarray.html
 * </p>
 */
public class PgIntArray implements java.sql.Array {
    private final Integer[] array;
    private final String stringValue;

    public PgIntArray(Integer[] array) {
        this.array = array;
        this.stringValue = toPgString(array);
    }

    @Override
    public String toString() {
        return stringValue;
    }

    @Override
    public String getBaseTypeName() throws SQLException {
        return "int4";
    }

    @Override
    public int getBaseType() throws SQLException {
        return Types.INTEGER;
    }

    @Override
    public Object getArray() throws SQLException {
        return array;
    }

    @Override
    public Object getArray(Map<String, Class<?>> map) throws SQLException {
        return array;
    }

    @Override
    public Object getArray(long index, int count) throws SQLException {
        return Arrays.copyOfRange(array, (int)index, (int)index + count);
    }

    @Override
    public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return getArray(index, count);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet getResultSet(long index, int count) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void free() throws SQLException {}

    public static String toPgString(Integer... array) {
        return toPgString(array, (Integer[]) null);
    }

    public static String toPgString(Integer[] array, Integer... additional) {
        if (array == null)
            return "{}";
        int iMax = array.length - 1;
        if (additional != null)
            iMax += additional.length;
        if (iMax == -1)
            return "{}";

        final StringBuilder b = new StringBuilder();
        b.append('{');
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                b.append(array[i]);
                if (i == iMax)
                    return b.append("}").toString();
                b.append(",");
            }
        }
        if (additional != null) {
            iMax = additional.length - 1;
            for (int i = 0; i < additional.length; i++) {
                if (additional[i] != null) {
                    b.append(additional[i]);
                    if (i == iMax)
                        return b.append("}").toString();
                    b.append(",");
                }
            }
        }
        return b.toString();
    }
}
