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

package dk.dbc.dataio.commons.utils.test.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Enables mocking af the JNDI system through an in-memory InitialContext implementation.
 * <code>
 *     // sets up the InMemoryInitialContextFactory as default factory.
 *     System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
 *     // binds the object
 *     InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_FBS_WS, ENDPOINT);
 * </code>
 */
public class InMemoryInitialContextFactory implements InitialContextFactory {
    private static Map<String, Object> bindings = new HashMap<>();
    private static Context context;

    static {
        try {
            context = new InitialContext(true) {

                @Override
                public void bind(String name, Object obj) throws NamingException {
                    bindings.put(name, obj);
                }

                @Override
                public Object lookup(String name) throws NamingException {
                    if (!bindings.containsKey(name)) {
                        throw new NamingException("Unable to lookup name: " + name);
                    }
                    return bindings.get(name);
                }
            };
        } catch (NamingException e) { // can't happen.
            throw new RuntimeException(e);
        }
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return context;
    }

    public static void bind(String name, Object obj) {
        try {
            context.bind(name, obj);
        } catch (NamingException e) { // can't happen.
            throw new RuntimeException(e);
        }
    }

    public static void clear() {
        bindings.clear();
    }
}
