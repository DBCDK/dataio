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

package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.jobprocessor.ejb.CapacityBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorCapacityExceededException;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class StatusBeanTest {
    @Test
    public void statusBeanReturnsResponseWhenCapacityIsNotExceeded() {
        assertThat(createStatusBean().getStatus(), is(notNullValue()));
    }

    @Test
    public void statusBeanThrowsWhenCapacityIsExceeded() {
        final StatusBean statusBean = createStatusBean();
        statusBean.capacityBean.signalCapacityExceeded();
        assertThat(statusBean::getStatus, isThrowing(JobProcessorCapacityExceededException.class));
    }

    private StatusBean createStatusBean() {
        final StatusBean statusBean = new StatusBean();
        statusBean.capacityBean = new CapacityBean();
        return statusBean;
    }
}