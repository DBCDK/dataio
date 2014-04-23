// LogCore module - implements a __LogCore_log( level, args ) function

/* Java Maven deps needed:

                <!-- LOGGING -->
                <!-- In order to support different log frameworks, we use a facade pattern 
                        for logging The actual facade pattern is slf4j, which is what dbc-commons 
                        is going to use too. For actual logging in the deployment, we use log4j. -->
                <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-api</artifactId>
                        <version>1.6.1</version>
                </dependency>
                <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-ext</artifactId>
                        <version>1.6.1</version>
                </dependency> 
*/

/* You may also want to actually bind to e.g. log4j:

                <!-- This one binds to log4j in the deployment. -->
                <dependency>
                        <groupId>org.slf4j</groupId>
                        <artifactId>slf4j-log4j12</artifactId>
                        <version>1.6.1</version>
                        <scope>runtime</scope>
                </dependency>
                <!-- In log we trust : http://logging.apache.org/log4j/ -->
                <dependency>
                        <groupId>log4j</groupId>
                        <artifactId>log4j</artifactId>
                        <version>1.2.16</version>
                </dependency>
                <!-- END LOGGING -->
*/


// We export this symbol, which we expect to be used from a Log module.
EXPORTED_SYMBOLS = [ '__LogCore_log', '__LogCore_isLevelEnabled' ];

// Actual log function
var __LogCore_log = function( level, msg ) {
    msg = "JS: " + msg;
    // Rhino specific bindings utilized.
    switch( level ) {
    case "trace" : __LogCore_log.logger.trace( msg ); break;
    case "debug" : __LogCore_log.logger.debug( msg ); break;
    case "info"  : __LogCore_log.logger.info( msg ); break;
    case "warn"  : __LogCore_log.logger.warn( msg ); break;
    case "error" : __LogCore_log.logger.error( msg ); break;
    case "fatal" : __LogCore_log.logger.error( "FATAL: " + msg ); break;
    default      : __LogCore_log.logger.error( "UNKNOWN LOGLEVEL: " + level + " : " + msg ); break;
    }
};

// Check level export
__LogCore_isLevelEnabled = function ( level ) {
    switch ( level ) {
    case "trace" : return __LogCore_log.logger.isTraceEnabled();
    case "debug" : return __LogCore_log.logger.isDebugEnabled();
    case "info"  : return __LogCore_log.logger.isInfoEnabled();
    case "warn"  : return __LogCore_log.logger.isWarnEnabled();
    case "error" : return __LogCore_log.logger.isErrorEnabled();
    default: return true;
    };
}


// Attach the logger variable to the global logger only once, not for each
// function call.
// Sort of the static way to do it.

// The next line can bind *directly* to log4j. It is only included for
// reference, as
// the facade below is the recommended way to do it.
// __LogCore_log.logger = Packages.org.apache.log4j.Logger.getLogger(
// "JavaScript.Logger" );

// Binding to the slf4j facade.
__LogCore_log.logger = Packages.org.slf4j.LoggerFactory
    .getLogger( "JavaScript.Logger" );
