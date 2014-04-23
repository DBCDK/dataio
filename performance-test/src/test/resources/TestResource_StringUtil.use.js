/** @file String utilities */
EXPORTED_SYMBOLS = [ 'StringUtil' ];

/* Sprintf implementation lifted from http://www.webtoolkit.info/javascript-sprintf.html
 * No license indicated - assuming public domain

 * Modifed to be a module, then linted, UnitTests added.

Javascript sprintf
Several programming languages implement a sprintf function, to output a formatted string. It originated from the C programming language, printf function. Its a string manipulation function.
This is limited sprintf Javascript implementation. Function returns a string formatted by the usual printf conventions. See below for more details. You must specify the string and how to format the variables in it. Possible format values:
 %% - Returns a percent sign
 %b - Binary number
 %c - The character according to the ASCII value
 %d - Signed decimal number
 %f - Floating-point number
 %o - Octal number
 %s - String
 %x - Hexadecimal number (lowercase letters)
 %X - Hexadecimal number (uppercase letters)
Additional format values. These are placed between the % and the letter (example %.2f):

 
 + (Forces both + and - in front of numbers. By default, only negative numbers are marked)
 - (Left-justifies the variable value)
 0 zero will be used for padding the results to the right string size
 [0-9] (Specifies the minimum width held of to the variable value)
 .[0-9] (Specifies the number of decimal digits or maximum string length)


*/

use( "UnitTest" );

/**
 * Various methods to manipulate strings.
 * 
 * This namespace contain(s) methods that provide some string manipulation
 * methods, that are not provided by the JavaScript String class, such as
 * sprintf. 
 *
 * @type {namespace}
 * @name StringUtil
 * @namespace */
