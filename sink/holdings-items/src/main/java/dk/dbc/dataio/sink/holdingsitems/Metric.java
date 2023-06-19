package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    CHUNK_ITEMS("dataio_sink_holdings_items_chunk_item_counter"),
    UNHANDLED_EXCEPTIONS("dataio_sink_holdings_items_exception_counter"),
    SET_HOLDINGS_REQUESTS("dataio_sink_holdings_items_setholdings_request_timer");

    public final String name;

    Metric(String name) {
        this.name = name;
    }
}
