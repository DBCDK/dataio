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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import org.junit.Test;

import javax.ejb.EJBException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenUpdateServiceConnectorBeanTest {

    private static final String template = "bog";

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final OpenUpdateServiceConnectorBean openUpdateServiceConnectorBean = new OpenUpdateServiceConnectorBean();
        openUpdateServiceConnectorBean.initializeConnector();
    }

    @Test
    public void updateRecord_returnsUpdateRecordResult() {
        final OpenUpdateServiceConnector openUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);

        final OpenUpdateServiceConnectorBean openUpdateServiceConnectorBean = new OpenUpdateServiceConnectorBean();
        openUpdateServiceConnectorBean.openUpdateServiceConnector = openUpdateServiceConnector;

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();

        when(openUpdateServiceConnector.updateRecord(anyString(), any(BibliographicRecord.class))).thenReturn(updateRecordResult);

        UpdateRecordResult returnedUpdateRecordResult = openUpdateServiceConnectorBean.getConnector().updateRecord(template, new BibliographicRecord());

        assertThat(returnedUpdateRecordResult, is(updateRecordResult));
    }

}