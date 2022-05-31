package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.proxies.ConfigProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProxyImpl implements ConfigProxy {
    private static final Logger log = LoggerFactory.getLogger(ConfigProxyImpl.class);

    public ConfigProxyImpl() {
    }


    @Override
    public String getConfigResource(String configName) {
        log.trace("ConfigProxy: getConfigResource({});", configName);
        final StopWatch stopWatch = new StopWatch();
        String systemProperty = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty(configName);
        log.debug("ConfigProxy: getConfigResource took {} milliseconds", stopWatch.getElapsedTime());
        return systemProperty;
    }

}
