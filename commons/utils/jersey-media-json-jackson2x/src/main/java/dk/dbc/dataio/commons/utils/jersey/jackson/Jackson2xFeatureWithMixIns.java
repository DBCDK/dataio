package dk.dbc.dataio.commons.utils.jersey.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jaxrs.base.JsonParseExceptionMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.CommonProperties;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.util.Map;

public class Jackson2xFeatureWithMixIns implements Feature {
    private final JacksonJaxbJsonProvider provider;

    public Jackson2xFeatureWithMixIns(Map<Class<?>, Class<?>> mixIns) {
        final ObjectMapper mapper = new ObjectMapper();
        if (mixIns != null) {
            for (Map.Entry<Class<?>, Class<?>> e : mixIns.entrySet()) {
                mapper.addMixInAnnotations(e.getKey(), e.getValue());
            }
        }

        provider = new JacksonJaxbJsonProvider() {{
            setMapper(mapper);
        }};
    }

    @Override
    public boolean configure(FeatureContext context) {
        final String disableMoxy = CommonProperties.MOXY_JSON_FEATURE_DISABLE + '.'
                + context.getConfiguration().getRuntimeType().name().toLowerCase();
        context.property(disableMoxy, true);
        context.register(JsonParseExceptionMapper.class);
        context.register(JsonMappingExceptionMapper.class);
        context.register(provider, MessageBodyReader.class, MessageBodyWriter.class);
        return true;
    }
}