var StringUtil = function( ) {
    var that = {};

    var convert = function( match, nosign ) {
        if ( nosign ) {
            match.sign = '';
        } else {
            match.sign = match.negative ? '-' : match.sign;
        }
        var l = match.min - match.argument.length + 1 - match.sign.length;
        var pad;
        if ( l < 0 ) {
            pad = [ ];
        } else {
            pad = new Array( l );
        }
        pad = pad.join( match.pad );
        if ( !match.left ) {
            if ( match.pad == "0" || nosign ) {
                return match.sign + pad + match.argument;
            } else {
                return pad + match.sign + match.argument;
            }
        } else {
            if ( match.pad == "0" || nosign ) {
                return match.sign + match.argument + pad.replace( /0/g, ' ' );
            } else {
                return match.sign + match.argument + pad;
            }
        }
    };


    /**
     * Format string with arguments.
     * 
     * Implementation of sprintf for JavaScript. Allows to format a string,
     * based on a 'template', and a number of arguments. 
     *
     * Possible format values:
     *
     * * %% : Returns a percent sign
     * * %b : Binary number
     * * %c : The character according to the ASCII value
     * * %d : Signed decimal number
     * * %f : Floating-point number
     * * %o : Octal number
     * * %s : String
     * * %x : Hexadecimal number (lowercase letters)
     * * %X : Hexadecimal number (uppercase letters)
     *
     * Additional format values. These are placed between the % and the letter (example %.2f):
     *
     * * + : Forces both + and - in front of numbers. By default, only negative numbers are marked
     * * - : Left-justifies the variable value
     * * 0 : zero will be used for padding the results to the right string size
     * * [0-9] : Specifies the minimum width held of to the variable value
     * * .[0-9] : Specifies the number of decimal digits or maximum string length
     *
     * Format values can be repeated and mixed with normal text. Each format value will consume one additional argument.
     *
     * **Note:** This method is slow. Use it with caution.
     * 
     * @type {function}
     * @param {String} format Formatting string, such as "index %i is %s"
     * @param {...object} value One or more values to use in the string format template. This will be converted to a string according to the specifics of the template.
     * @return {String} A string formatted according to the format specification.
     * @example 
// Using the sprintf function in a JavaScript shell.
js> print( StringUtil.sprintf( "Database '%-10s': Number of records in file: %08d. Average size: %.2f liters", "ABCD", 1043, 34.456 ) );
Database 'ABCD      ': Number of records in file: 00001043. Average size: 34.46 liters
     * @name StringUtil.sprintf
     * @method
     */
    that.sprintf = function( ) {

        if ( typeof arguments == "undefined" ) {
            return null;
        }
        if ( arguments.length < 1 ) {
            return null;
        }
        if ( typeof arguments[ 0 ] != "string" ) {
            return null;
        }
        if ( typeof RegExp == "undefined" ) {
            return null;
        }

        var string = arguments[ 0 ];
        var exp = new RegExp( /(%([%]|(\-)?(\+|\x20)?(0)?(\d+)?(\.(\d)?)?([bcdfosxX])))/g );
        var matches = [ ];
        var strings = [ ];
        var convCount = 0;
        var stringPosStart = 0;
        var stringPosEnd = 0;
        var matchPosEnd = 0;
        var newString = '';
        var match = exp.exec( string );
        var substitution = undefined;
        while ( match ) {
            if ( match[ 9 ] ) {
                convCount += 1;
            }

            stringPosStart = matchPosEnd;
            stringPosEnd = exp.lastIndex - match[ 0 ].length;
            strings[ strings.length ] = string.substring( stringPosStart, stringPosEnd );

            matchPosEnd = exp.lastIndex;
            matches[ matches.length ] = {
                match: match[ 0 ],
                left: match[ 3 ] ? true : false,
                sign: match[ 4 ] || '',
                pad: match[ 5 ] || ' ',
                min: match[ 6 ] || 0,
                precision: match[ 8 ],
                code: match[ 9 ] || '%',
                negative: parseInt( arguments[ convCount ], 10 ) < 0 ? true : false,
                argument: String( arguments[ convCount ] )
            };
            match = exp.exec( string );
        }
        strings[ strings.length ] = string.substring( matchPosEnd );

        if ( matches.length === 0 ) {
            return string;
        }
        if ( ( arguments.length - 1 ) < convCount ) {
            return null;
        }

        var code = null;
        match = null;
        var i = null;

        for ( i = 0; i < matches.length; i++ ) {

            if ( matches[ i ].code == '%' ) {
                substitution = '%';
            } else if ( matches[ i ].code == 'b' ) {
                matches[ i ].argument = String( Math.abs( parseInt( matches[ i ].argument, 10 ) ).toString( 2 ) );
                substitution = convert( matches[ i ], true );
            } else if ( matches[ i ].code == 'c' ) {
                matches[ i ].argument = String( String.fromCharCode( parseInt( Math.abs( parseInt( matches[ i ].argument, 10 ) ), 10 ) ) );
                substitution = convert( matches[ i ], true );
            } else if ( matches[ i ].code == 'd' ) {
                matches[ i ].argument = String( Math.abs( parseInt( matches[ i ].argument, 10 ) ) );
                substitution = convert( matches[ i ] );
            } else if ( matches[ i ].code == 'f' ) {
                matches[ i ].argument = String( Math.abs( parseFloat( matches[ i ].argument ) ).toFixed( matches[ i ].precision ? matches[ i ].precision : 6 ) );
                substitution = convert( matches[ i ] );
            } else if ( matches[ i ].code == 'o' ) {
                matches[ i ].argument = String( Math.abs( parseInt( matches[ i ].argument, 10 ) ).toString( 8 ) );
                substitution = convert( matches[ i ] );
            } else if ( matches[ i ].code == 's' ) {
                matches[ i ].argument = matches[ i ].argument.substring( 0, matches[ i ].precision ? matches[ i ].precision : matches[ i ].argument.length );
                substitution = convert( matches[ i ], true );
            } else if ( matches[ i ].code == 'x' ) {
                matches[ i ].argument = String( Math.abs( parseInt( matches[ i ].argument, 10 ) ).toString( 16 ) );
                substitution = convert( matches[ i ] );
            } else if ( matches[ i ].code == 'X' ) {
                matches[ i ].argument = String( Math.abs( parseInt( matches[ i ].argument, 10 ) ).toString( 16 ) );
                substitution = convert( matches[ i ] ).toUpperCase( );
            } else {
                substitution = matches[ i ].match;
            }

            newString += strings[ i ];
            newString += substitution;

        }
        newString += strings[ i ];

        return newString;

    };

   /**
     * Return the number of octets of a unicode char
     * 
     * @syntax utf8LengthOfChar( UnicodeChar )
     * @param {Integer} UnicodeChar Codepoint for char
     * @return {Integer} octet length of Unicode Char in UTF-8
     *
     * Internal Helper Function
     *
     *
     * code Based on 'man UTF-8'
     *        0x00000000 - 0x0000007F:
     0xxxxxxx
     0x00000080 - 0x000007FF:
     110xxxxx 10xxxxxx
     0x00000800 - 0x0000FFFF:
     1110xxxx 10xxxxxx 10xxxxxx
     0x00010000 - 0x001FFFFF:
     11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     0x00200000 - 0x03FFFFFF:
     111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
     0x04000000 - 0x7FFFFFFF:
     1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx

     */
    var utf8LengthOfChar = function( UnicodeChar ) {
        if ( UnicodeChar <= 0x7F ) return 1
        if ( UnicodeChar <= 0x77F ) return 2;
        if ( UnicodeChar <= 0xFFFF ) return 3;
        if ( UnicodeChar <= 0x001FFFFF ) return 4;
        if ( UnicodeChar <= 0x03FFFFFF ) return 5;
        //if( UnicodeChar <= 0x7FFFFFFF )
        return 6;

    };
    // Internal tests of utf8LengthOfChar
    UnitTest.addFixture( "util.StringUtil module utf8LengthOfChar", function( ) {

            Assert.equal( "utf8LengthOfChar a", utf8LengthOfChar( 'a'.charCodeAt( 0 ) ), 1 );
            Assert.equal( "utf8LengthOfChar U+007f", utf8LengthOfChar( '\u007f'.charCodeAt( 0 ) ), 1 );
            Assert.equal( "utf8LengthOfChar U+0080", utf8LengthOfChar( '\u0080'.charCodeAt( 0 ) ), 2 );
            Assert.equal( "utf8LengthOfChar U+0800", utf8LengthOfChar( '\u0800'.charCodeAt( 0 ) ), 3 );
            Assert.equal( "utf8LengthOfChar U+FFFF", utf8LengthOfChar( '\uFFFF'.charCodeAt( 0 ) ), 3 );

            // extra test for characters of spceial interest.
            Assert.equal( "utf8LengthOfChar \u00E6", utf8LengthOfChar( '\u00C6'.charCodeAt( 0 ) ), 2 );
            Assert.equal( "utf8LengthOfChar \u00F8", utf8LengthOfChar( '\u00f8'.charCodeAt( 0 ) ), 2 );
            Assert.equal( "utf8LengthOfChar \u00E5", utf8LengthOfChar( '\u00E5'.charCodeAt( 0 ) ), 2 );
    } );

    /**
     * Return the number of octets of a string, when encoded as UTF8.
     * 
     * This method is used, if you wish to know the lenght of a string
     * in octets/bytes, if the string is encoded in UTF8. This is
     * mostly relevant if you need to produce encoded files from
     * within JavaScript, which is the case for e.g. "addi" files.
     *
     * @syntax utf8LengthOfString( UnicodeString )
     * @param {String} UnicodeString A standard javastring
     * @return {Integer} Octet length of UTF8 encoding of String
     * @example
// This call will return 2:
StringUtil.utf8LengthOfChar( "\u00E6" ); // Danish letter ae
// There is a difference between the number of characters, and the utf8 length:
var test = "Delfelt \u00E6 er her"; // Still Danish letter ae
test.length; // Returns 16
StringUtil.utf8LengthOfString( test ); // Returns 17
     * @name StringUtil.utf8LengthOfString
     * @method
     */
    that.utf8LengthOfString = function( UnicodeString ) {
        var length = 0;
        var i = 0;
        var n = UnicodeString.length;

        for ( ; i < n; ++i ) {
            length += utf8LengthOfChar( UnicodeString.charCodeAt( i ) );
        }
        return length;
    };

    return that;
}( );


