package dk.dbc.dataio.gatekeeper;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.gatekeeper.operation.Opcode;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.Modification;

import java.util.ArrayList;
import java.util.List;

/**
 * This class derives the necessary WAL modifications from a given transfile
 */
public class ModificationFactory {
    private static final String EMPTY_STRING = "";

    private final TransFile transfile;

    /**
     * Class Constructor
     *
     * @param transfile for which modifications are to be listed
     * @throws NullPointerException if given null-valued transfile
     */
    public ModificationFactory(TransFile transfile) throws NullPointerException {
        this.transfile = InvariantUtil.checkNotNullOrThrow(transfile, "transfile");
    }

    /**
     * Pacakage scoped constructor used for unit testing
     */
    ModificationFactory() {
        this.transfile = null;
    }

    /**
     * @return list of modifications
     */
    public List<Modification> getModifications() {
        final ArrayList<Modification> modifications = new ArrayList<>();
        if (transfile.getLines().isEmpty()) {
            // Handle special case where transfile is empty.
            // For now just move to posthus...
            modifications.add(getCreateInvalidTransfileNotificationModification());
        } else if (!transfile.isValid()) {
            modifications.add(getCreateInvalidTransfileNotificationModification());
            modifications.add(getFileDeleteModification(transfile.getPath().getFileName().toString()));
        } else {
            for (TransFile.Line line : transfile.getLines()) {
                modifications.addAll(processLine(line));
            }

            // Delete the original transfile...
            modifications.add(getFileDeleteModification(
                    transfile.getPath().getFileName().toString()));
        }
        return modifications;
    }

    /* Returns modifications for given transfile line based on type
     */
    List<Modification> processLine(TransFile.Line line) {
        final ArrayList<Modification> modifications = new ArrayList<>();
        modifications.add(getCreateJobModification(line.getLine()));
        final String dataFilename = getDataFilename(line);
        if (!dataFilename.isEmpty()) {
            modifications.add(getFileDeleteModification(dataFilename));
        }
        return modifications;
    }

    String getDataFilename(TransFile.Line line) {
        final String f = line.getField("f");
        if (f == null) {
            return EMPTY_STRING;
        }
        return f.trim();
    }

    Modification getFileDeleteModification(String filename) {
        final Modification fileDelete = new Modification();
        fileDelete.setOpcode(Opcode.DELETE_FILE);
        fileDelete.setTransfileName(transfile.getPath().getFileName().toString());
        fileDelete.setArg(filename);
        return fileDelete;
    }

    Modification getCreateJobModification(String arg) {
        final Modification createTransfile = new Modification();
        createTransfile.setOpcode(Opcode.CREATE_JOB);
        createTransfile.setTransfileName(transfile.getPath().getFileName().toString());
        createTransfile.setArg(arg);
        return createTransfile;
    }

    Modification getCreateInvalidTransfileNotificationModification() {
        final Modification createNotification = new Modification();
        createNotification.setOpcode(Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION);
        createNotification.setTransfileName(transfile.getPath().getFileName().toString());
        String causeForInvalidation = transfile.getCauseForInvalidation();
        if (causeForInvalidation == null || causeForInvalidation.isEmpty()) {
            causeForInvalidation = "Transfilen har intet indhold";
        }
        createNotification.setArg(causeForInvalidation);
        return createNotification;
    }

}
