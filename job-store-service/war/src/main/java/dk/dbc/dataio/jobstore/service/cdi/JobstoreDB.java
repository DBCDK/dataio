package dk.dbc.dataio.jobstore.service.cdi;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface JobstoreDB {
}
