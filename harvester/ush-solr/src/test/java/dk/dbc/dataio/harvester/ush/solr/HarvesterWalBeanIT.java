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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.ush.solr.entity.ProgressWal;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class HarvesterWalBeanIT extends IntegrationTest {
    @Test
    public void testWal() throws HarvesterException {
        final ProgressWal progressWal = new ProgressWal()
                .withConfigId(42L)
                .withConfigVersion(1L)
                .withHarvestedFrom(new Date())
                .withHarvestedUntil(new Date());

        final HarvesterWalBean harvesterWalBean = createHarvesterWalBean();

        final Optional<ProgressWal> uncommitted = persistenceContext.run(() -> {
            harvesterWalBean.write(progressWal);
            return harvesterWalBean.read(progressWal.getConfigId());
        });
        assertThat("Uncommitted entry can be retrieved", uncommitted.isPresent(), is(true));
        assertThat("Uncommitted entry", uncommitted.get(), is(progressWal));

        persistenceContext.run(() -> {
            harvesterWalBean.commit(uncommitted.get());
        });
        assertThat("Committed entry no longer exists", entityManager.find(ProgressWal.class, progressWal.getConfigId()), is(nullValue()));
    }

    private HarvesterWalBean createHarvesterWalBean() {
        final HarvesterWalBean harvesterWalBean = new HarvesterWalBean();
        harvesterWalBean.entityManager = entityManager;
        return harvesterWalBean;
    }
}