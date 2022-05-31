package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

/**
 * Gin Module for Flowbinder Show
 */
public class ViewModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(View.class).in(Singleton.class);
        bind(Texts.class).in(Singleton.class);
    }
}
