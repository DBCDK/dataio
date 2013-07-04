package dk.dbc.dataio.gui.activities;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.places.FlowEditPlace;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 * @author slf
 */
public class AppActivityMapper implements ActivityMapper {
    private ClientFactory clientFactory;

    public AppActivityMapper(ClientFactory clientFactory) {
        super();
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(Place place) {
        if (place instanceof FlowEditPlace)
            return new FlowEditActivity((FlowEditPlace) place, clientFactory);
        return null;
    }
}
