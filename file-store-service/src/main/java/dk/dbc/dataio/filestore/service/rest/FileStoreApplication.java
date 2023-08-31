package dk.dbc.dataio.filestore.service.rest;

import dk.dbc.dataio.filestore.service.ejb.FilesBean;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class FileStoreApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(FilesBean.class, StatusBean.class);
    }
}
