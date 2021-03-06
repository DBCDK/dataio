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

public class JavaScriptJsonBuilder extends JsonBuilder {
    private String javascript = "javascript";
    private String moduleName = "moduleName";

    public JavaScriptJsonBuilder setJavascript(String javascript) {
        this.javascript = javascript;
        return this;
    }

    public JavaScriptJsonBuilder setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("javascript", javascript)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("moduleName", moduleName));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
