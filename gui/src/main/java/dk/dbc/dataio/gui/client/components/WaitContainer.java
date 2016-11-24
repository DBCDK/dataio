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

package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class waits for the arrival of multiple objects
 */
public class WaitContainer {
    private Consumer<Map<String, Object>> callback = null;
    private Map<String, Object> waitingList = new HashMap<>();


    /**
     * Constructor taking the callback lambda and the list of Object identifiers, each identifying an object to observe
     * @param callback The callback lambda to be called, when all objects are ready
     * @param keys A list of strings, identifying a list of Objects to observe
     */
    public WaitContainer(Consumer<Map<String, Object>> callback, String... keys) {
        this.callback = callback;
        if (keys != null) {
            for (String key: keys) {
                waitingList.put(key, null);
            }
        }
    }

    /**
     * This method is called whenever an object is delivered and ready<br>
     *     When all objects are collected, the callback lambda delivered in the constructor is activated<br>
     *     Please note, that multiple calls to put() on the same variable is possible - still, whenever all variables are present, this results in an activation of the lambda method
     * @param key A string, identifying the observed object
     * @param object The object, that is delivered to the Await class
     */
    public void put(String key, Object object) {
        GWT.log("WaitContainer.put('" + key + "')");
        if (waitingList.containsKey(key)) {
            waitingList.put(key, object);
        }
        if (areAllReady()) {
            if (callback != null) {
                GWT.log("  -> callback, " + waitingList.size() + " items");
                callback.accept(waitingList);
            }
        }
    }

    /**
     * This method checks, whether all objects are collected
     * @return True if all objects are collected, and ready, false if not
     */
    private boolean areAllReady() {
        for (String key: waitingList.keySet()) {
            if (waitingList.get(key) == null) {
                return false;
            }
        }
        return true;
    }

}
