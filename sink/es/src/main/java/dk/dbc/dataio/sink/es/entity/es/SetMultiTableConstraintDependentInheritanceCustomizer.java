package dk.dbc.dataio.sink.es.entity.es;

import org.eclipse.persistence.config.DescriptorCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;

/**
 * Created by ja7 on 24-09-14.
 *
 * JPA Description Customizer used to work around EclipseLink/JPA specification bug
 *   Se    Bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=333100
 *
 * Workaround taken from.
 * http://stackoverflow.com/questions/21384589/jpa-inheritance-foreign-key-constraint-fails
 *
 */
public class SetMultiTableConstraintDependentInheritanceCustomizer implements DescriptorCustomizer {

    @Override
    public void customize(ClassDescriptor descriptor) throws Exception {
        descriptor.setHasMultipleTableConstraintDependecy(true);
    }

}
