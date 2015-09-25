package dk.dbc.dataio.sink.es.entity.es;

//import org.eclipse.persistence.annotations.ReadOnly;

//import org.eclipse.persistence.annotations.ReadOnly;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;

/**
 * Created by ja7 on 26-09-14.
 *  Entity Class for reading from updatePackagesView
 */
@Entity
//@ReadOnly
@Table(name="updatepackages")
public class UpdatePackageEntity {
    @Id
    public BigInteger targetreference;
}
