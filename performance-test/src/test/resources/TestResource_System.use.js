/** @file System module. Provides System class, that can be populated by other modules. */
EXPORTED_SYMBOLS = [ 'System' ];

/**
 * Provides a namespace to add lowlevel/system like functionality to.
 * 
 * Methods and modules related to low-level stuff is added into this
 * namespace. 
 *
 * To actually use most of the functions in this namespace, you need
 * to load additional modules. E.g. to be able to use the 
 * {@link System.readFile} function, you must use the module `ReadFile`. to
 * use the {@link System.print} function you must use the module
 * `Print`. For all functions, it is indicated which module they are
 * part of, that is, which module you should use to get access to the
 * function.
 * 
 * @namespace 
 * @name System */
var System = function( ) {
    var that = {};

    /**
     * Array containing arguments passed to the system, if invoked interactively.
     * 
     * Contains any arguments passed to the system, if used interactively. Is
     * an empty array if not. 
     *
     * @example
// Get the first argument to a script when called in a shell
use( "System" );
if ( System.arguments.length > 0 ) {
  var firstArg = System.arguments[0];
}
     *
     * @name System.arguments
     * @type {String[]}
     * @property */
    that.arguments = [ ];

    /** 
     * String containing the name of the called script, if invoked interactively.
     * 
     * @example
// Print the name of the calling script
use( "System" );
use( "Print" );
print( System.scriptname + "\n" );
     * 
     * @name System.scriptname
     * @type {String}
     * @property */
    that.scriptname = "";

    /** Property to define the Rhino platform.
     * @name System.platform_rhino
     * @type {String}
     * @property */
    that.platform_rhino = "Rhino";

    /** Property to define the SpiderMonkey platform.
     * @name System.platform_spidermonkey
     * @type {String}
     * @property */
    that.platform_spidermonkey = "SpiderMonkey";

    /**
     * Get the name of the platform we are currently running under.
     *
     * This returns the name of our platform, one of "Rhino" or "SpiderMonkey".
     * This method is meant for the very rare situations where we have to do
     * something slightly different depending on platform, in order to 
     * work around differences.
     *
     * Use the properties platform_rhino and platform_spidermonkey to
     * check against this property.
     *
     * **Note:** More platforms may be added in the future. Always
     * check explicitly for a specific platform.

     * **Note:** ONLY USE THIS METHOD AS A LAST RESORT. The platforms behave almost 
     * exactly the same, and the code should not be sprinkled with "if platform then".
     * It is suspected this is only needed for E4X code that is broken anyway.
     * If you use this function, at least wrap it in a library/module such that
     * you can change the implementation later, without breaking the interface.
     *  
     * @name System.platform
     * @type {String}
     * @property */
    that.platform  = function() {
        if ( ( typeof Packages === "object" ) && ( typeof Packages.java === "object" ) ) {
            return "Rhino";
        } else {
            return "SpiderMonkey";
        }
    }(); // Note, not a function, a value.


    return that;
}( );
