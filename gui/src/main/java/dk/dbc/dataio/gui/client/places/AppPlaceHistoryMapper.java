package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({
        dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flow.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.submitter.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.javascriptlog.JavaScriptLogPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.sink.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.newJob.show.Place.Tokenizer.class,
        dk.dbc.dataio.gui.client.pages.item.show.Place.Tokenizer.class
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}

