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

package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.sink.fbs.connector.FbsUpdateConnector;
import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.junit.Test;

import javax.ejb.EJBException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FbsUpdateConnectorBeanTest {

    private static final String collection = "collection";
    private static final String trackingId = "trackingId";

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.initializeConnector();
    }

    @Test(expected = FbsUpdateConnectorException.class)
    public void updateMarcExchange_throwsFbsUpdateConnectorException() throws Exception {

        final FbsUpdateConnector fbsUpdateConnector = mock(FbsUpdateConnector.class);
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.fbsUpdateConnector = fbsUpdateConnector;

        when(fbsUpdateConnector.updateMarcExchange(any(String.class), any(String.class)))
                .thenThrow(new FbsUpdateConnectorException("FbsUpdateConnectorException thrown"));
        fbsUpdateConnectorBean.getConnector().updateMarcExchange(collection, trackingId);
    }

    @Test
    public void updateMarcExchange_returnsUpdateMarcXchangeResult() throws Exception {

        final FbsUpdateConnector fbsUpdateConnector = mock(FbsUpdateConnector.class);
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.fbsUpdateConnector = fbsUpdateConnector;

        UpdateMarcXchangeResult updateMarcXchangeResult = new UpdateMarcXchangeResult();

        when(fbsUpdateConnector.updateMarcExchange(any(String.class), any(String.class)))
                .thenReturn(updateMarcXchangeResult);

        UpdateMarcXchangeResult returnedupdateMarcXchangeResult =
                fbsUpdateConnectorBean.getConnector().updateMarcExchange(collection, trackingId);

        assertThat(returnedupdateMarcXchangeResult, is(updateMarcXchangeResult));
    }
}