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
package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;

public class OpenUpdateSinkConfigJsonBuilder extends JsonBuilder {
    private String userId = "defaultUserId";
    private String password = "defaultPassword";
    private String endpoint = "defaultEndpoint";

    public OpenUpdateSinkConfigJsonBuilder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public OpenUpdateSinkConfigJsonBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public OpenUpdateSinkConfigJsonBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("@class", OpenUpdateSinkConfig.class.getTypeName()));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("userId", userId));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("password", password));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("endpoint", endpoint));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }

}
