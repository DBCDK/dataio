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

package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;


public interface Presenter extends GenericPresenter {
    void nameChanged(String name);
    void descriptionChanged(String text);
    void resourceChanged(String resource);
    void consumerIdChanged(String consumerId);
    void sizeChanged(String size);
    String formatOverrideAdded(String overrideKey, String overrideValue);
    void relationsChanged(Boolean relations);
    void libraryRulesChanged(Boolean libraryRules);
    void harvesterTypeChanged(String value);
    void holdingsTargetChanged(String text);
    void destinationChanged(String destination);
    void formatChanged(String format);
    void typeChanged(String type);
    void noteChanged(String text);
    void enabledChanged(Boolean value);
    void keyPressed();
    void saveButtonPressed();
    void updateButtonPressed();
    void deleteButtonPressed();
    void formatOverridesAddButtonPressed();

    void formatOverridesRemoveButtonPressed(String item);
}