UnitTest.addFixture( "util.StringUtil module", function( ) {
        // Lots of tests are lacking...

        // Does it work at all
        Assert.equal( "sprintf 1", 'StringUtil.sprintf( "Hello World!" )', "Hello World!" );

        // Floats
        Assert.equal( "sprintf float 1", 'StringUtil.sprintf( "%4.2f", 42.456 )', "42.46" );
        Assert.equal( "sprintf float 2", 'StringUtil.sprintf( "%1.2f", 42.456 )', "42.46" );
        Assert.equal( "sprintf float 3", 'StringUtil.sprintf( "%1.8f", 42.456 )', "42.45600000" );
        Assert.equal( "sprintf float 4", 'StringUtil.sprintf( "%8.8f", 42.456789 )', "42.45678900" );
        Assert.equal( "sprintf float 5", 'StringUtil.sprintf( "%8.4f", 42.45 )', " 42.4500" );
        Assert.equal( "sprintf float 6", 'StringUtil.sprintf( "%10.4f", 42 )', "   42.0000" );
        Assert.equal( "sprintf float 7", 'StringUtil.sprintf( "%20.9f", 42 )', "        42.000000000" );
        Assert.equal( "sprintf float 8", 'StringUtil.sprintf( "%+8.2f", 42.454 )', "  +42.45" );
        Assert.equal( "sprintf float 9", 'StringUtil.sprintf( "%+8.2f", -42.45 )', "  -42.45" );
        Assert.equal( "sprintf float 10", 'StringUtil.sprintf( "%+08.2f", -42.45 )', "-0042.45" );

        // Decimal
        Assert.equal( "sprintf decimal 1", 'StringUtil.sprintf( "%4d", 42 )', "  42" );
        Assert.equal( "sprintf decimal 2", 'StringUtil.sprintf( "%1d", 42 )', "42" );
        Assert.equal( "sprintf decimal 3", 'StringUtil.sprintf( "%2d", 42 )', "42" );
        Assert.equal( "sprintf decimal 4", 'StringUtil.sprintf( "%3d", 42 )', " 42" );
        Assert.equal( "sprintf decimal 5", 'StringUtil.sprintf( "%8d", 42 )', "      42" );
        Assert.equal( "sprintf decimal 6", 'StringUtil.sprintf( "%-10d", 42 )', "42        " );
        Assert.equal( "sprintf decimal 7", 'StringUtil.sprintf( "%20d", 42 )', "                  42" );
        Assert.equal( "sprintf decimal 8", 'StringUtil.sprintf( "%-+8d", 42 )', "+42     " );
        Assert.equal( "sprintf decimal 9", 'StringUtil.sprintf( "%+8d", -42 )', "     -42" );
        Assert.equal( "sprintf decimal 10", 'StringUtil.sprintf( "%+08d", -42 )', "-0000042" );

        // Hexadecimal
        Assert.equal( "sprintf hex 1", 'StringUtil.sprintf( "%x", 42 )', "2a" );
        Assert.equal( "sprintf hex 2", 'StringUtil.sprintf( "%X", 42 )', "2A" );
        Assert.equal( "sprintf hex 3", 'StringUtil.sprintf( "%x", 420 )', "1a4" );
        Assert.equal( "sprintf hex 5", 'StringUtil.sprintf( "%X", 42042 )', "A43A" );
        Assert.equal( "sprintf hex 6", 'StringUtil.sprintf( "%X", 420424 )', "66A48" );
        Assert.equal( "sprintf hex 7", 'StringUtil.sprintf( "%4x", 42 )', "  2a" );
        Assert.equal( "sprintf hex 8", 'StringUtil.sprintf( "%6X", 42 )', "    2A" );
        Assert.equal( "sprintf hex 9", 'StringUtil.sprintf( "%08x", 420 )', "000001a4" );
        Assert.equal( "sprintf hex 10", 'StringUtil.sprintf( "%08X", 42042 )', "0000A43A" );
        Assert.equal( "sprintf hex 11", 'StringUtil.sprintf( "%8X", 420424 )', "   66A48" );

        // Binary
        Assert.equal( "sprintf binary 1", 'StringUtil.sprintf( "%b", 42 )', "101010" );
        Assert.equal( "sprintf binary 2", 'StringUtil.sprintf( "%b", 4242 )', "1000010010010" );
        Assert.equal( "sprintf binary 3", 'StringUtil.sprintf( "%8b", 42 )', "  101010" );
        Assert.equal( "sprintf binary 4", 'StringUtil.sprintf( "%16b", 4242 )', "   1000010010010" );
        Assert.equal( "sprintf binary 5", 'StringUtil.sprintf( "%08b", 42 )', "00101010" );
        Assert.equal( "sprintf binary 6", 'StringUtil.sprintf( "%016b", 4242 )', "0001000010010010" );

        // String tests
        Assert.equal( "sprintf string 1", 'StringUtil.sprintf( "%s", "Hello World!" )', "Hello World!" );
        Assert.equal( "sprintf string 2", 'StringUtil.sprintf( "%20s", "Hello World!" )', "        Hello World!" );
        Assert.equal( "sprintf string 3", 'StringUtil.sprintf( "%-20s", "Hello World!" )', "Hello World!        " );
        Assert.equal( "sprintf string 4", 'StringUtil.sprintf( "%4s", "Hello" )', "Hello" );
        Assert.equal( "sprintf string 5", 'StringUtil.sprintf( "%5s", "Hello" )', "Hello" );
        Assert.equal( "sprintf string 6", 'StringUtil.sprintf( "%6s", "Hello" )', " Hello" );
        Assert.equal( "sprintf string 7", 'StringUtil.sprintf( "%-4s", "Hello" )', "Hello" );
        Assert.equal( "sprintf string 8", 'StringUtil.sprintf( "%-5s", "Hello" )', "Hello" );
        Assert.equal( "sprintf string 9", 'StringUtil.sprintf( "%-6s", "Hello" )', "Hello " );
        Assert.equal( "sprintf string 10", 'StringUtil.sprintf( "%4s", "" )', "    " );
        Assert.equal( "sprintf string 11", 'StringUtil.sprintf( "%5s", "" )', "     " );
        Assert.equal( "sprintf string 12", 'StringUtil.sprintf( "%6s", "" )', "      " );

        // Combined
        Assert.equal( "sprintf combined 1", 'StringUtil.sprintf( "%20s %4.2f %04x %014b", "Hello World", 42.456, 42, 4242 )', "         Hello World 42.46 002a 01000010010010" );

    } );



    UnitTest.addFixture( "util.StringUtil module utf8LengthOfString", function( ) {
            Assert.equal( "utf8LengthOfString abc", StringUtil.utf8LengthOfString( 'abc' ), 3 );
            Assert.equal( "utf8LengthOfString \u00E6", StringUtil.utf8LengthOfString( '\u00E6' ), 2 );

        } );    
    
