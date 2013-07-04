package dk.dbc.dataio.gui.places;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({
    FlowEditPlace.Tokenizer.class,
})

public interface AppPlaceHistoryMapper extends PlaceHistoryMapper
{
}

