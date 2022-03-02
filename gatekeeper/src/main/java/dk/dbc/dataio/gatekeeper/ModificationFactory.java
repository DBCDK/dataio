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

    enum Type {
        DATAIO_EXCLUSIVE
    }

    private final TransFile transfile;
    private final StringBuilder newTransfile = new StringBuilder();

    /**
     * Class Constructor
     *
     * @param transfile                 for which modifications are to be listed
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
        newTransfile.setLength(0);

        final ArrayList<Modification> modifications = new ArrayList<>();
        if (transfile.getLines().isEmpty()) {
            // Handle special case where transfile is empty.
            // For now just move to posthus...
            modifications.add(getFileMoveModification(transfile.getPath().getFileName().toString()));
        } else if (!transfile.isValid()) {
            modifications.add(getCreateInvalidTransfileNotificationModification());
            modifications.add(getFileDeleteModification(transfile.getPath().getFileName().toString()));
        } else {
            for (TransFile.Line line : transfile.getLines()) {
                modifications.addAll(processLine(line));
            }
            if (newTransfile.length() > 0) {
                // Since lines may be excluded due to exclusivity we simply write a
                // new transfile with the content from the newTransfile buffer.
                newTransfile.append("slut");
                modifications.add(getCreateTransfileModification(newTransfile.toString()));
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

    Modification getFileMoveModification(String filename) {
        final Modification fileMove = new Modification();
        fileMove.setOpcode(Opcode.MOVE_FILE);
        fileMove.setTransfileName(transfile.getPath().getFileName().toString());
        fileMove.setArg(filename);
        return fileMove;
    }

    Modification getCreateJobModification(String arg) {
        final Modification createTransfile = new Modification();
        createTransfile.setOpcode(Opcode.CREATE_JOB);
        createTransfile.setTransfileName(transfile.getPath().getFileName().toString());
        createTransfile.setArg(arg);
        return createTransfile;
    }

    Modification getCreateTransfileModification(String arg) {
        final Modification createTransfile = new Modification();
        createTransfile.setOpcode(Opcode.CREATE_TRANSFILE);
        createTransfile.setTransfileName(transfile.getPath().getFileName().toString());
        createTransfile.setArg(arg);
        return createTransfile;
    }

    Modification getCreateInvalidTransfileNotificationModification() {
        final Modification createNotification = new Modification();
        createNotification.setOpcode(Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION);
        createNotification.setTransfileName(transfile.getPath().getFileName().toString());
        createNotification.setArg(transfile.getCauseForInvalidation());
        return createNotification;
    }

}
