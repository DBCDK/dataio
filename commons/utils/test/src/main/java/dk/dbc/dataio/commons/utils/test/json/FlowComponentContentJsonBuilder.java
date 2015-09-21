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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String description = "description";
    private String svnProjectForInvocationJavascript = "svnprojectforinvocationjavascript";
    private long svnRevision = 1L;
    private String invocationJavascriptName = "invocationJavascriptName";
    private List<String> javascripts = new ArrayList<>(Arrays.asList(
            new JavaScriptJsonBuilder().build()));
    private String invocationMethod = "invocationMethod";

    public FlowComponentContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowComponentContentJsonBuilder setSvnProjectForInvocationJavascript(String project) {
        this.svnProjectForInvocationJavascript = project;
        return this;
    }

    public FlowComponentContentJsonBuilder setSvnRevision(long revision) {
        this.svnRevision = revision;
        return this;
    }

    public FlowComponentContentJsonBuilder setInvocationJavascriptName(String invocationJavascriptName) {
        this.invocationJavascriptName = invocationJavascriptName;
        return this;
    }

    public FlowComponentContentJsonBuilder setJavascripts(List<String> javascripts) {
        this.javascripts = new ArrayList<>(javascripts);
        return this;
    }

    public FlowComponentContentJsonBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

   public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("svnProjectForInvocationJavascript", svnProjectForInvocationJavascript)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("svnRevision", svnRevision)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("invocationJavascriptName", invocationJavascriptName)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("invocationMethod", invocationMethod)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectArray("javascripts", javascripts)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
