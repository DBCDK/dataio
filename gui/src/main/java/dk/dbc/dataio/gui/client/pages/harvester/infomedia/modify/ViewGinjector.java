/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */
package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();
    Texts getTexts();
}
