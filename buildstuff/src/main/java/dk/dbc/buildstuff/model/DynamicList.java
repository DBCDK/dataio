package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.ValueResolver;

import java.util.Map;
import java.util.stream.Collectors;

public class DynamicList extends ResolvingObject {


    public Map<String, ValueResolver> getValues(Namespace ns) {
        return properties.stream().collect(Collectors.toMap(p -> p.name, p -> p.getValue(ns)));
    }
}
