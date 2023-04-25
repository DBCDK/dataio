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
        dk.dbc.dataio.gui.client.pages.flowbinder.status.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.corepo.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.corepo.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.corepo.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.rr.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.rr.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.rr.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.infomedia.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.promat.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.promat.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.dmat.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.harvester.dmat.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.iotraffic.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.item.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.purge.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.ShowAcctestJobsPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.job.show.ShowPeriodicJobsPlace.Tokenizer.class,
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
