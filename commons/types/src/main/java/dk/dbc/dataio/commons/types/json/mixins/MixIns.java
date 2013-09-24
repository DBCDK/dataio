package dk.dbc.dataio.commons.types.json.mixins;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to all JSON mixins
 */
public class MixIns {
    private static Map<Class<?>, Class<?>> mixIns = new HashMap<>();
    static {
        mixIns.put(Flow.class, FlowJsonMixIn.class);
        mixIns.put(FlowBinder.class, FlowBinderJsonMixIn.class);
        mixIns.put(FlowBinderContent.class, FlowBinderContentJsonMixIn.class);
        mixIns.put(FlowContent.class, FlowContentJsonMixIn.class);
        mixIns.put(FlowComponent.class, FlowComponentJsonMixIn.class);
        mixIns.put(FlowComponentContent.class, FlowComponentContentJsonMixIn.class);
        mixIns.put(JavaScript.class, JavaScriptJsonMixIn.class);
        mixIns.put(Submitter.class, SubmitterJsonMixIn.class);
        mixIns.put(SubmitterContent.class, SubmitterContentJsonMixIn.class);
    }

    private MixIns() { }

    public static Map<Class<?>, Class<?>> getMixIns() {
        return mixIns;
    }
}
