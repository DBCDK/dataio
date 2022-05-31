package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;

import java.util.HashMap;

public final class Urls extends HashMap<String, String> {
    private static final Urls instance = new Urls();

    static {
        instance.put("ELK_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("ELK_URL"));
        instance.put("FILESTORE_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FILESTORE_URL"));
        instance.put("FLOWSTORE_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FLOWSTORE_URL"));
        instance.put("PERIODIC_JOBS_HARVESTER_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("PERIODIC_JOBS_HARVESTER_URL"));
        instance.put("FTP_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FTP_URL"));
        instance.put("JOBSTORE_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("JOBSTORE_URL"));
        instance.put("LOGSTORE_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("LOGSTORE_URL"));
        instance.put("OPENAGENCY_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("OPENAGENCY_URL"));
        instance.put("SUBVERSION_URL", ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("SUBVERSION_URL"));
    }

    private Urls() {
    }

    public static Urls getInstance() {
        return instance;
    }
}
