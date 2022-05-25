package dk.dbc.dataio.gui.client.components.flowbinderfilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class configures the filters for the flow binders listing
 */
final class FlowBinderFilterList {
    private Map<String, List<FlowBinderFilterItem>> flowBinderFilters = new HashMap<>();

    class FlowBinderFilterItem {
        BaseFlowBinderFilter flowBinderFilter;
        boolean activeOnStartup;

        FlowBinderFilterItem(BaseFlowBinderFilter flowBinderFilter, boolean activeOnStartup) {
            this.flowBinderFilter = flowBinderFilter;
            this.activeOnStartup = activeOnStartup;
        }
    }

    FlowBinderFilterList() {
        flowBinderFilters.put(dk.dbc.dataio.gui.client.pages.flowbinder.show.Place.class.getSimpleName(),
                Arrays.asList(
                        new FlowBinderFilterItem(new DestinationFilter("", false), false),
                        new FlowBinderFilterItem(new FlowFilter("", false), false),
                        new FlowBinderFilterItem(new FormatFilter("", false), false),
                        new FlowBinderFilterItem(new NameFilter("", false), false),
                        new FlowBinderFilterItem(new QueueProviderFilter("", false), false),
                        new FlowBinderFilterItem(new PackagingFilter("", false), false),
                        new FlowBinderFilterItem(new DataPartitionerFilter("", false), false),
                        new FlowBinderFilterItem(new SinkFilter("", false), false),
                        new FlowBinderFilterItem(new SubmitterFilter("", false), false),
                        new FlowBinderFilterItem(new CharsetFilter("", false), false)
                ));
    }

    FlowBinderFilterList(Map<String, List<FlowBinderFilterItem>> flowBinderFilters) {
        this.flowBinderFilters = flowBinderFilters;
    }

    List<FlowBinderFilterItem> getFilters(String place) {
        return flowBinderFilters.get(place);
    }
}
