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

package dk.dbc.dataio.integrationtest;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.HttpClient;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Integration test utility
 */
public class ITUtil {
    public static final String FILE_STORE_BASE_URL = String.format("http://%s:%s%s",
            System.getProperty("container.hostname"), System.getProperty("container.http.port"), System.getProperty("file-store-service.context"));
    public static final String FLOW_STORE_BASE_URL = String.format("http://%s:%s%s",
                System.getProperty("container.hostname"), System.getProperty("container.http.port"), System.getProperty("flow-store-service.context"));
    public static final String JOB_STORE_BASE_URL = String.format("http://%s:%s%s",
                System.getProperty("container.hostname"), System.getProperty("container.http.port"), System.getProperty("job-store-service.context"));

    public static final String FLOWS_TABLE_NAME = "flows";
    public static final String FLOW_COMPONENTS_TABLE_NAME = "flow_components";
    public static final String FLOW_BINDERS_TABLE_NAME = "flow_binders";
    public static final String FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME = "flow_binders_submitters";
    public static final String FLOW_BINDERS_SEARCH_INDEX_TABLE_NAME = "flow_binders_search_index";
    public static final String SUBMITTERS_TABLE_NAME = "submitters";
    public static final String SINKS_TABLE_NAME = "sinks";
    public static final String GATEKEEPER_DESTINATIONS_TABLE_NAME = "gatekeeper_destinations";
    public static final String HARVESTER_CONFIGS_TABLE_NAME = "harvester_configs";

    private ITUtil() { }

    public static Connection newIntegrationTestConnection(String dbName) throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");

        final String dbUrl = String.format("jdbc:postgresql://localhost:%s/%s",
                System.getProperty("postgresql.port","5432"), dbName);
        final Connection connection = DriverManager.getConnection(dbUrl,
                System.getProperty("user.name"), System.getProperty("user.name"));
        connection.setAutoCommit(true);
        return connection;
    }

    /**
     * Deletes all rows from all named tables
     *
     * @param conn open database connection
     * @param tableNames table names
     * @throws SQLException if a database access error occurs
     */
    public static void clearDbTables(Connection conn, String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            JDBCUtil.update(conn, String.format("DELETE FROM %s", tableName));
        }
    }

    /**
     * Deletes all rows from all flow store database tables
     *
     * @param conn open database connection
     * @throws SQLException if a database access error occurs
     */
    public static void clearAllDbTables(Connection conn) throws SQLException {
        clearDbTables(conn,
                FLOW_BINDERS_SEARCH_INDEX_TABLE_NAME,
                FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME,
                FLOW_BINDERS_TABLE_NAME,
                FLOWS_TABLE_NAME,
                FLOW_COMPONENTS_TABLE_NAME,
                SUBMITTERS_TABLE_NAME,
                SINKS_TABLE_NAME,
                GATEKEEPER_DESTINATIONS_TABLE_NAME,
                HARVESTER_CONFIGS_TABLE_NAME);
    }

    public static void clearFileStore() {
        FileUtils.deleteQuietly(new File(System.getProperty("file.store.basepath")));
        try (final Connection connection = newIntegrationTestConnection("filestore")) {
            clearDbTables(connection, "file_attributes");
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void clearFlowStore() {
        try (final Connection connection = ITUtil.newIntegrationTestConnection("flowstore")) {
            clearAllDbTables(connection);
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long createFlowComponent(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS));
    }

    public static long createFlow(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.FLOWS));
    }

    public static long createSubmitter(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.SUBMITTERS));
    }

    public static long createSink(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.SINKS));
    }

    /**
     * @param response the response
     * @param headerName the name of the header
     * Extracts named header from given response while asserting that it is not null
     *
     * @return list of header values
     */
    public static List<Object> getHeaderAndAssertNotNull(Response response, String headerName) {
        final List<Object> header = response.getHeaders().get(headerName);
        Assert.assertThat(header, CoreMatchers.is(CoreMatchers.notNullValue()));
        return header;
    }

    /**
     * @param response the response
     * Extracts Location header from given response while asserting that it contains a single
     * value from which a resource Id can be derived
     * @return resource Id
     */
    public static long getResourceIdFromLocationHeaderAndAssertHasValue(Response response) {
        final List<Object> locationHeader = getHeaderAndAssertNotNull(response, "Location");
        Assert.assertThat(locationHeader.size(), CoreMatchers.is(1));

        final String[] locationHeaderValueParts = ((String) locationHeader.get(0)).split("/");
        Assert.assertThat(locationHeaderValueParts.length > 2, CoreMatchers.is(true));

        final String id = locationHeaderValueParts[locationHeaderValueParts.length - 1];

        return Long.parseLong(id);
    }

}
