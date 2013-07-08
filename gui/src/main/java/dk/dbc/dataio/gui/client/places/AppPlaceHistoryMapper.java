package dk.dbc.dataio.gui.client.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({
    FlowCreatePlace.Tokenizer.class,
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper
{
}

