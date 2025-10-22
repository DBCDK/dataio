package dk.dbc.dataio.flowstore.model;

import dk.dbc.dataio.commons.types.SubmitterContent;

public record SubmitterContentLight(Long id, String name, long number) {
    public static SubmitterContentLight from(Long id, SubmitterContent content) {
        return new SubmitterContentLight(id, content.getName(), content.getNumber());
    }
}
