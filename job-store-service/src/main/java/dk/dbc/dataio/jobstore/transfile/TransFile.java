package dk.dbc.dataio.jobstore.transfile;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;


//public class TransFileException extends Exception {}

/**
 *
 * @author slf
 */
public class TransFile {
    private static final String ENCODING = "UTF-8";
//    private static final Logger log = LoggerFactory.getLogger(TransFile.class);

    private final InputStream inputStream;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        TransFile parser = new TransFile(new FileInputStream("/Users/slf/projects/dio/job-store-service/src/main/java/dk/dbc/dataio/jobstore/transfile/150014.albums.alt-godt.trans"));
        parser.processLineByLine();
//        log.info("Done.");
    }

    /**
     * TransFile constructor
     */
    private TransFile(FileInputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    /**
     * Processes all lines in the input stream
     */
    public void processLineByLine() {
        Scanner fileScanner =  new Scanner(inputStream, ENCODING);
        while (fileScanner.hasNextLine()) {
            if (fileScanner.hasNext("slut")) {
                System.out.println("Slut blev fundet");
                break;
            }
            processLine(fileScanner.nextLine());
        }      
//        throw new 
    }
    
    /**
     * Processes one line in the Transfile
     * @param line The text to process
     */
    public void processLine(String line) {
        System.out.println("Line: " + line);
        Scanner lineScanner =  new Scanner(line);
        lineScanner.useDelimiter(",");
        while (lineScanner.hasNext()){
            processTransField(lineScanner.next());
        }      
    }

    /**
     * Processes a TransFile pair in the form: X=value, where X is a 
     * @param field 
     */
    private void processTransField(String field) {
        System.out.println(" Field: " + field);
        Scanner fieldScanner =  new Scanner(field);
        fieldScanner.useDelimiter("=");
//        while (fieldScanner.hasNext()){
//            processTransPair(pairScanner.next());
//        }      
    }
}
