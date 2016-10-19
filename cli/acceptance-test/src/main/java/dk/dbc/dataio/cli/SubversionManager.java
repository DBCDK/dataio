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

package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.javascript.JavaScriptSubversionProject;
import dk.dbc.dataio.commons.types.FlowComponentContent;


/**
 * Class managing all interactions with subversion needed for acceptance test operation
 */
public class SubversionManager {

    private final JavaScriptSubversionProject subversionProject;

    public SubversionManager(String scmEndpoint) {
        subversionProject = new JavaScriptSubversionProject(scmEndpoint);
    }

    public JavaScriptProject getJavaScriptProject(Long revision, FlowComponentContent current) throws JavaScriptProjectException {
        return subversionProject.fetchRequiredJavaScript(
                current.getSvnProjectForInvocationJavascript(),
                revision,
                current.getInvocationJavascriptName(),
                current.getInvocationMethod());
    }
}
