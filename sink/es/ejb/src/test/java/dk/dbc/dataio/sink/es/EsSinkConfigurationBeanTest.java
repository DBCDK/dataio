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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * EsSinkConfigurationBean unit tests.
 * The test methods of this class uses the following naming convention:
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsSinkConfigurationBeanTest {
    @Test
    public void getEsResourceName_esResourceNameResourceIsSet_returnsValue() {
        final String resourceValue = "resourceName";
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esResourceName = resourceValue;
        assertThat(configuration.getEsResourceName(), is(resourceValue));
    }

    @Test
    public void getEsDatabaseName_esDatabaseNameResourceIsSet_returnsValue() {
        final String resourceValue = "databaseName";
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esDatabaseName = resourceValue;
        assertThat(configuration.getEsDatabaseName(), is(resourceValue));
    }

    @Test
    public void getEsUserId_esUserIdResourceIsNotSet_returnsDefaultValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        assertThat(configuration.getEsUserId(), is(EsSinkConfigurationBean.DEFAULT_USER_ID));
    }

    @Test
    public void getEsUserId_esUserIdResourceIsSet_returnsValue() {
        final int userId = 42;
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esUserId = userId;
        assertThat(configuration.getEsUserId(), is(userId));
    }

    @Test
    public void getEsPackageType_esPackageTypeResourceIsNotSet_returnsDefaultValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        assertThat(configuration.getEsPackageType(), is(ESUtil.PackageType.valueOf(EsSinkConfigurationBean.DEFAULT_PACKAGE_TYPE)));
    }

    @Test
    public void getEsPackageType_esPackageTypeResourceIsSet_returnsValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esPackageType = ESUtil.PackageType.PERSISTENT_QUERY.name().toLowerCase();
        assertThat(configuration.getEsPackageType(), is(ESUtil.PackageType.PERSISTENT_QUERY));
    }

    @Test
    public void getEsAction_esActionResourceIsNotSet_returnsDefaultValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        assertThat(configuration.getEsAction(), is(ESUtil.Action.valueOf(EsSinkConfigurationBean.DEFAULT_ACTION)));
    }

    @Test
    public void getEsAction_esActionResourceIsSet_returnsValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esAction = ESUtil.Action.INSERT.name().toLowerCase();
        assertThat(configuration.getEsAction(), is(ESUtil.Action.INSERT));
    }
}
