package dk.dbc.dataio.gui.client.pages.basemaintenance;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

public class ViewModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(View.class).in(Singleton.class);
    }
}
