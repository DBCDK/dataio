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
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;

import java.util.List;

public class Script {
    public static final String INTERNAL_LOAD_REQUIRE_CACHE = "__internal_load_require_cache";
    public static final String DEFINE_REQUIRE_CACHE_FUNCTION_JAVASCRIPT = "use(\"Require\");\n" +
            "function "+ INTERNAL_LOAD_REQUIRE_CACHE + "( json ) {\n" +
            "Require.setCache( JSON.parse(json) );\n" +
            "};";

    private final Environment jsEnvironment;
    private final String invocationMethod;
    private final String scriptId;

    public Script(String scriptId, String invocationMethod,
                  List<StringSourceSchemeHandler.Script> javascripts,
                  String requireCacheJson ) throws Throwable {
        final ModuleHandler mh = new ModuleHandler();
        StringSourceSchemeHandler sssh = new StringSourceSchemeHandler(javascripts);
        mh.registerHandler("string", sssh);
        mh.addSearchPath(new SchemeURI("string", "."));
        jsEnvironment = new Environment();
        jsEnvironment.registerUseFunction(mh);
        if( requireCacheJson != null ) {
            loadRequireCache( requireCacheJson );
        }
        jsEnvironment.eval(javascripts.get(0).javascript);
        this.scriptId = scriptId;
        this.invocationMethod = invocationMethod;
    }


    private void loadRequireCache(String requireCacheJSON) throws Throwable {
        jsEnvironment.eval(DEFINE_REQUIRE_CACHE_FUNCTION_JAVASCRIPT);
        jsEnvironment.callMethod(INTERNAL_LOAD_REQUIRE_CACHE, new Object[]{requireCacheJSON});
    }

    public Object invoke(final Object[] args) throws Throwable {
        return jsEnvironment.callMethod(invocationMethod, args);
    }

    public Object eval(String s) throws Throwable {
        return jsEnvironment.eval(s);
    }

    public String getScriptId() {
        return scriptId;
    }

    public String getInvocationMethod() {
        return invocationMethod;
    }
}
