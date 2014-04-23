/** @file Implementation of a Log module. */
// This module depends on a provided LogCore module, which must
// provide a function __LogCore_log that takes two string arguments
// a level, and an already formatted messag. 
// A simple LogCore module that does logging using Print will suffice, or
// in most cases a LogCore module provided by the environment.
use( "LogCore" );
use( "Underscore" ); // For the 'magic' experimental function.
use( "UnitTest" );
use( "Global" );

EXPORTED_SYMBOLS = [ 'Log' ];

/**
 * Namespace that contains functions to Log with.
 * 
 * This namespace contains functions used for logging. Each function
 * reflects a certain level of urgency of the message to be logged.
 * All logging methods supports passing a variable number of arguments. If the log statement level is enabled
 * each parameter will be converted to a String (by calling its toString method), and the strings will be concatenated.
 *
 * **A note on the levels**  
 * 
 * The log levels are, from low/unimportant to high/important: `trace`,
 * `debug`, `info`, `warn`, `error`, and `fatal`. They are used roughly as such:
 * 
 * * `trace`: Very detailed, function enter/exit, parameter dumps, usually only used for tracing functions, and specific bug solving.
 * 
 * * `debug`: For ordinary development, program flow, dumping important values, etc.
 * 
 * * `info`: Information about the running program/system of a more status-like character, e.g. how far the program is in a given process, that something started or stopped, or similar.
 * 
 * * `warn`: Expectations the program had about flow or data has not been met, but the program is capable of continuing. It might indicate that the user/operator has to look into an issue and solve a problem.
 * 
 * * `error`: An unexpected situation that the program is unable to correctly handle, has occurred. The current process can not run until completion. This might be missing data, or similar. The user/operator must look into the issue, and solve it.
 * 
 * * `fatal`: As error, but the program is unable to continue at all, and will shut down. (Not used much in JavaScript, and not supported in all frameworks).
 * 
 * **A note on performance**
 * 
 * You are encouraged to pass multiple values, instead of 
 * using string concatenation in the call to the logging method,
 *  as the log module will skip the toString conversion process and concatenation, until it determines that it needs
 * to perform the actual logging. See the example.
 * 
 * When logging xml variables for levels that might be often be off (trace, debug, info) you should consider using
 *  {@link XmlUtil.logXMLString}, instead of calling the xml instances toXMLString method. This ensures the best performance. 
 * 
 * @example
// When logging, use multiple parameters that supports toString, instead of string concatenation.
// E.g. do this:
Log.debug( "Value of foo.bar: ", foo.bar, ", value of foo.foo: ", foo.foo );
// Instead of this:
Log.debug( "Value of foo.bar: " + foo.bar + ", value of foo.foo: " + foo.foo );
 * 
 * @namespace
 * @name Log
 */
