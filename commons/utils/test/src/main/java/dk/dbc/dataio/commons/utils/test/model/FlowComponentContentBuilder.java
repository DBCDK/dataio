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

package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowComponentContentBuilder {
    private String name = "name";
    private String description = "description";
    private String svnProjectForInvocationJavascript = "svnprojectforinvocationjavascript";
    private long svnRevision = 1L;
    private String invocationJavascriptName = "invocationJavascriptName";
    private List<JavaScript> javascripts = new ArrayList<>(Arrays.asList(
            new JavaScriptBuilder().build()));
    private String invocationMethod = "invocationMethod";
    private String requireCache = null;

    public FlowComponentContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowComponentContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowComponentContentBuilder setSvnProjectForInvocationJavascript(String project) {
        this.svnProjectForInvocationJavascript = project;
        return this;
    }

    public FlowComponentContentBuilder setSvnRevision(long revision) {
        this.svnRevision = revision;
        return this;
    }

    public FlowComponentContentBuilder setInvocationJavascriptName(String invocationJavascriptName) {
        this.invocationJavascriptName = invocationJavascriptName;
        return this;
    }

    public FlowComponentContentBuilder setJavascripts(List<JavaScript> javascripts) {
        this.javascripts = new ArrayList<>(javascripts);
        return this;
    }

    public FlowComponentContentBuilder setInvocationMethod(String invocationMethod) {
        this.invocationMethod = invocationMethod;
        return this;
    }

    public FlowComponentContentBuilder setRequireCache( String requireCache) {
        this.requireCache = requireCache;
        return this;
    }

    public FlowComponentContent build() {
        return new FlowComponentContent(name, svnProjectForInvocationJavascript, svnRevision, invocationJavascriptName, javascripts, invocationMethod, description, requireCache);
    }
}
