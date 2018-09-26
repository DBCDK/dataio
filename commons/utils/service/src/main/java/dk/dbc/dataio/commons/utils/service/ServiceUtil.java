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

package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ServiceError;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This utility class provides convenience methods for working with Servlet/JAX-RS based web services
 */
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);
    private static final JSONBContext jsonbContext = new JSONBContext();

    private ServiceUtil() { }

    /**
     * Looks up job-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_JOBSTORE_RS}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_JOBSTORE_RS}'
     * system property.
     *
     * @return job-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getJobStoreServiceEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_JOBSTORE_RS);
    }

    /**
     * Looks up file-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_FILESTORE_RS}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_FILESTORE_RS}'
     * system property.
     *
     * @return file-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getFileStoreServiceEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_FILESTORE_RS);
    }

    /**
     * Looks up flow-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#FLOW_STORE_SERVICE_ENDPOINT_RESOURCE}'. For testing purposes
     * the JNDI lookup can be bypassed by defining a 'flowStoreURL' system property.
     *
     * @return flow-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getFlowStoreServiceEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
    }

    /**
     * Looks up log-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_LOGSTORE_RS}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_LOGSTORE_RS}'
     * system property.
     *
     * @return log-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getLogStoreServiceEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_LOGSTORE_RS);
    }

    /**
     * Looks up subversion SCM endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#SUBVERSION_SCM_ENDPOINT_RESOURCE}'. For testing purposes
     * the JNDI lookup can be bypassed by defining a '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#SUBVERSION_SCM_ENDPOINT_RESOURCE}'
     * system property.
     *
     * @return subversion repository URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getSubversionScmEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE);
    }

    /**
     * Looks up USH Solr endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_USH_SOLR}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_USH_SOLR}' system property.
     * @return USH Solr URL as String
     * @throws NamingException if unable to lookup name
     */
    public static String getUshSolrEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_USH_SOLR);
    }

    /**
     * Looks up USH Harvester endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_USH_HARVESTER}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_USH_HARVESTER}' system property.
     * @return USH Harvester URL as String
     * @throws NamingException if unable to lookup name
     */
    public static String getUshHarvesterEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_USH_HARVESTER);
    }

    /**
     * Looks up USH Solr Harvester endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_USH_SOLR_HARVESTER_RS}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_USH_SOLR_HARVESTER_RS}' system property.
     * @return USH Solr Harvester URL as String
     * @throws NamingException if unable to lookup name
     */
    public static String getUshSolrHarvesterServiceEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_USH_SOLR_HARVESTER_RS);
    }

    /**
     * Looks up Tickle Harvester endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_HARVESTER_TICKLE_RS}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_HARVESTER_TICKLE_RS}' system property.
     * @return Tickle Harvester URL as String
     * @throws NamingException if unable to lookup name
     */
    public static String getTickleHarvesterServiceEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_HARVESTER_TICKLE_RS);
    }

    /**
     * Looks up Open Agency service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_OPEN_AGENCY}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_OPEN_AGENCY}' system property.
     * @return Open Agency service URL as String
     * @throws NamingException if unable to lookup name
     */
    public static String getOpenAgencyEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_OPEN_AGENCY);
    }

    public static String getFbsEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_FBS_WS);
    }

    public static String getGuiFtpEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_GUI_FTP);
    }

    public static String getElkEndpoint() throws NamingException {
        return getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_ELK);
    }

    /**
     * Builds service method response
     *
     * @param status HTTP status code of response
     * @param entity entity to include in response
     * @param <T> type parameter for entity type
     *
     * @return response object
     */
    public static <T> Response buildResponse(Response.Status status, T entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.commons.types.ServiceError object
     * constructed from given exception
     *
     * @param ex exception to wrap
     *
     * @return JSON string representation of ServiceError object
     */
    public static String asJsonError(Exception ex) {
        return asJsonError(ex, null);
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.commons.types.ServiceError object
     * constructed from given exception and message
     *
     * @param ex exception to wrap
     * @param message describing the error
     *
     * @return JSON string representation of ServiceError object
     */
    public static String asJsonError(Exception ex, String message) {
        String error = null;
        try {
            log.error("Generating error based on exception", ex);
            error = jsonbContext.marshall(new ServiceError().withMessage(message).withDetails(ex.getMessage()).withStacktrace(stackTraceToString(ex)));
        } catch (JSONBException e) {
            log.error("Caught exception trying to create JSON representation of error", e);
        }
        return error;
    }

    public static String stackTraceToString(Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Looks up a resource through Java Naming and Directory Interface (JNDI)
     * using the name passed as a parameter in the call to this method. For testing purposes
     * the JNDI lookup can be bypassed by defining a similar named system property.
     *
     * @param resourceName The name of the resource
     * @return JNDI or System Property name as String
     * @throws NamingException if unable to lookup name
     */
    public static String getStringValueFromSystemPropertyOrJndi(String resourceName) throws NamingException {
        String value = getStringValueFromSystemProperty(resourceName);
        if (value == null || value.isEmpty()) {
            value = getStringValueFromResource(resourceName);
        }
        return value;
    }

    /**
     * Looks up a resource through System Environment or System Property
     * using the name passed as a parameter in the call to this method.
     *
     * @param resourceName The name of the resource
     * @return System Environment or System Property as String
     */
    public static String getStringValueFromSystemEnvironmentOrProperty(String resourceName) {
        String value = getStringValueFromSystemEnvironment(resourceName);
        if (value == null || value.isEmpty()) {
            value = getStringValueFromSystemProperty(resourceName);
        }
        return value;
    }

    /**
     * Looks up a resource through named system property.
     *
     * @param resourceName The name of the resource
     * @return System Property name as String
     */
    public static String getStringValueFromSystemProperty(String resourceName) {
        return System.getProperty(resourceName);
    }

    /**
     * Looks up a resource through named environment variable.
     *
     * @param resourceName The name of the resource
     * @return System Property name as String
     */
    public static String getStringValueFromSystemEnvironment(String resourceName) {
        return System.getenv(resourceName);
    }

    /**
     * Looks up a resource through Java Naming and Directory Interface (JNDI)
     * using the name passed as a parameter in the call to this method
     *
     * @param resourceName The name of the resource
     * @return The string content of the resource, if found
     * @throws NamingException if unable to lookup name
     */
    public static String getStringValueFromResource(String resourceName) throws NamingException {
        String resourceValue;
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            resourceValue = (String) initialContext.lookup(resourceName);
        } catch (NamingException e) {
            // a kind of compatibility mode for defining values as custom
            // resources in glassfish-web.xml which have previously been
            // set by asadmin add-resources and which were globally scoped.
            // when set in glassfish-resources they need to be scoped to
            // java:app.
            final String jndiPrefix = "java:app/";
            if(!resourceName.startsWith(jndiPrefix)) {
                return getStringValueFromResource(jndiPrefix + resourceName);
            }
            throw e;
        } finally {
            closeInitialContext(initialContext);
        }
        return resourceValue;
    }

    private static void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                log.warn("Unable to close initial context", e);
            }
        }
    }
}
