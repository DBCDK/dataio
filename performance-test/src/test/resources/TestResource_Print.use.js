/** @file Print module */

use( "System" );
// PrintCore adds __PrintCore_print, which we move to system, then links to the global symbol print
use( "PrintCore" );
/**
 * Output the arguments as string.
 * 
 * Outputs the arguments as string in an application specific manner. By
 * default to stdout 
 * 
 * This function can be accessed by loading the module `Print`. For
 * interactive use, this function is also exported as a global symbol
 * `print`.
 *
 * **Note:** Usually application environments provide specific
 * input/output methods/ways, and System.print is mostly used in
 * console/shell applications.
 *
 * **Note:** This method has a number of methods as properties. They
 * can not be documented directly, but is documented through the
 * pseudo namespace {@link System.Print}.
 *
 * @example
// Print the string "Hello World!" to default output, followed by a newline.
System.print( "Hello World!\n" );
 *
 * @see System.Print
 * @syntax System.print( arg, ... )
 * @param {...Object} arg The argument(s) to output. Must be able to be converted to a string, can be repeated
 * @name System.print
 * @method
 */
System.print = __PrintCore_print;
print = __PrintCore_print;
delete this.__PrintCore_print;

// Also provide a printf funktion.
use( "StringUtil" );

/**
 * Pseudo module for print functions.
 *
 * Due to limitations in JSDoc, we can not document functions that are
 * attributes to non-constructor methods. {@link System.print} is one
 * such method. To be able to document these methods, this pseudo
 * namespace is used. All methods in this namespace are really
 * properties on {@link System.print} and you access them by calling
 * e.g. `System.print.formatted`.
 * 
 * @borrows System.print as System.print
 * @see System.print
 * @name System.Print
 * @namespace */


/**
 * Prints a formatted string.
 * 
 * This method takes a format string, and a number of arguments. It
 * then expand the format string, using the arguments, and finally
 * prints the string. 
 *
 * The format string and use of arguments is described in the
 * documentation for {@link StringUtil.sprintf}.
 *
 * This function can be accessed by loading the module `Print`.
 *
 * @example
// Prints:
// Total     :  42.00
System.print.formatted( "%-10s:%7.2f\n", "Total", 42 );
 * @see StringUtil.sprintf
 * @see System.print
 * @syntax System.print.formatted( format, arg, ... );
 * @param {String} format Format string for the output
 * @param {...Object} arg One or more arguments to use in the format string.
 * @name System.print.formatted 
 * @memberOf System.Print
 * @method */
System.print.formatted = function( ) {
    var res = StringUtil.sprintf.apply( this, arguments );
    System.print( res );
}

// Extend print to provide a fixed width print function that 
// prints the arguments prefixed with a number of spaces to 
// fit the width, which is passed as the first argument
/**
 * Prints with fixed width, prefixing the string with spaces.
 * 
 * Concatenates all args except width and prefixs with spaces until string
 * has length width, then prints the string. If result string is longer
 * than width, no spaces are prefixed, but whole string is printed. 
 *
 * This function can be accessed by loading the module `Print`.
 *
 * @deprecated Prefer to use {@link StringUtils.sprintf} instead of
 * this method to format strings when outputting, or use {@link
 * System.print.formatted}.
 * 
 * @see StringUtils.sprintf
 * @see System.print.formatted
 * @see System.print
 * @syntax print.fixed( width, arg, ... )
 * @param {Number} width The total width of the printed string
 * @param {...Object} arg Objects to print. Must support toString.
 * @name System.print.fixed
 * @memberOf System.Print
 * @method
 */
System.print.fixed = function( ) {
    if ( arguments.length === 0 ) {
        return;
    }
    var width = arguments[ 0 ];
    var str = "";
    for ( var i = 1; i < arguments.length; ++i ) {
        str += arguments[ i ];
    }
    var prefixl = width - str.length;
    var prefix = "          ";
    while ( prefix.length < prefixl ) {
        prefix += prefix;
    }
    if ( prefixl < 0 ) {
        prefixl = 0;
    }
    print( prefix.slice( 0, prefixl ), str );
};

// Extend print to provide a fixed width print function that 
// prints the arguments postfixed with a number of spaces to 
// fit the width, which is passed as the first argument
/**
 * Prints with fixed width, postfixing the string with spaces.
 * 
 * Concatenates all args except width and postfixs with spaces until
 * string has length width, then prints the string. If result string is
 * longer than width, no spaces are postfixed, but whole string is
 * printed. 
 *
 * This function can be accessed by loading the module `Print`.
 *
 * @deprecated Prefer to use {@link StringUtils.sprintf} instead of
 * this method to format strings when outputting, or use {@link
 * System.print.formatted}.
 * 
 * @see StringUtils.sprintf
 * @see System.print.formatted
 * @see System.print
 * @syntax print.fixedl( width, arg, ... )
 * @param {Number} width The total width of the printed string
 * @param {...Object} arg Objects to print. Must support toString.
 * @name System.print.fixedl
 * @memberOf System.Print
 * @method */
System.print.fixedl = function( ) {
    if ( arguments.length === 0 ) {
        return;
    }
    var width = arguments[ 0 ];
    var str = "";
    for ( var i = 1; i < arguments.length; ++i ) {
        str += arguments[ i ];
    }
    var prefixl = width - str.length;
    var prefix = "          ";
    while ( prefix.length < prefixl ) {
        prefix += prefix;
    }
    if ( prefixl < 0 ) {
        prefixl = 0;
    }
    print( str, prefix.slice( 0, prefixl ) );
};

/**
 * As {@link System.print}, but adds a newline at the end.
 * 
 * Concatenates all args then prints the string followed by a newline 
 * 
 * This function can be accessed by loading the module `Print`. For
 * interactive use, this function is also exported as a global symbol
 * `printn`.
 *
 * @syntax System.printn( arg, ... )
 * @param arg The arguments to print
 * @name System.print.printn
 * @memberOf System.Print
 * @method */
System.print.printn = function( ) {
    System.print.apply( this, arguments );
    System.print( "\n" );
};
printn = System.print.printn;

EXPORTED_SYMBOLS = [ 'print', 'printn' ];
