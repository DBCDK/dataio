package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

/**
 * This stateless Enterprise Java Bean (EJB) class handles storing and retrieval of
 * file data
 */
@Stateless
public class FileStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreBean.class);

    @PersistenceContext
    EntityManager entityManager;

    @EJB
    BinaryFileStoreBean binaryFileStore;

    /**
     * Adds content of given input stream as file in store
     * @param dataSource input stream of bytes to be written
     * @return ID of generated file
     * @throws NullPointerException if given null-valued dataSource argument
     * @throws IllegalStateException on general failure to write data
     */
    public String addFile(InputStream dataSource) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(dataSource, "dataSource");
        final Path location = getCurrentLocation();
        FileAttributes fileAttributes = new FileAttributes(new Date(), location);
        fileAttributes = entityManager.merge(fileAttributes);
        entityManager.flush();

        final Path path = location.resolve(String.valueOf(fileAttributes.getId()));
        final BinaryFile binaryFile = binaryFileStore.getBinaryFile(path);
        binaryFile.write(dataSource);
        LOGGER.info("Wrote file {}", path.toString());
        return String.valueOf(fileAttributes.getId());
    }

    /**
     * Retrieves file content from store into given output stream
     * @param fileId ID of file
     * @param dataDestination output stream to which bytes are written
     * @throws NullPointerException if given null-valued fileId or dataDestination argument
     * @throws IllegalArgumentException if given empty valued fileId argument
     * @throws IllegalStateException on general failure to read data
     * @throws EJBException if no file attributes can be found for given file ID
     */
    public void getFile(String fileId, OutputStream dataDestination)
            throws NullPointerException, IllegalArgumentException, IllegalStateException, EJBException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
        InvariantUtil.checkNotNullOrThrow(dataDestination, "dataDestination");
        final FileAttributes fileAttributes = lookupFileAttributes(Long.parseLong(fileId));
        if (fileAttributes == null) {
            throw new EJBException(String.format("Trying to get non-existing file with ID '%s'", fileId));
        }

        final Path path = fileAttributes.getLocation().resolve(String.valueOf(fileAttributes.getId()));
        final BinaryFile binaryFile = binaryFileStore.getBinaryFile(path);
        binaryFile.read(dataDestination);
    }

    /**
     * Tests existence in store of file identified by given ID
     * @param fileId ID of file
     * @return true if file exists, false if not
     * @throws NullPointerException if given null-valued fileId argument
     * @throws IllegalArgumentException if given empty valued fileId argument
     */
    public boolean fileExists(String fileId) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
        try {
            final long id = Long.parseLong(fileId);
            return lookupFileAttributes(id) != null;
        } catch (NumberFormatException e) {
            LOGGER.warn("Given id '{}' is invalid", fileId);
            return false;
        }
    }

    private Path getCurrentLocation() {
        final Calendar now = Calendar.getInstance();
        final int year = now.get(Calendar.YEAR);
        final int month = now.get(Calendar.MONTH); // Note: zero based!
        final int day = now.get(Calendar.DAY_OF_MONTH);
        return Paths.get(String.format("%d/%02d/%02d", year, month + 1, day));
    }

    private FileAttributes lookupFileAttributes(long id) {
        return entityManager.find(FileAttributes.class, id);
    }
}
