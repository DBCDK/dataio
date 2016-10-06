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

package dk.dbc.dataio.cli.command;

import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.javascript.JavaScriptSubversionProject;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

/**
 * Flow component command line interface 'create' sub command
 */
public class CreateCommand {
    private final CreateOptions options;
    private final JSONBContext jsonbContext = new JSONBContext();

    public CreateCommand(CreateOptions options) {
        this.options = options;
    }

    /**
     * Creates flow component content from SCM project pointed to by {@link CreateOptions}
     * and prints its JSON representation to the console
     * @throws JavaScriptProjectException on failure to extract javascript project
     * @throws JSONBException on failure to marshall to JSON representation
     */
    public void execute() throws JavaScriptProjectException, JSONBException {
        final JavaScriptSubversionProject subversionProject = new JavaScriptSubversionProject(options.scmEndpoint);
        final JavaScriptProject javaScriptProject = subversionProject.fetchRequiredJavaScript(
                options.projectPath, options.revision, options.javaScriptFile, options.javaScriptMethod);
        final FlowComponentContent flowComponentContent = new FlowComponentContent(
                options.flowComponentName,
                options.projectPath,
                options.revision,
                options.javaScriptFile,
                javaScriptProject.getJavaScripts(),
                options.javaScriptMethod,
                options.flowComponentDescription,
                javaScriptProject.getRequireCache());
        System.out.println(jsonbContext.marshall(flowComponentContent));
    }
}
