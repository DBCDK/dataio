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

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * This is the list of Dependency Injections for the JobFilter
 * They are directly callable methods, returning instance objects of the given type
 * Underlying Dependency Injections will automatically be injected
 */
@GinModules(JobFilterClientModule.class)
public interface JobFilterGinjector extends Ginjector {
    SinkJobFilter getSinkJobFilter();
    SubmitterJobFilter getSubmitterJobFilter();
    SuppressSubmitterJobFilter getSuppressSubmitterJobFilter();
    DateJobFilter getDateJobFilter();
    ErrorJobFilter getErrorJobFilter();
}
