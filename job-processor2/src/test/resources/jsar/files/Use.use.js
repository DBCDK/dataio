/** @file Core use functionality */
// WARNING: If you break this file, the module system will stop working!

// This file defines the function `use(module-name, version-number)`,
// which is used for loading modules.
// The module-name is resolved, and the module is loaded,
// if not already available.
// The version-number is optional and indicates which version of
// the module that is required, and used to check if the module
// is compatible with the code.
//
// A module must define a global variable `EXPORTED_SYMBOLS`
// which lists the global variable it exports.
// The optional global variable `IGNORED_SYMBOLS` can be added,
// which suppresses warnings if those symbols are written to.
// It also has the option of adding a three-part string version number
// a la: "1.4.57" in a `VERSION` variable which can be checked for.
//
// It also ensures that a given module is only loaded once,
// and makes some checks on version numbers and whether
// global variables are written
//
// Resolution of module name is currently done in
// the native embedding code.
//
// jslib depends on, and loads this module.

use = ( function( ) {

        // this object contains all variables exported from modules
        // and is static, via closure
        var exports = {};
        return function( name, version ) {
            var elem, i;

            // symbols that are ignored when checking for
            // module-writes to the global scope
            var ignore = {
                'EXPORTED_SYMBOLS': true,
                'IGNORED_SYMBOLS': true,
                'VERSION': true
            };

            // global scope
            var globals = ( function( ) {
                    return this;
                } )( );

            // quit if module is already loaded
            if ( globals.hasOwnProperty( '__ModulesInfo' ) && __ModulesInfo.checkDepAlreadyLoaded( name ) ) {
                return;
            }

            // shallow copy of global scope, used to check for writes
            var oldGlobals = {};
            for ( elem in globals ) {
                if ( globals.hasOwnProperty( elem ) ) {
                    oldGlobals[ elem ] = globals[ elem ];
                }
            }

            // name resolution is done in the native function `__builtin_use`
            //
            // For the SpiderMonkey implementation, we want to rethrow possible exception
            // to get file name etc. with error as the native error doesn't include that.
            // We do not have a good way to determine one or the other, this is kind of
            // hackish: rely on Packages.java - if present and object, this is Rhino/Java backend
            // If not, this is spidermonkey
            // Note: This module can not rely on any other modules, as it is
            // the loader module.... capish?
            if( typeof( __environment ) === "object" ) {
                // JSLib with Nashorn
                __environment.use( name );
            } else if ( ( typeof Packages === "object" ) && ( typeof Packages.java === "object" ) ) {
                // Rhino/Java path
                __builtin_use( name );
            } else {
                // SpiderMonkey/C/C++ path
                try {
                    __builtin_use( name );
                } catch ( e ) {
                    throw e;
                }
            }
            // modules must initialise an EXPORTED_SYMBOLS variable.
            // This is inspirede by mozilla's module system, see also
            // https://developer.mozilla.org/en/JavaScript_code_modules/Using
            //
            // this code throws if the variable is not defined
            // or if it is not of the proper type.
            if ( !globals.hasOwnProperty( 'EXPORTED_SYMBOLS' ) || typeof EXPORTED_SYMBOLS !== 'object' || EXPORTED_SYMBOLS.constructor !== Array ) {
                throw 'Use error: module "' + name + '" does not initialise array EXPORTED_SYMBOLS.';
            }


            // version of the module
            globals.VERSION = globals.VERSION || "0.0.0"


            // if `use(...)` requires a certain version,
            // test if the module supplies that
            // or otherwise throw an error
            if ( version ) {
                var versionerror = "Requesting version " + uneval( version ) + " of module " + name + " which is version " + uneval( VERSION );

                function splitversion( v ) {
                    var result = ( typeof( v ) === "string" ) && v.split( "." ).map( function( s ) {
                            return parseInt( s, 10 )
                        } );
                    if ( result.constructor !== Array || result.length !== 3 || result.join( "." ) !== v ) {
                        throw versionerror;
                    }
                    return result;
                }
                var moduleversion = splitversion( VERSION );
                version = splitversion( version );
                if ( !( moduleversion[ 0 ] == version[ 0 ] && ( moduleversion[ 1 ] > version[ 1 ] || ( moduleversion[ 1 ] === version[ 1 ] && moduleversion[ 2 ] >= version[ 2 ] ) ) ) ) {
                    throw versionerror;
                }
            }
            /* TODO: seems like we dont check version number
           if module is already loaded... */
            __ModulesInfo.setVersion( name, VERSION );

            // mbd: Add symbols to ModulesInfo
            __ModulesInfo.addSymbols( name, EXPORTED_SYMBOLS );

            // mbd: I wonder why this is needed?
            // rje: When loading a module, we create (or change) the global variable __ModulesInfo
            // rje: and thus it needs to exported, or we would get a warning from the module system.
            // rje: Could be handled as a special case in the code instead, but I find it simpler
            // rje: just to add it to the exported symbols.
            EXPORTED_SYMBOLS.push( '__ModulesInfo' );

            // append `IGNORED_SYMBOLS` to `ignore` list
            if ( globals.hasOwnProperty( 'IGNORED_SYMBOLS' ) ) {
                for ( i = 0; i < IGNORED_SYMBOLS.length; ++i ) {
                    elem = IGNORED_SYMBOLS[ i ];
                    ignore[ elem ] = true;
                }
            }

            // warn if `EXPORTED_SYMBOLS` is not exported
            for ( i = 0; i < EXPORTED_SYMBOLS.length; ++i ) {
                elem = EXPORTED_SYMBOLS[ i ];
                if ( globals.hasOwnProperty( 'print' ) && !globals.hasOwnProperty( elem ) ) {
                    print( 'Warning: module "' + name + '" says it exports global "' + elem + '" which does not exist\n' );
                }
                exports[ elem ] = globals[ elem ];
            }

            // warn if module creates or changes global symbol
            // which is not in `IGNORED_SYMBOLS` or `EXPORTED_SYMBOLS`
            // and delete them if created
            for ( elem in globals ) {
                if ( globals.hasOwnProperty( elem ) ) {

                    if ( oldGlobals.hasOwnProperty( elem ) && oldGlobals[ elem ] !== globals[ elem ] ) {
                        if ( globals.hasOwnProperty( 'print' ) && !ignore[ elem ] ) {
                            print( 'Warning: module "' + name + '" overwrites global variable ' + elem + '\n' );
                        }
                    }

                    if ( !exports.hasOwnProperty( elem ) ) {
                        if ( oldGlobals.hasOwnProperty( elem ) ) {
                            globals[ elem ] = oldGlobals[ elem ];
                        } else if ( globals.hasOwnProperty( 'print' ) && !ignore[ elem ] ) {
                            print( 'Warning: module "' + name + '" creates non-exported global variable ' + elem + '\n' );
                            if ( !delete globals[ elem ] ) {
                                globals[ elem ] = undefined;
                            }
                        }
                    }
                }
            }
        }
    } )( );

// Modules to be loaded just after initialization of module system
use( "Use.RequiredModules" );
