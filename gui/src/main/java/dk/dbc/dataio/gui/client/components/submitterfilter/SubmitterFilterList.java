/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.components.submitterfilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class configures the filters for the submitters listing
 */
final class SubmitterFilterList {
    private Map<String, List<SubmitterFilterItem>> submitterFilters = new HashMap<>();

    class SubmitterFilterItem {
        BaseSubmitterFilter submitterFilter;
        boolean activeOnStartup;

        SubmitterFilterItem(BaseSubmitterFilter submitterFilter, boolean activeOnStartup) {
            this.submitterFilter = submitterFilter;
            this.activeOnStartup = activeOnStartup;
        }
    }

    SubmitterFilterList() {
        submitterFilters.put(dk.dbc.dataio.gui.client.pages.submitter.show.Place.class.getSimpleName(),
                Arrays.asList(
                        new SubmitterFilterItem(new NumberFilter("", false), false)
                ));
    }

    SubmitterFilterList(Map<String, List<SubmitterFilterItem>> submitterFilters) {
        this.submitterFilters = submitterFilters;
    }

    List<SubmitterFilterItem> getFilters(String place) {
        return submitterFilters.get(place);
    }
}
