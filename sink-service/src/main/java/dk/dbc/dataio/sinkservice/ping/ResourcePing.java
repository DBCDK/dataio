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

package dk.dbc.dataio.sinkservice.ping;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pings application server for necessary resource
 */
public class ResourcePing {

    private ResourcePing() { }

    /**
     * Executes resource ping
     *
     * @param context context for performing naming operations
     * @param resourceName name of resource
     * @param resourceClass type of resource
     * @param <T> the type of the object
     *
     * @return ping response
     *
     * @throws NullPointerException when given null-valued argument
     * @throws IllegalArgumentException when given empty-valued resourceName argument
     */
    public static <T> PingResponse execute(InitialContext context, String resourceName, Class<T> resourceClass)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(context, "context");
        InvariantUtil.checkNotNullNotEmptyOrThrow(resourceName, "resourceName");
        InvariantUtil.checkNotNullOrThrow(resourceClass, "resourceClass");
        PingResponse.Status status = PingResponse.Status.OK;
        final List<String> log = new ArrayList<>();
        try {
            doResourceLookup(context, resourceName, resourceClass);
            log.add(String.format("Found %s resource with name '%s'", resourceClass.getName(), resourceName));
        } catch (NamingException e) {
            status = PingResponse.Status.FAILED;
            log.add(String.format("Unable to find %s resource with name '%s' : %s", resourceClass.getName(), resourceName, e.getMessage()));
        }
        return new PingResponse(status, log);
    }

    private static <T> void doResourceLookup(InitialContext context, String resourceName, Class<T> resourceClass) throws NamingException {
        final Object lookup = context.lookup(resourceName);
        if (!resourceClass.isInstance(lookup)) {
            throw new NamingException("Unexpected type of resource returned from lookup");
        }
    }


}
