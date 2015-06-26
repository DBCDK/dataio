package dk.dbc.dataio.bfs.ejb;

/**
 * Test utility class
 */
public class BinaryFileStoreBeanTestUtil {
    private BinaryFileStoreBeanTestUtil() { }

    /**
     * Creates BinaryFileStoreBean usable outside of application server environment
     * when used in conjunction with the InMemoryInitialContextFactory class.
     * <P>
     * <code>
     *  <P>
     *  System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
     *  <P>
     *  final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
     *  <P>
     *  InMemoryInitialContextFactory.bind(BFS_BASE_PATH_JNDI_NAME, someFolder);
     *  <P>
     *  final BinaryFileStoreBean bean = BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME);
     *  <P>
     * </code>
     * @param basePathJndiName JNDI lookup name of binary file store base path
     * @return BinaryFileStoreBean instance able to read from and write to folder resolved by JNDI lookup
     */
    public static BinaryFileStoreBean getBinaryFileStoreBean(String basePathJndiName) {
        final BinaryFileStoreConfigurationBean binaryFileStoreConfigurationBean = new BinaryFileStoreConfigurationBean();
        binaryFileStoreConfigurationBean.basePathJndiName = basePathJndiName;
        binaryFileStoreConfigurationBean.initialize();
        final BinaryFileStoreBean binaryFileStoreBean = new BinaryFileStoreBean();
        binaryFileStoreBean.configuration = binaryFileStoreConfigurationBean;
        binaryFileStoreBean.initializeBinaryFileStore();
        return binaryFileStoreBean;
    }
}
