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

package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerRestBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsExportsBean;
import dk.dbc.dataio.jobstore.service.ejb.NotificationsBean;
import dk.dbc.dataio.jobstore.service.ejb.RerunsBean;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobStoreApplication extends Application {
    private static final Set<Class<?>> classes = new HashSet<>();
    static {
        classes.add(JobsBean.class);
        classes.add(JobsExportsBean.class);
        classes.add(NotificationsBean.class);
        classes.add(StatusBean.class);
        classes.add(JobSchedulerRestBean.class);
        classes.add(RerunsBean.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
