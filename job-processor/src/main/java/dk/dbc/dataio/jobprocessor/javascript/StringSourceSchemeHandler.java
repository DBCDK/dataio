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

package dk.dbc.dataio.jobprocessor.javascript;

import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ISchemeHandler;
import dk.dbc.jslib.SchemeURI;

import java.util.Collections;
import java.util.List;

public class StringSourceSchemeHandler implements ISchemeHandler {
    public static final String SCHEME = "string";
    public final List<Script> scripts;

    public StringSourceSchemeHandler(List<Script> scripts) {
        this.scripts = scripts;
    }

    @Override
    public List<String> schemes() {
        return Collections.singletonList(SCHEME);
    }

    @Override
    public SchemeURI lookup(SchemeURI uri, String module) {
        SchemeURI result = new SchemeURI();
        if (uri.getScheme().equals(SCHEME)) {
            for (Script script : scripts) {
                if (script.name.equals(module)) {
                    result = new SchemeURI(SCHEME, module);
                    return result;
                }
            }
        }
        return result;
    }

    @Override
    public void load(SchemeURI uri, Environment env) throws Exception {
        if (!uri.getScheme().equals(SCHEME)) {
            throw new IllegalArgumentException("Illegal scheme: " + uri.getScheme());
        }
        env.eval(retrieveScriptByModuleOrThrow(uri.getPath()).javascript);
    }

    private Script retrieveScriptByModuleOrThrow(String module) {
        for (Script script : scripts) {
            if (script.name.equals(module)) {
                return script;
            }
        }
        throw new IllegalArgumentException("Module: " + module + " is not contained in the scripts");
    }

    public static class Script {
        public final String name;
        public final String javascript;

        public Script(String name, String javascript) {
            this.name = name;
            this.javascript = javascript;
        }
    }
}
