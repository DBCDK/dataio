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

package dk.dbc.dataio.sinkservice.ejb;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
  * PingBean unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    PingBean.class,
})
public class PingBeanTest {
    private static final String JDBC_RESOURCE_NAME = "jdbc/db";
    private static final String URL_RESOURCE_NAME = "url/path";

    @Test(expected = NullPointerException.class)
    public void ping_sinkContentDataIsNull_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping(null);
    }

    @Test(expected = JSONBException.class)
    public void ping_sinkContentDataIsEmpty_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping("");
    }

    @Test(expected = JSONBException.class)
    public void ping_sinkContentDataIsInvalidJson_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping("{");
    }

    @Test(expected = JSONBException.class)
    public void ping_sinkContentDataIsInvalidSinkContent_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping("{\"name\": \"name\"}");
    }

    @Test(expected = EJBException.class)
    public void ping_initialContextCreationThrows_throws() throws Exception {
        whenNew(InitialContext.class).withNoArguments().thenThrow(new NamingException());
        final PingBean pingBean = new PingBean();
        pingBean.ping(getValidSinkContent(URL_RESOURCE_NAME));
    }

    @Test
    public void ping_pingingUrlResource_returnsOkResponse() throws Exception {
        final PingBean pingBean = new PingBean();
        final Response response = pingBean.ping(getValidSinkContent(URL_RESOURCE_NAME));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void ping_pingingJdbcResource_returnsOkResponse() throws Exception {
        final PingBean pingBean = new PingBean();
        final Response response = pingBean.ping(getValidSinkContent(JDBC_RESOURCE_NAME));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    @Test(expected = EJBException.class)
    public void ping_pingingUnknownResourceType_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping(getValidSinkContent("unknown/resource"));
    }

    private String getValidSinkContent(String resourceName) throws JSONBException {
        final SinkContent sinkContent = new SinkContent("name", resourceName, "description");
        return new JSONBContext().marshall(sinkContent);
    }
}
