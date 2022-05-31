package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class HarvestRequest<T extends HarvestRequest<T>> implements Serializable {
    private static final long serialVersionUID = 6524469166041870343L;
}
