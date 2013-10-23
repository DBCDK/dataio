package dk.dbc.dataio.commons.utils.transfile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TransFile {
    private static final String ENCODING = "UTF-8";
    public static class UnexpectedEndOfFileException extends RuntimeException {}

//    private static final Logger log = LoggerFactory.getLogger(TransFile.class);


    /**
     * TransFile constructor
     */
    private TransFile() {}  // The constructor is private to make the class static
    
    /**
     * Processes all lines in a TransFile input stream
     * 
     * @param inputStream
     * @return
     * @throws UnexpectedEndOfFileException
     * @throws IllegalArgumentException 
     */
    public static List<TransFileData> process(InputStream inputStream) throws UnexpectedEndOfFileException, IllegalArgumentException {
        List<TransFileData> transFile = new ArrayList<>();
        Scanner fileScanner = new Scanner(inputStream, ENCODING);

        while (fileScanner.hasNextLine()) {
            if (fileScanner.hasNext("slut")) {
                return transFile;
            }
            transFile.add(new TransFileData(fileScanner.nextLine()));
        }
        throw new UnexpectedEndOfFileException();
    }
    
}
