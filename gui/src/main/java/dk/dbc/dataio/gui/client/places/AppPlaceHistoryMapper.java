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

package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({
        dk.dbc.dataio.gui.client.pages.basemaintenance.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.failedftps.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.corepo.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.corepo.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.corepo.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.holdingsitem.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.holdingsitem.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.holdingsitem.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.rr.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.rr.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.rr.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.infomedia.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.iotraffic.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.item.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.purge.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.ShowAcctestJobsPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.ShowTestJobsPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.status.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.show.Place.Tokenizer.class,
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}
/*
 * List of Place Tokenizers
 * The list is alphabetically ordered to prevent duplicates - please keep the ordering
 */