var Log = function( ) {

    // Store a copy to the function from LogCore - then remove it from the global scope.
    var LogCore_log = __LogCore_log;
    delete this.__LogCore_log;

    // Pointer to function that checks if a given log level is enabled. Optionally supplied by LogCore.
    var LogCore_isLevelEnabled = undefined;

    // The object to return.
    var that = {};    
    
    ////////////////////////////////////////////////////////////////////////////////
    // Helper function.
    // Collect an arguments object to a string. Note, args is *not* an array, but an instance of an arguments object.
    var collect = function( args ) {
        switch( args.length ) {
        case 0: return "";
        case 1: return args[0].toString();
        case 2: return args[0].toString() + args[1].toString();
        default:
            var myres = args[0];
            for ( var i = 1; i < args.length; ++i ) {
                myres += args[ i ].toString( );
            }
            return myres;
        };
    };

    ////////////////////////////////////////////////////////////////////////////////
    // Helper function.
    // Collect an arguments object to a string. Note, args is *not* an array.
    // This version does not throw, but tries to collect as much as possibly
    var safeCollect = function( args ) {
        var myres = "";
        for ( var i = 0; i < args.length; ++i ) {
            try {
                myres += args[ i ].toString( );
            } catch ( e ) {
                myres += "[illegal/null/undefined value at position " + i + "]";
            }
        }
        return myres;
    };

    ////////////////////////////////////////////////////////////////////////////////
    // Actual methods

    ////////////////////////////////////////////////////////////////////////////////
    // Actual methods
    // Collect the args, then log them using the level.
    // Note, args is not an array, but an instance of an arguments object.
    var log = function( level, args ) {
        try {
            LogCore_log( level, collect( args ) );
        } catch ( error ) {
            var msg = "Error while calling formatting log. One or more values passed to log was probably null or undefined. Substituted line follows:\n";
            msg += safeCollect( args ) + "\n";
            if ( "" != error.stack ) {
                msg += "Displaying stack: " + error.stack.replace( /(@[^:\[]*:)/g, "\n$1" );
            }
            LogCore_log( "warn", msg );
            return;
        }
    };


    ////////////////////////////////////////////////////////////
    // Actual user exposed functions/stuff
    
    // Setting up level properties
    /** 
     * Enum for log levels.
     * 
     * Use this enum when calling isLevelEnabled.
     * 
     * This enum has properties `trace`, `debug`, `info`, `warn` and `error`. Use them when calling isLevelEnabled.
     * 
     * @see {@link Log.isLevelEnabled}
     * @example
// Check if info level is enabled, then log.
if ( Log.isLevelEnabled( Log.Level.info ) ) {
    Log.info( "The result of calling slow_function is : ", slow_function() );
}
     * 
     * @readonly
     * @enum {String}
     * @name Log.Level
     */
    that.Level = { 
            /** The trace level */
            trace : "trace", 
            /** The debug level */
            debug : "debug",
            /** The info level */
            info  : "info", 
            /** The warn level */
            warn  : "warn",
            /** The error level */
            error : "error"
    };
    
   /**
    *  Check if a log level is enabled.
    *  
    *  Return true if a log level is enabled, false otherwise.
    *  
    *  This function can be used when logging objects that are expensive to evaluate when passing to the log system.
    *  By only conditionally evaluating, you can make the program go faster, which can be handy in some situations.
    *  
    *  **Note:** You do not need this in most situations, only when you need to call slow functions, or similar, in case of logging. The Log object only calls the underlying Log framework, if it needs to.
    *  
    *  **Note:** This functionality requires the underlying Log framework to support retreiving this kind of information.
    *  
     * @see {@link Log.Level}
    *  @example
// Only perform evaluation, if log level is enabled
if ( Log.isLevelEnabled( Log.Level.debug ) ) {
    Log.debug( "The result of calling slow_function is : ", slow_function() );
}
    *  
    *  @example
// No reason to use in ordinary case
// Don't do this normally
if ( Log.isLevelEnabled( Log.Level.debug ) ) {
    Log.debug( "I need to log the value of something :", some_value );
}
// Instead, do this
Log.debug( "I need to log the value of something :", some_value );
    *
    *  @syntax Log.isLevelEnabled( level )
    *  @param level The level to evaluate. Use the Level property constants.
    *  @return {Boolean} True if the log level is enabled, false otherwise.
    *  
    *  @name Log.isLevelEnabled
    *  @method
    */     
    that.isLevelEnabled = function( level ) {
        return LogCore_isLevelEnabled( level );
    }
    
    
   /**
   * Log a message at `trace` level.
   * 
   * Use the trace function to log very detailed information, such as
   * entering/leaving functions, values of parameters, etc. Most often used
   * as a detailed debug facility for a specific problem 
   * 
   * @syntax Log.trace( arg, ... )
   * @param {...Object} arg The argument(s) to log. Must be convertable to String
   * @name Log.trace
   * @method
   * @example
Log.trace( "Entering function foo, with paramter bar=", bar );
   */
    that.trace = function( ) {
	if ( LogCore_isLevelEnabled( "trace" ) ) {
            log( "trace", arguments );
	};
    };
    /**
   * Log a message at `debug` level.
   * 
   * Use the debug function to log debug information, such as toplevel
   * program flow, important values that influences the program, etc. 
   * 
   * @syntax Log.debug( arg, ... )
   * @param {...Object} arg The argument(s) to log. Must be convertable to String
   * @name Log.debug
   * @method
   * @example
Log.debug( "Service returned the following answer: ", serviceAnswer );
   */
    that.debug = function( ) {
	if ( LogCore_isLevelEnabled( "debug" ) ) {
            log( "debug", arguments );
	};
    };
    /**
   * Log a message at `info` level.
   * 
   * Use the info function to log information of a 'status' like type, such
   * as state changes, how far a given process has progressed, etc. 
   * 
   * @syntax Log.info( arg, ... )
   * @param {...Object} arg The argument(s) to log. Must be convertable to String
   * @name Log.info
   * @method
   * @example
Log.info( "Task with taskId ", taskId, " now done and unloaded" );
   */
    that.info = function( ) {
	if ( LogCore_isLevelEnabled( "info" ) ) {
            log( "info", arguments );
	};
    };
    /**
   * Log a message at `warn` level.
   * 
   * Use the warn function to log warnings about expectations that have not
   * been met, but that the program is still able to work around. The
   * problem should not have a big impact on the process, but a warning
   * should indicate that the user may want to investigate if the warning is
   * a symptom of something that should be corrected. 
   * 
   * @syntax Log.warn( arg, ... )
   * @param {...Object} arg The argument(s) to log. Must be convertable to String
   * @name Log.warn
   * @method
   * @example
Log.warn( "No response from service - will retry in 5 minutes" );
   */
    that.warn = function( ) {
	if ( LogCore_isLevelEnabled( "warn" ) ) {
            log( "warn", arguments );
	};
    };
    /**
   * Log a message at `error` level.
   * 
   * Use the error function to log errors for a given process that the
   * program can not correct. An error indicates that the given task at hand
   * can not be completed. However, other tasks may very well be able to be
   * processed. The user should definitively investigate the reason for the
   * error and if possibly correct it. 
   * 
   * @syntax Log.error( arg, ... )
   * @param {...Object} arg The argument(s) to log. Must be convertable to String
   * @name Log.error
   * @method
   * @example
Log.error( "Error when handling task: tasktype was empty. Task can not be handled" );
   */
    that.error = function( ) {
	if ( LogCore_isLevelEnabled( "error" ) ) {
            log( "error", arguments );
	};
    };

  /**
   * Log a message at `fatal` level.
   * 
   * Use the fatal function to log a fatal problem, that stops the program
   * from being able to proceed at all, or without risk of severe
   * problems/corruption of data. It is probably not a function much used
   * from JavaScript. 
   * 
   * **Note:** Not all log frameworks supports the fatal level. In that case, it will be converted to an error level.
   * 
   * @syntax Log.fatal( arg, ... )
   * @param {...Object} arg The argument(s) to log. Must be convertable to String
   * @name Log.fatal
   * @method
   * @example
Log.fatal( "Configuration error in someobject.somevalue. This option was incorrectly set. Aborting" );
   */
    that.fatal = function( ) {
	if ( LogCore_isLevelEnabled( "fatal" ) ) {
            log( "fatal", arguments );
	};
    };

    /**
     * Log a message at `magic` level.
     * 
     * The magic function is a special log function. It is used to log a
     * function call with the parameters, where the parameters will be
     * 'magically' evaluated before printed. The level is 'magic', and the
     * feature is experimental, and may not make it to production. 
     * 
     * **Note:** This method is experimental.
     *
     * @param {XML} arg The argument to log. Must be XML
     * @name Log.magic
     * @method
     */
    that.magic = function( xml ) {
        // This function checks the contest 
        if ( typeof xml !== "xml" ) {
            log( "warn", "magic called with non xml argument: ", xml );
        }

        function paramToString( ) {
            res = [ ];
            for ( var i = 0; i < arguments.length; ++i ) {
                // Specialcase undefined - if something returns undefined
                if ( arguments[ i ] === undefined ) {
                    res.push( "undefined" );
                    continue;
                }
                // Specialcase arrays, as their toString method is a bit wacky
                if ( _.isArray( arguments[ i ] ) ) {
                    res.push( "[ " + arguments[ i ].toString( ) + " ]" );
                    continue;
                }
                if ( typeof arguments[ i ] === "string" ) {
                    res.push( '"' + arguments[ i ].toString( ) + '"' );
                } else {
                    res.push( arguments[ i ].toString( ) );
                }
            }
            return res.join( ", " );
        }

        content = xml.toString( );
        // Replace upto the first ( with paramToString, eval it, and print it
        // cbo: feel free to replace this with something clever using regexps...
        all = content.split( "(" );
        func = all.shift( );
        replace = "paramToString(" + all.join( "(" );
        newargs = eval( replace );
        complete = func + "( " + newargs + " )";
        log( "magic", [ "Function was called like this:\n", content, "\nAfter parameter evaluating:\n", complete ] );
    };


    // Setup the two "advanced" functions from above
    if ( this.__LogCore_isLevelEnabled ) {
	LogCore_isLevelEnabled = __LogCore_isLevelEnabled;
	delete this.__LogCore_isLevelEnabled;
    } else {
	// Dummy function - all is true
	LogCore_isLevelEnabled = function( level ) { return true; }
        // mbd: 2013-10-31
        // The C++ logging framework does not, and probably will not, support something
        // like isLevelEnabled, and it is also speedy enough to not matter.
        // The check then ends up adding a lot of noise into some applications, such as 
        // esgaroth, where a warning is logged for each JavaScript environment created.
        // So, it is best not to do this check under C++.
        // But, if this is Java, check if the performance could be improved by 
        // adding a "native" __LogCore_isLevelEnabled function.
        // This check is an exception to the rule that we do not check for platforms in non *Core modules.
        // You are not entitled to do this other places. Feel free to complain about this unfairness of life
        // on your favorite social network, or something, as long as you don't expect anyone to care... ;-)
        if ( Global.Packages && Global.Packages.java ) {
            that.warn( "Java interprenter detected, and LogCore module does not export __LogCore_isLevelEnabled - may impact logging performance" );
        } 
    }

    ////////////////////////////////
    // Test the collect function. 
    UnitTest.addFixture( "devel.Log internal", function () {
        var colWrap = function() { return collect( arguments ); };
        Assert.equalValue( "collect, empty arguments", colWrap(), "" );
        Assert.equalValue( "collect, one argument", colWrap( "hej" ), "hej" );
        Assert.equalValue( "collect, two arguments", colWrap( "hej", 42 ), "hej42" );
        Assert.equalValue( "collect, multiple argument", colWrap( "hej", 43, "med", 2/2 ), "hej43med1" );
    });
    
    // Return the Log object
    return that;
}( );

UnitTest.addFixture( "devel.Log module", function( ) {
        // This mostly checks if the log does not throw errors, 
        // see bug 14399
        __Log_ut = {};
        __Log_ut.foo = {}
        __Log_ut.foo.a = "a";
        __Log_ut.foo.foo = null;
        __Log_ut.foo.bar = undefined;

        Assert.equal( "Log.debug with object", 'Log.debug( __Log_ut )', undefined );
        Assert.equal( "Log.debug with value", 'Log.debug( __Log_ut.foo.a )', undefined );
        Assert.equal( "Log.debug with null", 'Log.debug( __Log_ut.foo.foo )', undefined );
        Assert.equal( "Log.debug with undefined", 'Log.debug( __Log_ut.foo.bar )', undefined );

        delete __Log_ut;
    } );
