/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class EsSinkConfig implements SinkConfig {

    private int userId;
    private String databaseName;
    private String esAction;

    public EsSinkConfig() {
        this.esAction = "INSERT";
    }

    public int getUserId() {
        return userId;
    }

    public EsSinkConfig withUserId(int userId) throws IllegalArgumentException {
        this.userId = userId;
        return this;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public EsSinkConfig withDatabaseName(String databaseName) throws IllegalArgumentException {
        this.databaseName = InvariantUtil.checkNotNullNotEmptyOrThrow(databaseName, "databaseName");
        return this;
    }

    public String getEsAction() {
        return esAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EsSinkConfig)) return false;

        EsSinkConfig that = (EsSinkConfig) o;

        if (userId != that.userId) return false;
        if (databaseName != null ? !databaseName.equals(that.databaseName) : that.databaseName != null) return false;
        return esAction != null ? esAction.equals(that.esAction) : that.esAction == null;

    }

    @Override
    public int hashCode() {
        int result = userId;
        result = 31 * result + (databaseName != null ? databaseName.hashCode() : 0);
        result = 31 * result + (esAction != null ? esAction.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EsSinkConfig{" +
                "userId=" + userId +
                ", databaseName='" + databaseName + '\'' +
                ", esAction='" + esAction + '\'' +
                '}';
    }
}