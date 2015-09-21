/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @file File that provides a module to handle some small conversion
 * issues with XML strings and objects and other utilities. */

use( "UnitTest" );
use( "Log" );
use( "ValueCheck" );
EXPORTED_SYMBOLS = [ "XmlUtil" ];
/**
 * Utility methods for XML handling.
 * 
 * Various utility methods for XML handling. Some mostly to ensure same
 * treatment of XML code across various implementations. 
 * 
 * @type {namespace}
 * @namespace 
 * @name XmlUtil
 */
var XmlUtil = function( ) {

    // Object that eventually gets exported
    var that = {};

    /**
     * Create an XML object from a string.  
     * 
     * This function safely creates an XML object from a string. If a
     * preprocessing directive is present, it removes it, likewise for
     * DOCTYPE, whitespace before and after, and so on. An error will
     * be thrown, if the resulting string, after stripping whitespace,
     * etc. is not wellformed XML.
     *
     * If you call this method with an XML object, it will log a
     * warning. You should consider this a fault in the calling code.
     *
     * There is no reason to call this method when constructing small
     * XML snippets. It is meant to be called with XML strings that
     * typically is passed in to the system from outside, e.g. a
     * webservice or similar. See the example for what to do and not to do.
     *
     * @example
// This is how the method is meant to be used
var x = XmlUtil.fromString( someSemiUnknownStringPassedUsFromAWebServiceOrSimilar );
// This is a bug, that will trigger a warning in the log:
var x = XmlUtil.fromString( somethingThatIsXMLAlready );
// This is an ineffecient way to create a small piece of XML:
var x = XmlUtil.fromString( "<some>xml</some>" );
// The above is better written as this:
var x = <some>xml</some>;
     * 
     * @type {function}
     * @param {string} xmlString A string containing XML, possibly with a preprocessing directive and other cruft.
     * @return {XML} An XML object created from the string.
     * @syntax XmlUtil.fromString( xmlString );
     * @see XmlUtil.prettyPrint
     * @method
     * @name XmlUtil.fromString */
    that.fromString = function( xmlString ) {
        Log.trace( "->XmlUtil.fromString()" ); // AUTO::BUG#8976
        // Warn if passed object is wrong type.
        if ( xmlString instanceof XML ) {
            Log.warn( "XmlUtil called with xml parameter, expected string!: " + xmlString.toXMLString( ) );
            return xmlString;
        }
        // Log.debug( "XmlUtil.fromString: xmlString before replace:\n", xmlString );
        xmlString = xmlString.replace( /^[\s\n]*</, "<" );
        // Log.debug( "XmlUtil.fromString: xmlString after replace 1:\n", xmlString );
        xmlString = xmlString.replace( /^<\?xml.*\?>[\s\n]*/, "" ); // string <?xml...> from head
        // Log.debug( "XmlUtil.fromString: xmlString after replace 2:\n", xmlString );
        xmlString = xmlString.replace( /^<\!DOCTYPE.*?\>[\s\n]*/, "" ); // strip <!DOCTYPE...> from head
        // Log.debug( "XmlUtil.fromString: xmlString after replace 3:\n", xmlString );
        xmlString = xmlString.replace( /^<\!--[\s\S]*?-->[\s\n]*/, "" ); // bug 14073: strip leading comments
        // Log.debug( "XmlUtil.fromString: xmlString after replace 4:\n", xmlString );
        xmlString = xmlString.replace( /[\s\n]*$/, "" ); // Remove spaces in the end
        // Log.debug( "XmlUtil.fromString: xmlString after replace 5:\n", xmlString );
        var res = new XML( xmlString ); // This is needed for reasons that are not totally clear to me );
        // Log.debug( "XmlUtil.fromString: resulting xml:\n", res );
        return res;
    };

    /**
     * Pretty print an XML document/snippet, with restrictions.
     *
     * This method takes and XML object (not XMLList) and produces a string
     * representing the object that is suited for e.g. use with diff 
     * 
     * **Note:** This method is slow! Compared to the builtin toXMLString,
     * the method is 10-30 times as slow. It should not be used for
     * programs that needs to run quickly. Also: The output string may
     * not always represent the exact XML input: Insignificant
     * whitespace can be introduced between elements, but not *in*
     * text elements. Contains no support for outputting CDATA,
     * comments or preprocessing nodes, as there is no known way to
     * get these nodes into the XML object. Only the control
     * characters of xml 1.0 are supported (x9, xA, xD).
     *
     * @syntax XmlUtil.prettyPrint( xml, options )
     * @param {XML} xml An xml object (not XMLList) to pretty print
     * @param options Object with options for the creation of the XML string representation.
     * @param {boolean} [options.sortAttributes=true] If true, the attributes will be sorted when output
     * @param {boolean} [options.diffImprove=true] If true, output will be better suited for diff (attributes and namespaces on single lines).
     * @param {number} [options.indentLevel=2] Level of indent for each XML level
     * @return {string} A string with a representation of the XML (see note)
     * @example
// This example, from using a JavaScript shell, demonstrates the formatting of a simple XML document
js> XmlUtil.prettyPrint( <root a='aat'   c='cat' b='bat'><foo fooaat='fooaatat'  foobat='foobatat'>bar</foo></root>, { diffImprove : true } );
<root
    a="aat"
    b="bat"
    c="cat">
  <foo
      fooaat="fooaatat"
      foobat="foobatat">bar</foo>
</root>
     * @method
     * @name XmlUtil.prettyPrint */
    that.prettyPrint = function( xml, options ) {
        // Note: Technically this should be a reimplementation of toXMLString from the ECMA standard,
        // pp 10.2.1, but that was just too crazy, so I did this instead. 
        // Normal toXMLString can insert non-significant whitespace in text elements, and between elements.
        // This implementation only inserts whitespace between elements, not *in* elements.
        // Note: The use of a buffer to collect generated strings in, is intentional and due to (real) performance issues 
        // in string concatenation.
        Log.trace( "->XmlUtil.prettyPrint" );
        // Set up default options
        options = options || {};
        if ( options.sortAttributes === undefined ) {
            options.sortAttributes = true;
        }
        options.diffImprove = options.diffImprove || false;
        if ( options.diffImprove ) {
            options.sortAttributes = true;
        }
        options.indentLevel = options.indentLevel || 2;
        Log.debug( "diffImprove: " + options.diffImprove.toString( ) + ", sortAttributes: " + options.sortAttributes.toString( ) +
            ", indentLevel: " + options.indentLevel.toString( ) );


        // Fix indentprefix - add one because join is "in-between".
        var indentPrefix = new Array( options.indentLevel + 1 ).join( " " ); // new Array OK

        // This is a "function global" buffer that is used to collect the 
        // result in. The idea is to reduce the number of string operations
        var buffer = [ ];

        ////////////////////////////////////////////////////////////
        // Escapes some key chars.
        // Lifted from a gist, no license.
        // NB: Adjusted to work almost according to ECMA
        // 10.2.1.1 : EscapeElementValue
        // 10.2.1.2 : EscapeAttributeValue
        var XML_CHAR_MAP = {
            '<': '&lt;',
            '>': '&gt;',
            '&': '&amp;',
            '"': '&quot;',
            "'": '&apos;',
            "\u000A": "&#xA;",
            "\u000D": "&#xD;",
            "\u0009": "&#x9;",
        };

        function escapeXml( s ) {
            return s.replace( /[<>&\"\'\r\t\n]/g, function( ch ) {
                    return XML_CHAR_MAP[ ch ];
                } );
        };

        ////////////////////////////////////////////////////////////
        // Format a single namespace
        var formatNamespace = function( namespace ) {
            var name = namespace.prefix;
            if ( name != "" ) {
                name = ":" + name;
            }
            var res = [ ];
            res.push( "xmlns" );
            res.push( name );
            res.push( '="' );
            res.push( escapeXml( String( namespace.valueOf( ) ) ) );
            res.push( '"' );
            return res.join( "" );
        };

        // Format all the available namespaces for a particular node...
        var formatNamespaces = function( xml, level ) {
            var namespaces = xml.namespaceDeclarations( );
            if ( namespaces.length == 0 ) {
                return "";
            }
            if ( options.sortAttributes ) {
                namespaces.sort( function( a, b ) {
                        return a.prefix > b.prefix;
                    } );
            }
            var res = [ ];
            var newline = "";
            var indent = " ";
            if ( options.diffImprove && namespaces.length > 1 ) {
                newline = "\n";
                indent = level + indentPrefix + indentPrefix;
            }
            for ( var i in namespaces ) {
                res.push( newline );
                res.push( indent );
                res.push( formatNamespace( namespaces[ i ] ) );
            }
            return res.join( "" );
        };

        // Format a single attribute, no indent, no newline, no nothing
        // Takes into acount namespaces and prefix values.
        var formatAttribute = function( xml ) {
            var res = [ ];
            var name = xml.localName( );
            var ns = xml.namespace( );
            if ( ns && ns.prefix != "" ) {
                name = ns.prefix + ":" + name;
            }
            res.push( name );
            res.push( '="' );
            res.push( escapeXml( String( xml.valueOf( ) ) ) );
            res.push( '"' );
            return res.join( "" );
        };

        // Format all attributes of an element
        // Takes into account indent and newlines (if more than one attribute)
        // The returned result always starts with at least a single space or a newline
        var formatAttributes = function( xml, level ) {
            var attributes = that.getAttributes( xml );
            if ( attributes.length == 0 ) {
                return "";
            }
            // If applicable, sort, but only by localName. Prefix'es will be "intermixed".
            if ( options.sortAttributes ) {
                attributes.sort( function( a, b ) {
                        return a.localName( ) > b.localName( );
                    } );
            }
            var res = [ ];
            var newline = "";
            var indent = " ";
            if ( options.diffImprove && attributes.length > 1 ) {
                newline = "\n";
                indent = level + indentPrefix + indentPrefix;
            }
            for ( var i in attributes ) {
                res.push( newline );
                res.push( indent );
                res.push( formatAttribute( attributes[ i ] ) );
            }
            return res.join( "" );
        };


        // Format an object, that is, this object, and all children.
        // xml - the xml object/element to format
        // indent - wheter to do indent/format first line of element or not
        // level - the current indentation level (as a string)
        var formatNode = function( xml, indent, level ) {

            // Do different things for element, text, comment and preprocesser instruction
            var nodeKind = xml.nodeKind( );
            if ( nodeKind == "element" ) {
                // Format namespaces, attributes and children.

                var name = xml.name( ).localName;
                var ns = xml.namespace( );
                if ( ns.prefix != "" ) {
                    name = ns.prefix + ":" + name;
                }
                if ( indent ) {
                    buffer.push( "\n" );
                    buffer.push( level );
                }
                buffer.push( "<" );
                buffer.push( name );
                buffer.push( formatNamespaces( xml, level ) );
                buffer.push( formatAttributes( xml, level ) );

                // For children:
                // The idea is to have each element format itself recursively.
                // Formatting includes newline and indents before the element, but not after.
                // Text elements do not insert newlines before themselves
                // Elements following a textelement is not allowed to newline and indent before, but may do after.
                // This is controlled by the "indent" variable.
                // If the final element is a newline, the closing tag is not set
                // If this element contains any text nodes, we do not want to indent the content of this 
                // element. (This does not influence e.g. attributes on separate lines).

                // Used for iteration
                var children = xml.children( );

                // Used to track if the childrens made any content
                // Optimistically assume that children means that we need a closing tag
                var contentIndex = buffer.push( ">" );

                // Actually figure out if we can introduce spaces/formatting into an element/mixed content
                var lastWasNonTrivialTextNode = false;
                for ( var i in children ) {
                    formatNode( children[ i ], !lastWasNonTrivialTextNode, level + indentPrefix );
                    lastWasNonTrivialTextNode = ( children[ i ].nodeKind( ) == "text" && String( children[ i ].valueOf( ) ) != "" );
                }

                // Did we generate any content at all? If not, close tag by it self.
                if ( contentIndex == buffer.length ) {
                    buffer[ contentIndex - 1 ] = "/>";
                } else {
                    // If last was a non trivial text node, do not insert a newline before close tag
                    // buffer[contentIndex-1] = ">"; // already done
                    if ( !lastWasNonTrivialTextNode ) {
                        buffer.push( "\n" + level );
                    }
                    buffer.push( "</" + name + ">" );
                }
            } else if ( nodeKind == "text" ) {
                buffer.push( escapeXml( String( xml.valueOf( ) ) ) );
            } else {
                // We do not actually expect to get here!
                Log.warn( "XmlUtil.prettyPrint: Unexpected nodeKind " + nodeKind + " occured. Reverting to toXMLString()" );
                buffer.push( nodeKind.toXMLString( ) );
            }
        };

        formatNode( xml, false, "" );
        return buffer.join( "" );
    };

    /** 
     * Make object that uses toXMLString as toString.
     * 
     * This function "wraps" the xml object passed to it, and creates a new object that uses the toXMLString as toString.
     * It is mostly intended to be used when logging XML objects. By using this method, the toXMLString is only called if the 
     * log level is enabled, which can result in huge performance improvements when levels are disabled.
     *  See the example for more information.
     * 
     * @example
// Do this, when you need to log some XML (good):
Log.debug( "The xml is: ", XmlUtil.logXMLString( xml ) );
// instead of this (bad, don't do this): 
Log.debug( "The xml is: ", xml.toXMLString() );
     * 
     * @param {XML} xml An xml object to call toXMLString on.
     * @return {Object} An object that can be used for delayed toString invocations.
     * @syntax XmlUtil.logXMLString( xml );
     * @name XmlUtil.logXMLString
     * @method
     */
    that.logXMLString = function ( xml ) {
        var res = {};
        res.toString = function () {
            return xml.toXMLString();
        }   
        return res;
    }
    
    
    /**
     * Get the attributes of an xml object.
     *
     * This function get the attributes of an xml object in a portable way.
     *
     * The call to attributes on an xml object does not work well across Rhino
     * and Spidermonkey. This call returns the same on both platforms. 
     *
     * **Note:** This filters out any attributes that truly are
     * namespaces. Rhino delivers those, and we do not want them.
     * 
     * @type {function}
     * @method
     * @param {XML} xml An xml object (not XMLList) to get the attributes from.
     * @return {XML[]} An array of attributes instances (XML objects). They have methods like name, namespace, localname and you can get the value of the attribute by using the valueOf method.
     * @syntax XmlUtil.getAttributes( xml );
     * @name XmlUtil.getAttributes */
    that.getAttributes = function( xml ) {
        var res = [ ];
        var attributesAll = xml.@*:: * ;
        for ( var i in attributesAll ) {
            // This is sort a heuristic. It must be attributes, but
            // Rhino puts in namespaces as well. But, the namespace Rhino puts in
            // has an undefined "namespaces" function... So, we check for that.
            // Also, attributes marked with the name xmlns are not attributes, but namespaces... 
            if ( attributesAll[ i ].namespace( ) != undefined && attributesAll[ i ].localName( ) != "xmlns" ) {
                res.push( attributesAll[ i ] );
            }
        }
        return res;
    };

    /**
     * Function that sorts XML elements alfabethically (element name).
     *
     * Function that sort the elements of an XML object after element name 
     *
     * **Note:** This function modifies the object passed to it. The
     * returned reference is a reference to the input parameter.
     *
     * @type {function}
     * @method
     * @syntax XmlUtil.sortElement( xml )
     * @param {XML} xml XML objekt to be sorted
     * @return {XML} XML object with alfabethically sorted elements
     * @name XmlUtil.sortElements */
    that.sortElements = function( xml ) {
        Log.trace( "->XmlUtil.sortElements()" ); // AUTO::BUG#8976
        var elementList = xml.*;
        var elementArray = [ ];
        for each( var item in elementList ) {
            elementArray.push( item.name( ) );
        }
        elementArray.sort( );
        var xmlOut = xml;
        delete xmlOut.*;
        for ( var i in elementArray ) {
            if ( elementArray.hasOwnProperty( i ) ) {
                var element = elementArray[ i ];
                for each( var child in elementList ) {
                    if ( child.name( ) === element ) {
                        xmlOut.element = child;
                    }
                }
            }
        }
        // Log.debug( xmlOut );
        return xmlOut;

    };

    /**
     * Function that filters an XMLList, using a predicate function.
     * 
     * Filters the XMLList object, and calls the predicateFunction for each
     * element. If the predicateFunction returns true, the element is added to
     * the result. If no elements match, an empty XMLList object is returned. 
     * 
     * @type {function}
     * @method
     * @syntax XmlUtil.filterXMLListPredicate( xmllist, predicateFunction );
     * @param {XMLList} xmllist The XMLList object to filter
     * @param {function} predicateFunction The predicate function to call for each element
     * @example
// Filter all XMLList nodes in the xmllist variable, that have an attribute called foobar, without a namespace
XmlUtil.filterXMLListPredicate( xmllist, 
  function ( element ) { return XmlUtil.hasAttribute( element, undefined, "foobar" ); } 
);
     * @return {XMLList} A new XMLList object with the elements from the xmllist where the predicate function returned true
     * @name XmlUtil.filterXMLListPredicate */
    that.filterXMLListPredicate = function( xmllist, predicateFunction ) {
        ValueCheck.checkThat( "xmllist", xmllist ).is.defined.and.has.type( "xml" );
        ValueCheck.checkThat( "predicateFunction", predicateFunction ).is.defined.and.has.type( "function" );
        var output = new XMLList( );
        for each( var element in xmllist ) {
            if ( predicateFunction( element ) ) {
                output += element;
            }
        }
        return output;
    };

    /**
     * Function that checks if a given xml object has a given attribute.
     * 
     * This function checks if the xml object has a given attribute with
     * attributeName, and optionally a specific namespace. 
     *
     * **Note:** The function returns true, even if the value of the
     * attribute is the empty string. This is a bit harder than it
     * sounds, when needed to work on both C++ and Java backends
     * 
     * @type {function}
     * @method
     * @syntax XmlUtil.hasAttribute( xml, ns, attributeName );
     * @param {XML} xml The xml object to check for an attribute
     * @param {Namespace} ns The namespace for the attributeName, or undefined if no namespace should be present
     * @param {string} attributeName The name of the attribute to check for
     * @return {boolean} True if the attribute with the optional namespace was found, false otherwise
     * @example 
// To check if the attribute xsi:type exists in a given xml object, do e.g. like this:
if ( XmlUtil.hasAttribute( xml, XmlNamespaces.xsi, "type" ) ) {
    print( "Found attribute xsi:type for xml\n" );
} else { 
    print( "Did not find attribute xsi:type for xml\n" );
}; 
// To check if the attribute xsi:type exists in a given xml object and 
// is not the empty string, do e.g. like this:
var xsi = XmlNamespaces.xsi;
// Note the String creation in the test
if ( XmlUtil.hasAttribute( xml, XmlNamespaces.xsi, "type" ) && String( xml.&#64;xsi::type ) != "" ) {
    print( "Found non-empty attribute xsi:type for xml\n" );
} else { 
    print( "Did not find non-empty attribute xsi:type for xml\n" );
};
     * @name XmlUtil.hasAttribute  */
    that.hasAttribute = function( xml, ns, attributeName ) {
        ValueCheck.checkThat( "xml", xml ).is.defined.and.has.type( "xml" );
        ValueCheck.checkThat( "attributeName", attributeName ).is.defined.and.has.type( "string" ).and.is.not.equalTo( "" );
        if ( ns === undefined ) {
            return ( xml.@[ attributeName ].length( ) !== 0 );
        } else {
            // This is really ugly. There is no way to get an
            // attribute with a namespace when not using the literal
            // syntax, AFAICT.  Attributes does not return those with
            // namespaces, according to spec/spidermonkey. (Rhino of
            // course - works). So, we get all the attributes with
            // namepart attributeName for xmlObject, with @*::[
            // attributeName ], then traverses to see if one matches
            // the uri.
            var attribs = xml.@*::[ attributeName ];
            for ( var attrib in attribs ) {
                if ( attribs[ attrib ].name( ).uri == ns.uri ) {
                    return true;
                }
            }
            return false;
        }
    };


    /**
     * Function that filters an XMLList, returning elements that have a specific attribute.
     *
     * Filters the XMLList object, and returns the elements that have a
     * specific attribute, optionally with a specific namespace. If no
     * elements match, an empty XMLList object is returned. 
     * 
     * @type {function}
     * @method
     * @syntax XmlUtil.filterXMLListHasAttribute( xmllist, ns, attributeName );
     * @param {XMLList} xmllist The XMLList object to filter
     * @param {Namespace} ns The namespace for the attributeName, or undefined if no namespace should be present
     * @param {string} attributeName The name of the attribute to filter for
     * @return {XMLList} A new XMLList object with the elements from the xmllist that has the given attribute
     * @example 
// To obtain an XMLList object with all the dc:title elements 
// that has a xsi:type attribute, do e.g. like this:
XmlUtil.filterXMLListHasAttribute( 
  <collection xmlns:dkabm="http://biblstandard.dk/abm/namespace/dkabm/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <dkabm:record><dc:title>This is a title</dc:title>
      <dc:title xsi:type="dkdcplus:full">This is a title : full</dc:title>
      <dc:title xsi:type="dkdcplus:series">MagnaPrintserien ; nr. 226</dc:title>
      <dc:title xsi:type="dkdcplus:series">Series without number</dc:title>
      <dc:title>No type title</dc:title>
    </dkabm:record>
  </collection>.dkabm::record.dc::title, 
  XmlNamespaces.xsi, 
  'type' );
     * @name XmlUtil.filterXMLListHasAttribute */
    that.filterXMLListHasAttribute = function( xmllist, ns, attributeName ) {
        return this.filterXMLListPredicate( xmllist, function( element ) {
                return XmlUtil.hasAttribute( element, ns, attributeName );
            } );
    };

    /**
     * Function to extract and return the first element from an xmlobject, that is not empty.
     *
     * This function scans all children of the xml object. The first
     * child that is an non-empty XML element is returned. Note, that
     * text nodes are ignored.
     *
     * This function is implemented as a call to getNonTrivialChildElement( xml, 0);
     * 
     * @type {function}
     * @method
     * @syntax XmlUtil.getFirstNonTrivialSubElement( xml )
     * @param {XML} xml XML object to extract first element from
     * @return {XML} XML object representing the first element that is not empty, i.e. have a name() != "", or undefined, if none found
     * @name XmlUtil.getFirstNonTrivialSubElement */
    that.getFirstNonTrivialSubElement = function( xml ) {
        return this.getNonTrivialChildElement( xml, 0 );
    };

    /** 
     * Function to extract a non-trivial child
     * 
     *
     * This function scans all children of the xml object. The n'th
     * child that is an non-empty XML element is returned. Note, that
     * text nodes are ignored.
     * 
     * @example
// The following call will return undefined, as there are only trivial elements:
XmlUtil.getNonTrivialChildElement( <foo></foo>, 0 );
// This will return the bar element 
XmlUtil.getNonTrivialChildElement( <foo>roo<bar></bar></foo>, 0 )
// This will return the fisk2 element
XmlUtil.getNonTrivialChildElement( <hep> <fisk1></fisk1> <fisk2></fisk2></hep>, 1 );
// This will return the fisk3 element
XmlUtil.getNonTrivialChildElement( <hep> <fisk1></fisk1> <fisk2></fisk2> <fisk3></fisk3></hep>, 2 );


     * @type {function}
     * @method
     * @syntax XmlUtil.getNonTrivialChildElement( xml )
     * @param {XML} xml XML object to extract first element from
     * @param {Number} index The index of the non-trivial child to extract, zero based
     * @return {XML} XML object representing the first element that is not empty, i.e. have a name() != "", or undefined, if none found
     * @name XmlUtil.getNonTrivialChildElement */
    that.getNonTrivialChildElement = function( xml, index ) {
        ValueCheck.checkThat( "xml", xml ).is.defined.and.has.type( "xml" );
        ValueCheck.checkThat( "index", index ).is.defined.and.has.type( "number" ).and.is.greaterThan( -1 );
        var count = 0;
        for ( var x in xml.* ) {
            if ( xml.*[ x ].name( ) != undefined ) {
                if ( count == index ) {
                    return xml.*[ x ];
                } else {
                    ++count;
                }
            }
        }
        return undefined;
    };


    /**
     * Function to determine if a given xml object has a given namespace uri and localName.
     * 
     * Tests ns.uri against xml.name().uri and localName against
     * xml.localName() 
     * 
     * @type {function}
     * @method
     * @syntax XmlUtil.harUriLocalName( xmlobject, ill5namespace, "Cancel-Reply-7" );
     * @param {XML} xml XML object to test
     * @param {Namespace} ns Namespace object to test for or undefined if no namespace should be present.
     * @param {string} localName localName to test for
     * @return {boolean} True if matching, false otherwise
     * @name XmlUtil.hasUriLocalName
     */
    that.hasUriLocalName = function( xml, ns, localName ) {
        ValueCheck.checkThat( "xml", xml ).is.defined.and.has.type( "xml" );
        ValueCheck.checkThat( "localName", localName ).is.defined.and.has.value.not.equalTo( "" );
        if ( ns != undefined ) {
            ValueCheck.checkThat( "ns", ns ).has.type( "object" );
            if ( ns.uri != xml.name( ).uri ) {
                return false;
            }
        } else {
            if ( xml.name( ).uri != "" ) {
                return false;
            }
        }
        return xml.localName( ) == localName;
    };

    /**
     * Function to check if a given xml object has root and element type of a specific type.
     *
     * This function checks if the given xml object has a name that matches
     * the namespace/root combo, and if the first xml subelement matches
     * namespace/element. It is not possibly to use different namespaces for
     * the rootName and elementName 
     * 
     * @type {function}
     * @method
     * @syntax XmlUtil.matchesRootAndElement( xml, ns, rootName, elementName )
     * @param {XML} xml XML object to test
     * @param {Namespace} ns Namespace object to test for or undefined if no namespace should be present.
     * @param {string} rootName localName for root element to test for
     * @param {string} elementName localName for first child xml element to test for
     * @return {boolean} True if matching, false otherwise
     * @name XmlUtil.matchesRootAndElement */
    that.matchesRootAndElement = function( xml, ns, rootName, elementName ) {
        ValueCheck.checkThat( "xml", xml ).is.defined.and.has.type( "xml" );
        ValueCheck.checkThat( "rootName", rootName ).is.defined.and.has.value.not.equalTo( "" );
        ValueCheck.checkThat( "elementName", elementName ).is.defined.and.has.value.not.equalTo( "" );
        if ( !this.hasUriLocalName( xml, ns, rootName ) ) {
            return false;
        }
        var element = this.getFirstNonTrivialSubElement( xml );
        return element != undefined && this.hasUriLocalName( element, ns, elementName );
    };

    //////////////////////////////////////////////////////////////////////
    // bug 13996 - harmonize whitespace Rhino and Spidermonkey
    var originalXmlSettings;
    var init = function( ) {
        // Store XML Settings
        originalXmlSettings = XML.settings( );
        // Set up whitespace handling to work as we want to
        XML.setSettings( {
                ignoreWhitespace: false,
                prettyPrinting: false
            } );
    };

    // Initialize
    init( );

    // Return the constructed object.
    return that;
}( );

////////////////////////////////////////////////////////////////////////////////
// UnitTests
////////////////////////////////////////////////////////////////////////////////

UnitTest.addFixture( "util.XmlUtil module", function( ) {

        // Test some whitespace handling before and after actual XML.
        // see bug https://bugs.dbc.dk/show_bug.cgi?id=12977#c3
        // These are not influenced by ignoreWhitespace or not
        Assert.equal( "Trim 1", 'XmlUtil.fromString( "<root>foo</root>" ).toSource()', ( <root>foo</root> ).toSource( ) );
        Assert.equal( "Trim 2", 'XmlUtil.fromString( " <root>foo</root>" ).toSource()', ( <root>foo</root> ).toSource( ) );
        Assert.equal( "Trim 3", 'XmlUtil.fromString( "<root>foo</root> " ).toSource()', ( <root>foo</root> ).toSource( ) );
        Assert.equal( "Trim 4", 'XmlUtil.fromString( " <root>foo</root> " ).toSource()', ( <root>foo</root> ).toSource( ) );
        Assert.equal( "Trim 5", 'XmlUtil.fromString( "   <root>foo</root>   " ).toSource()', ( <root>foo</root> ).toSource( ) );
        Assert.equal( "Trim 6", 'XmlUtil.fromString( "<root>foo</root>\\n" ).toSource()', ( <root>foo</root> ).toSource( ) );

        // These are influenced by ignoreWhitespace and must be changed if this setting is changed.
        // NOTE: The newlines matter!!!
        Assert.equal( "Trim 7", 'XmlUtil.fromString( "<root>  <bar>foo</bar>\\n </root>" ).toSource()', ( <root>  <bar>foo</bar>
 </root> ).toSource( ) );
        Assert.equal( "Trim 8", 'XmlUtil.fromString( "   <root>  <bar>foo</bar>\\n </root>\\n   " ).toSource()', ( <root>  <bar>foo</bar>
 </root> ).toSource( ) );
        __XmlUtil_ut = {};

        // Lots of whitespace tests from raw XML. bug 13996
        __XmlUtil_ut.ns = <div>;</div>;
        __XmlUtil_ut.sr = <div>; </div>;
        __XmlUtil_ut.sl = <div> ;</div>;
        __XmlUtil_ut.sb = <div> ; </div>;

        Assert.equal( "toString with no spaces", "__XmlUtil_ut.ns.toString()", ';' );
        Assert.equal( "toString with space right", "__XmlUtil_ut.sr.toString()", '; ' );
        Assert.equal( "toString with space left", "__XmlUtil_ut.sl.toString()", ' ;' );
        Assert.equal( "toString with space both", "__XmlUtil_ut.sb.toString()", ' ; ' );

        Assert.equal( "toXMLString with no spaces", "__XmlUtil_ut.ns.toXMLString()", "<div>;</div>" );
        Assert.equal( "toXMLString with space right", "__XmlUtil_ut.sr.toXMLString()", "<div>; </div>" );
        Assert.equal( "toXMLString with space left", "__XmlUtil_ut.sl.toXMLString()", "<div> ;</div>" );
        Assert.equal( "toXMLString with space both", "__XmlUtil_ut.sb.toXMLString()", "<div> ; </div>" );

        Assert.equal( "uneval with no spaces", "uneval( __XmlUtil_ut.ns )", "<div>;</div>" );
        Assert.equal( "uneval with space right", "uneval( __XmlUtil_ut.sr )", "<div>; </div>" );
        Assert.equal( "uneval with space left", "uneval( __XmlUtil_ut.sl )", "<div> ;</div>" );
        Assert.equal( "uneval with space both", "uneval( __XmlUtil_ut.sb )", "<div> ; </div>" );

        Assert.equal( "toSource() with no spaces", "( __XmlUtil_ut.ns ).toSource()", "<div>;</div>" );
        Assert.equal( "toSource() with space right", "( __XmlUtil_ut.sr ).toSource()", "<div>; </div>" );
        Assert.equal( "toSource() with space left", "( __XmlUtil_ut.sl ).toSource()", "<div> ;</div>" );
        Assert.equal( "toSource() with space both", "( __XmlUtil_ut.sb ).toSource()", "<div> ; </div>" );

        // fromString(), bug 13996
        __XmlUtil_ut.ns = XmlUtil.fromString( "<div>;</div>" );
        __XmlUtil_ut.sr = XmlUtil.fromString( "<div>; </div>" );
        __XmlUtil_ut.sl = XmlUtil.fromString( "<div> ;</div>" );
        __XmlUtil_ut.sb = XmlUtil.fromString( "<div> ; </div>" );

        Assert.equal( "toString with no spaces", "__XmlUtil_ut.ns.toString()", ';' );
        Assert.equal( "toString with space right", "__XmlUtil_ut.sr.toString()", '; ' );
        Assert.equal( "toString with space left", "__XmlUtil_ut.sl.toString()", ' ;' );
        Assert.equal( "toString with space both", "__XmlUtil_ut.sb.toString()", ' ; ' );

        Assert.equal( "toXMLString with no spaces", "__XmlUtil_ut.ns.toXMLString()", "<div>;</div>" );
        Assert.equal( "toXMLString with space right", "__XmlUtil_ut.sr.toXMLString()", "<div>; </div>" );
        Assert.equal( "toXMLString with space left", "__XmlUtil_ut.sl.toXMLString()", "<div> ;</div>" );
        Assert.equal( "toXMLString with space both", "__XmlUtil_ut.sb.toXMLString()", "<div> ; </div>" );

        Assert.equal( "uneval with no spaces", "uneval( __XmlUtil_ut.ns )", "<div>;</div>" );
        Assert.equal( "uneval with space right", "uneval( __XmlUtil_ut.sr )", "<div>; </div>" );
        Assert.equal( "uneval with space left", "uneval( __XmlUtil_ut.sl )", "<div> ;</div>" );
        Assert.equal( "uneval with space both", "uneval( __XmlUtil_ut.sb )", "<div> ; </div>" );

        Assert.equal( "toSource() with no spaces", "( __XmlUtil_ut.ns ).toSource()", "<div>;</div>" );
        Assert.equal( "toSource() with space right", "( __XmlUtil_ut.sr ).toSource()", "<div>; </div>" );
        Assert.equal( "toSource() with space left", "( __XmlUtil_ut.sl ).toSource()", "<div> ;</div>" );
        Assert.equal( "toSource() with space both", "( __XmlUtil_ut.sb ).toSource()", "<div> ; </div>" );

        // fromString(), bug 13996
        __XmlUtil_ut.ns = XmlUtil.fromString( "<foo>\n  <div>\n;</div>\n</foo>" );
        __XmlUtil_ut.sr = XmlUtil.fromString( "<foo>\n  <div>\n; </div>\n</foo>" );
        __XmlUtil_ut.sl = XmlUtil.fromString( "<foo>\n  <div>\n ;</div>\n</foo>" );
        __XmlUtil_ut.sb = XmlUtil.fromString( "<foo>\n  <div>\n ; </div>\n</foo>" );

        Assert.equal( "toString with no spaces", "__XmlUtil_ut.ns.toString()", "<foo>\n  <div>\n;</div>\n</foo>" );
        Assert.equal( "toString with space right", "__XmlUtil_ut.sr.toString()", "<foo>\n  <div>\n; </div>\n</foo>" );
        Assert.equal( "toString with space left", "__XmlUtil_ut.sl.toString()", "<foo>\n  <div>\n ;</div>\n</foo>" );
        Assert.equal( "toString with space both", "__XmlUtil_ut.sb.toString()", '<foo>\n  <div>\n ; </div>\n</foo>' );

        Assert.equal( "toXMLString with no spaces", "__XmlUtil_ut.ns.toXMLString()", "<foo>\n  <div>\n;</div>\n</foo>" );
        Assert.equal( "toXMLString with space right", "__XmlUtil_ut.sr.toXMLString()", "<foo>\n  <div>\n; </div>\n</foo>" );
        Assert.equal( "toXMLString with space left", "__XmlUtil_ut.sl.toXMLString()", "<foo>\n  <div>\n ;</div>\n</foo>" );
        Assert.equal( "toXMLString with space both", "__XmlUtil_ut.sb.toXMLString()", '<foo>\n  <div>\n ; </div>\n</foo>' );

        Assert.equal( "uneval with no spaces", "uneval( __XmlUtil_ut.ns )", "<foo>\n  <div>\n;</div>\n</foo>" );
        Assert.equal( "uneval with space right", "uneval( __XmlUtil_ut.sr )", "<foo>\n  <div>\n; </div>\n</foo>" );
        Assert.equal( "uneval with space left", "uneval( __XmlUtil_ut.sl )", "<foo>\n  <div>\n ;</div>\n</foo>" );
        Assert.equal( "uneval with space both", "uneval( __XmlUtil_ut.sb )", '<foo>\n  <div>\n ; </div>\n</foo>' );

        Assert.equal( "toSource() with no spaces", "( __XmlUtil_ut.ns ).toSource()", "<foo>\n  <div>\n;</div>\n</foo>" );
        Assert.equal( "toSource() with space right", "( __XmlUtil_ut.sr ).toSource()", "<foo>\n  <div>\n; </div>\n</foo>" );
        Assert.equal( "toSource() with space left", "( __XmlUtil_ut.sl ).toSource()", "<foo>\n  <div>\n ;</div>\n</foo>" );
        Assert.equal( "toSource() with space both", "( __XmlUtil_ut.sb ).toSource()", '<foo>\n  <div>\n ; </div>\n</foo>' );

        // Remove preprocessing stuff in the beginning
        __XmlUtil_ut.xmlStringWP = " <?xml version=\"1.0\"?> \n <root><some>node</some></root>\n ";
        __XmlUtil_ut.xmlStringWOP = "<root><some>node</some></root>";
        __XmlUtil_ut.xmlStringRes = ( <root><some>node</some></root> ).toSource( );

        Assert.equal( "Ens uden pp", 'XmlUtil.fromString( __XmlUtil_ut.xmlStringWP ).toSource()', __XmlUtil_ut.xmlStringRes );
        Assert.equal( "Ens med pp", 'XmlUtil.fromString( __XmlUtil_ut.xmlStringWOP ).toSource()', __XmlUtil_ut.xmlStringRes );
        Assert.exception( "Invalid XML", 'XmlUtil.fromString( "<root>dette er ikke XML</toor>" )' );

        // Bug 14073: Starting with comments no longer works, after not-ignoring whitespace. Sigh.

        Assert.equal( "Start with comment 1", 'XmlUtil.fromString( "<!--Alle niveauer--><sroot><some>node</some></sroot>" ).toSource()', "<sroot><some>node</some></sroot>" );
        __XmlUtil_ut.foobar = XmlUtil.fromString( "<?xml version=\"1.0\"?><!--Alle niveauer--><sroot><some>node</some></sroot>" ).toSource( );
        Assert.equal( "Start with comment 2", '__XmlUtil_ut.foobar', "<sroot><some>node</some></sroot>" );
        __XmlUtil_ut.foobar = XmlUtil.fromString( "<?xml version=\"1.0\"?>\n<!--Alle niveauer\n-->\n<sroot><some>node</some></sroot>" ).toSource( );
        Assert.equal( "Start with comment 3", '__XmlUtil_ut.foobar', "<sroot><some>node</some></sroot>" );
        __XmlUtil_ut.foobar = XmlUtil.fromString( "<!--Alle niveauer--><sroot><!-- Andre niveauer--><some>node</some></sroot>" ).toSource( );
        Assert.equal( "Start with comment 4", '__XmlUtil_ut.foobar', "<sroot><!-- Andre niveauer--><some>node</some></sroot>" );

        ////////////////////////////////////////////////////////////////////////////////
        // Test logXMLString
        __XmlUtil_ut.logXMLString = <foo><bar>foobar</bar></foo>;
        Assert.equal( "logXMLString test 1", '__XmlUtil_ut.logXMLString.toXMLString()', 
                      XmlUtil.logXMLString(__XmlUtil_ut.logXMLString ).toString() );
        
        ////////////////////////////////////////////////////////////////////////////////
        // Test get attributes
        __XmlUtil_ut.ns1 = new Namespace( "fop", "http://www.example.org/" );
        __XmlUtil_ut.ns2 = new Namespace( "foz", "http://www.example2.org/" );

        __XmlUtil_ut.at1 = <root/>;
        Assert.equal( "getAtttributes 1", "XmlUtil.getAttributes( __XmlUtil_ut.at1 )", [ ] );

        __XmlUtil_ut.at2 = <root a="aat"/>;
        __XmlUtil_ut.at2_aat = __XmlUtil_ut.at2.attribute( "a" );
        Assert.equal( "getAtttributes 2", "XmlUtil.getAttributes( __XmlUtil_ut.at2 )", [ __XmlUtil_ut.at2_aat ] );

        __XmlUtil_ut.at3 = <root a="aat" b="bat"/>;
        __XmlUtil_ut.at3_aat = __XmlUtil_ut.at3.attribute( "a" );
        __XmlUtil_ut.at3_bat = __XmlUtil_ut.at3.attribute( "b" );
        Assert.that( "getAttributes 3 postcond", "__XmlUtil_ut.at3_bat != undefined" );
        Assert.equal( "getAtttributes 3", "XmlUtil.getAttributes( __XmlUtil_ut.at3 ).sort()", [ __XmlUtil_ut.at3_aat, __XmlUtil_ut.at3_bat ].sort( ) );

        __XmlUtil_ut.at4 = <root a="aat" c="cat" b="bat"/>;
        __XmlUtil_ut.at4_aat = __XmlUtil_ut.at4.attribute( "a" );
        __XmlUtil_ut.at4_bat = __XmlUtil_ut.at4.attribute( "b" );
        __XmlUtil_ut.at4_cat = __XmlUtil_ut.at4.attribute( "c" );
        Assert.equal( "getAtttributes 4", "XmlUtil.getAttributes( __XmlUtil_ut.at4 ).sort()", [ __XmlUtil_ut.at4_aat, __XmlUtil_ut.at4_bat, __XmlUtil_ut.at4_cat ].sort( ) );

        // Namespaces
        __XmlUtil_ut.at5 = <foo:order a="aat" b="bat" xmlns:foo="http://www.example2.org/"
        xmlns:fop="http://www.example.org/"
        fop:schemaLocation="http://www.example.org/fop/" c="cat">
        <foo:author foo="bar">Foo, Bar</foo:author>
        </foo:order>;
        __XmlUtil_ut.at5_aat = __XmlUtil_ut.at5.attribute( "a" );
        __XmlUtil_ut.at5_bat = __XmlUtil_ut.at5.attribute( "b" );
        __XmlUtil_ut.at5_cat = __XmlUtil_ut.at5.attribute( "c" );
        __XmlUtil_ut_ns1 = __XmlUtil_ut.ns1;
        __XmlUtil_ut.at5_sat = ( __XmlUtil_ut.at5 ).@__XmlUtil_ut_ns1::schemaLocation;
        delete this.__XmlUtil_ut_ns1;
        Assert.equal( "getAtttributes 5", "XmlUtil.getAttributes( __XmlUtil_ut.at5 ).sort()", [ __XmlUtil_ut.at5_aat, __XmlUtil_ut.at5_bat, __XmlUtil_ut.at5_cat, __XmlUtil_ut.at5_sat ].sort( ) );

        // Default namespace
        __XmlUtil_ut.at6 = <foo:order a="aat" b="bat" xmlns:foo="http://www.example2.org/"
        xmlns:fop="http://www.example.org/" xmlns="http://www.example3.org/"
        fop:schemaLocation="http://www.example.org/fop/" c="cat">
        <foo:author foo="bar">Foo, Bar</foo:author>
        </foo:order>;
        __XmlUtil_ut.at6_aat = __XmlUtil_ut.at6.attribute( "a" );
        __XmlUtil_ut.at6_bat = __XmlUtil_ut.at6.attribute( "b" );
        __XmlUtil_ut.at6_cat = __XmlUtil_ut.at6.attribute( "c" );
        __XmlUtil_ut_ns1 = __XmlUtil_ut.ns1;
        __XmlUtil_ut.at6_sat = ( __XmlUtil_ut.at6 ).@__XmlUtil_ut_ns1::schemaLocation;
        delete this.__XmlUtil_ut_ns1;
        Assert.equal( "getAtttributes 6", "XmlUtil.getAttributes( __XmlUtil_ut.at6 ).sort()", [ __XmlUtil_ut.at6_aat, __XmlUtil_ut.at6_bat, __XmlUtil_ut.at6_cat, __XmlUtil_ut.at6_sat ].sort( ) );

        ////////////////////////////////////////////////////////////////////////////////
        // Test prettyPrint
        // Simple stuff, no attributes, no namespaces.
        Assert.equal( "PrettyPrint 10", "XmlUtil.prettyPrint( <root><foo>bar</foo></root> )", "<root>\n  <foo>bar</foo>\n</root>" );
        Assert.equal( "PrettyPrint 20", "XmlUtil.prettyPrint( <root> bar </root> )", "<root> bar </root>" );
        Assert.equal( "PrettyPrint 21", "XmlUtil.prettyPrint( <root> </root> )", "<root> </root>" );
        Assert.equal( "PrettyPrint 22", "XmlUtil.prettyPrint( <root> \n </root> )", "<root> &#xA; </root>" );

        // By design, the pretty printer introduces line changes after non-text elements.
        Assert.equal( "PrettyPrint 23", "XmlUtil.prettyPrint( <root>foo<bar>foobar</bar></root> )", "<root>foo<bar>foobar</bar>\n</root>" );
        Assert.equal( "PrettyPrint 24", "XmlUtil.prettyPrint( <root>foo<bar><foo>bar</foo></bar></root> )", "<root>foo<bar>\n    <foo>bar</foo>\n  </bar>\n</root>" );
        Assert.equal( "PrettyPrint 25", "XmlUtil.prettyPrint( <root><bar><foo> bar </foo></bar>foo</root> )", "<root>\n  <bar>\n    <foo> bar </foo>\n  </bar>foo</root>" );
        Assert.equal( "PrettyPrint 26", "XmlUtil.prettyPrint( <root><bar><foo> bar </foo></bar>foo</root>, { indentLevel : 3} )", "<root>\n   <bar>\n      <foo> bar </foo>\n   </bar>foo</root>" );

        Assert.equal( "PrettyPrint 30", "XmlUtil.prettyPrint( <root><foo>bar</foo><bar></bar></root> )", "<root>\n  <foo>bar</foo>\n  <bar/>\n</root>" );

        // Attributes
        // diff improve puts attributes on seperate lines, if more than 2...
        Assert.equal( "PrettyPrint 40", "XmlUtil.prettyPrint( <root a='caat'   c='bcat' b='abat'><foo>bar</foo></root>, { diffImprove : true} )",
            "<root\n" +
            "    a=\"caat\"\n" +
            "    b=\"abat\"\n" +
            "    c=\"bcat\">\n" +
            "  <foo>bar</foo>\n" +
            "</root>" );
        // No diff improve
        Assert.equal( "PrettyPrint 41", "XmlUtil.prettyPrint( <root  a='aat'    b='bat' c='cat'><foo>bar</foo></root> )",
            "<root a=\"aat\" b=\"bat\" c=\"cat\">\n" +
            "  <foo>bar</foo>\n" +
            "</root>" );
        // No diff improve, because of limited attributes
        Assert.equal( "PrettyPrint 42", "XmlUtil.prettyPrint( <root  a='aat'  ><foo>bar</foo></root>, { diffImprove : true } )",
            "<root a=\"aat\">\n" +
            "  <foo>bar</foo>\n" +
            "</root>" );
        // More attributes
        Assert.equal( "PrettyPrint 43", "XmlUtil.prettyPrint( <root a='aat'   c='cat' b='bat'><foo fooaat='fooaatat'  \n foobat='foobatat'>bar</foo></root>, { diffImprove : true} )",
            '<root\n' +
            '    a="aat"\n' +
            '    b="bat"\n' +
            '    c="cat">\n' +
            '  <foo\n' +
            '      fooaat="fooaatat"\n' +
            '      foobat="foobatat">bar</foo>\n' +
            '</root>' );

        // Even more attributes
        Assert.equal( "PrettyPrint 44", "XmlUtil.prettyPrint( <root a='aat'   c='cat' b='bat'><foo fooaat='fooaatat'  \n foobat='foobatat'><bar baraat='baraatat'  \n barbat='barbatat'> horse...\nged </bar></foo></root>, { diffImprove : true} )",
            '<root\n' +
            '    a="aat"\n' +
            '    b="bat"\n' +
            '    c="cat">\n' +
            '  <foo\n' +
            '      fooaat="fooaatat"\n' +
            '      foobat="foobatat">\n' +
            '    <bar\n' +
            '        baraat="baraatat"\n' +
            '        barbat="barbatat"> horse...&#xA;ged </bar>\n' +
            '  </foo>\n' +
            '</root>' );

        // Namespaces
        Assert.equal( "PrettyPrint 50", 'XmlUtil.prettyPrint( <foo:order a="aat" xmlns:foo="http://www.example2.org/" b="bat" xmlns:fop="http://www.example.org/"        fop:schemaLocation="http://www.example.org/fop/" c="cat">' +
            '<foo:author fop:foo="bar">Foo, Bar</foo:author></foo:order>, { diffImprove : true} )',
            "<foo:order\n" +
            "    xmlns:foo=\"http://www.example2.org/\"\n" +
            "    xmlns:fop=\"http://www.example.org/\"\n" +
            "    a=\"aat\"\n" +
            "    b=\"bat\"\n" +
            "    c=\"cat\"\n" +
            "    fop:schemaLocation=\"http://www.example.org/fop/\">\n" +
            "  <foo:author fop:foo=\"bar\">Foo, Bar</foo:author>\n" +
            "</foo:order>" );

        Assert.equal( "PrettyPrint 51", 'XmlUtil.prettyPrint( <foo:order a="aat" xmlns:foo="http://www.example2.org/" b="bat" xmlns:fop="http://www.example.org/"   xmlns="http://www.example3.org/" c="cat">' +
            '<foo:author fop:foo="bar">Foo, Bar</foo:author><other>Something</other></foo:order>, { diffImprove : true} )',
            "<foo:order\n" +
            "    xmlns=\"http://www.example3.org/\"\n" +
            "    xmlns:foo=\"http://www.example2.org/\"\n" +
            "    xmlns:fop=\"http://www.example.org/\"\n" +
            "    a=\"aat\"\n" +
            "    b=\"bat\"\n" +
            "    c=\"cat\">\n" +
            "  <foo:author fop:foo=\"bar\">Foo, Bar</foo:author>\n" +
            "  <other>Something</other>\n" +
            "</foo:order>" );

        // Special characters
        // The SpiderMonkey engine handles transform \r to \n when XML literals, so we have to 
        // jump some hoops to get a \r into the code
        // It actually do work in output, its just that getting it into the XML is pretty 
        // hard.
        __XmlUtil_ut.special = "<>&\"'\u000A\u000D\u0009";
        // __XmlUtil_ut.special = "<>&\"'\u000A\u0009";
        // Do not split this line!:
        __XmlUtil_ut.special_xml = <order a={__XmlUtil_ut.special} xmlns:foo={__XmlUtil_ut.special} b="bat" xmlns:fop="http://www.example.org/"   xmlns="http://www.example3.org/" c="cat"></order>;
        __XmlUtil_ut.special_xml.other.* += __XmlUtil_ut.special + __XmlUtil_ut.special;
        Assert.equal( "PrettyPrint 60", 'XmlUtil.prettyPrint( __XmlUtil_ut.special_xml, { diffImprove : true} )',
            "<order\n" +
            "    xmlns=\"http://www.example3.org/\"\n" +
            "    xmlns:foo=\"&lt;&gt;&amp;&quot;&apos;&#xA;&#xD;&#x9;\"\n" +
            "    xmlns:fop=\"http://www.example.org/\"\n" +
            "    a=\"&lt;&gt;&amp;&quot;&apos;&#xA;&#xD;&#x9;\"\n" +
            "    b=\"bat\"\n" +
            "    c=\"cat\">\n" +
            "  <other>&lt;&gt;&amp;&quot;&apos;&#xA;&#xD;&#x9;&lt;&gt;&amp;&quot;&apos;&#xA;&#xD;&#x9;</other>\n" +
            "</order>" );

        /* It appears that Rhino and SpiderMonkey does not really support the special chars from xml 1.1... 
    __XmlUtil_ut.special = "<>&\"'\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000A\u000B\u000C\u000D\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F";
    Assert.equal( "PrettyPrint 61", 'XmlUtil.prettyPrint( <foo:order a={__XmlUtil_ut.special} xmlns:foo={__XmlUtil_ut.special} c="cat">' +
                  '<foo:bar>{__XmlUtil_ut.special}</foo:bar></foo:order>, { diffImprove : true} )', 
                  "<foo:order\n" + 
                  "    xmlns:foo=\"&lt;&gt;&amp;&quot;&apos;&#x1;&#x2;&#x3;&#x4;&#x5;&#x6;&#x7;&#x8;&#x9;&#xA;&#xB;&#xC;&#xD;&#xE;&#xF;&#x10;&#x11;&#x12;&#x13;&#x14;&#x15;&#x16;&#x17;&#x18;&#x19;&#x1A;&#x1B;&#x1C;&#x1D;&#x1E;&#x1F;\"\n" +
                  "    a=\"&lt;&gt;&amp;&quot;&apos;&#x1;&#x2;&#x3;&#x4;&#x5;&#x6;&#x7;&#x8;&#x9;&#xA;&#xB;&#xC;&#xD;&#xE;&#xF;&#x10;&#x11;&#x12;&#x13;&#x14;&#x15;&#x16;&#x17;&#x18;&#x19;&#x1A;&#x1B;&#x1C;&#x1D;&#x1E;&#x1F;\"\n" + 
                  "    c=\"cat\">\n" + 
                  "  <foo:bar>&lt;&gt;&amp;&quot;&apos;&#x1;&#x2;&#x3;&#x4;&#x5;&#x6;&#x7;&#x8;&#x9;&#xA;&#xB;&#xC;&#xD;&#xE;&#xF;&#x10;&#x11;&#x12;&#x13;&#x14;&#x15;&#x16;&#x17;&#x18;&#x19;&#x1A;&#x1B;&#x1C;&#x1D;&#x1E;&#x1F;</foo:bar>\n" +
                  "</foo:order>" ); 
    */
        /* Note about comments, cdata and preprocessing instructions:
       I have not been able find any way to actually create XML documents
       where such elements have been retained after beeing parsed by 
       the Rhino/SpiderMonkey E4X parsers. 
       - CDATA seems to be converted to character data
       - Comments seems to be totally ignored (disappear)
       - Preprocessing instructions go the way of the comments 
       Therefore the testing of these elements is quite limited */

        // CDATA: Make sure it does not become a tag in the output
        Assert.equal( "PrettyPrint 70", 'XmlUtil.prettyPrint( <root><tag>foo</tag><tag><![CDATA[<sender>John Smith</sender>]]></tag></root> )',
            "<root>\n" +
            "  <tag>foo</tag>\n" +
            "  <tag>&lt;sender&gt;John Smith&lt;/sender&gt;</tag>\n" +
            "</root>" );

        // Comments - no tests
        // Preprocessing instructions - no tests


        ////////////////////////////////////////////////////////////////////////////////
        // Test function to filter an XML List
        Assert.equal( "filterXMLListPredicate 1", "XmlUtil.filterXMLListPredicate( new XMLList( '<foo>hej</foo><foo>med</foo><foo>dig</foo>' ), function( elem ) { return true; })", new XMLList( '<foo>hej</foo><foo>med</foo><foo>dig</foo>' ) );
        Assert.equal( "filterXMLListPredicate 2", "XmlUtil.filterXMLListPredicate( new XMLList( '<foo>hej</foo><foo>med</foo><foo>dig</foo>' ), function( elem ) { return elem.text() == 'med'; })", new XMLList( '<foo>med</foo>' ) );
        Assert.equal( "filterXMLListPredicate 3", "XmlUtil.filterXMLListPredicate( new XMLList( '<foo>hej</foo><foo>med</foo><foo>dig</foo>' ), function( elem ) { return elem.text().indexOf( 'e' ) != -1; })", new XMLList( '<foo>hej</foo><foo>med</foo>' ) );
        Assert.equal( "filterXMLListPredicate 4", "XmlUtil.filterXMLListPredicate( new XMLList( '<foo>hej</foo><foo>med</foo><foo>dig</foo>' ), function( elem ) { return elem.text() == 'foo'; })", new XMLList( ) );

        ////////////////////////////////////////////////////////////////////////////////
        // Test function that determines if an Xml object have a given attribute
        Assert.equal( "hasAttribute 1", "XmlUtil.hasAttribute( <xml/>, undefined, 'atr' )", false );

        Assert.equal( "hasAttribute 2", "XmlUtil.hasAttribute( <xml atr='hej'/>, undefined, 'atr' )", true );
        Assert.equal( "hasAttribute 3", "XmlUtil.hasAttribute( <xml atr=''/>, undefined, 'atr' )", true );
        Assert.equal( "hasAttribute 4", "XmlUtil.hasAttribute( <xml atr='hej'/>, __XmlUtil_ut.ns1, 'atr' )", false );
        Assert.equal( "hasAttribute 5", "XmlUtil.hasAttribute( <xml atr=''/>, __XmlUtil_ut.ns1, 'atr' )", false );
        Assert.equal( "hasAttribute 6", "XmlUtil.hasAttribute( <xml atr='hej'/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 7", "XmlUtil.hasAttribute( <xml atr=''/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 8", "XmlUtil.hasAttribute( <xml atr='hej'/>, __XmlUtil_ut.ns1, 'atri' )", false );
        Assert.equal( "hasAttribute 9", "XmlUtil.hasAttribute( <xml atr=''/>, __XmlUtil_ut.ns1, 'atri' )", false );

        Assert.equal( "hasAttribute 10", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr='hej'/>, undefined, 'atr' )", false );
        Assert.equal( "hasAttribute 11", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr=''/>, undefined, 'atr' )", false );
        Assert.equal( "hasAttribute 12", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr='hej'/>, __XmlUtil_ut.ns1, 'atr' )", true );
        Assert.equal( "hasAttribute 13", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr=''/>, __XmlUtil_ut.ns1, 'atr' )", true );
        Assert.equal( "hasAttribute 14", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr='hej'/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 15", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr=''/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 16", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr='hej'/>, __XmlUtil_ut.ns1, 'atri' )", false );
        Assert.equal( "hasAttribute 17", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' fop:atr=''/>, __XmlUtil_ut.ns1, 'atri' )", false );

        Assert.equal( "hasAttribute 18", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej'/>, undefined, 'atr' )", false );
        Assert.equal( "hasAttribute 19", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr=''/>, undefined, 'atr' )", false );
        Assert.equal( "hasAttribute 20", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej'/>, __XmlUtil_ut.ns1, 'atr' )", false );
        Assert.equal( "hasAttribute 21", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr=''/>, __XmlUtil_ut.ns1, 'atr' )", false );
        Assert.equal( "hasAttribute 22", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej'/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 23", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr=''/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 24", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej'/>, __XmlUtil_ut.ns1, 'atri' )", false );
        Assert.equal( "hasAttribute 25", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr=''/>, __XmlUtil_ut.ns1, 'atri' )", false );

        Assert.equal( "hasAttribute 26", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej' atr='hejsa'/>, undefined, 'atr' )", true );
        Assert.equal( "hasAttribute 27", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='' atr='hejsa'/>, undefined, 'atr' )", true );
        Assert.equal( "hasAttribute 28", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej' atr='hejsa'/>, __XmlUtil_ut.ns1, 'atr' )", false );
        Assert.equal( "hasAttribute 29", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='' atr='hejsa'/>, __XmlUtil_ut.ns1, 'atr' )", false );
        Assert.equal( "hasAttribute 30", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej' fop:atr='hejsa'/>, __XmlUtil_ut.ns1, 'atr' )", true );
        Assert.equal( "hasAttribute 31", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='' fop:atr='hejsa'/>, __XmlUtil_ut.ns1, 'atr' )", true );
        Assert.equal( "hasAttribute 32", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej' fop:atri='hejsa'/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 33", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='' fop:atr='hejsa'/>, undefined, 'atri' )", false );
        Assert.equal( "hasAttribute 34", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='hej' fop:atri='hejsa'/>, __XmlUtil_ut.ns1, 'atri' )", true );
        Assert.equal( "hasAttribute 35", "XmlUtil.hasAttribute( <xml xmlns:foz='http://www.example2.org/' xmlns:fop='http://www.example.org/' foz:atr='' fop:atr='hejsa'/>, __XmlUtil_ut.ns1, 'atri' )", false );



        ////////////////////////////////////////////////////////////////////////////////
        // Test function to return elements from xmllist that have attributes of a given type.
        Assert.equal( "filterXMLListHasAttribute 1", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<foo>hej</foo><foo type=\"foo\">med</foo><foo>dig</foo>' ), undefined, 'type' )", new XMLList( '<foo type="foo">med</foo>' ) );
        Assert.equal( "filterXMLListHasAttribute 2", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<foo>hej</foo><foo>med</foo><foo type=\"foo\">dig</foo>' ), undefined, 'type' )", new XMLList( '<foo type="foo">dig</foo>' ) );
        Assert.equal( "filterXMLListHasAttribute 3", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<foo type=\"foo\">hej</foo><foo>med</foo><foo type=\"foo\">dig</foo>' ), undefined, 'type' )", new XMLList( '<foo type="foo">hej</foo><foo type="foo">dig</foo>' ) );

        Assert.equal( "filterXMLListHasAttribute NS 1", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\">hej</fop:foo>' ), undefined, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 2", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\">hej</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 3", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo>' ), undefined, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo>' ) );
        Assert.equal( "filterXMLListHasAttribute NS 4", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo>' ), undefined, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 5", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo>' ) );
        Assert.equal( "filterXMLListHasAttribute NS 6", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">med</fop:foo>' ) );
        Assert.equal( "filterXMLListHasAttribute NS 7", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" fop:typer=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo>' ) );
        Assert.equal( "filterXMLListHasAttribute NS 10", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example2.org/\" fop:type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo>' ) );
        Assert.equal( "filterXMLListHasAttribute NS 11", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example2.org/\" fop:type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns2, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example2.org/\" fop:type=\"bar\">med</fop:foo>' ) );
        Assert.equal( "filterXMLListHasAttribute NS 12", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" fop:type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns2, 'type' )", new XMLList( ) );

        Assert.equal( "filterXMLListHasAttribute NS 13", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 14", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 15", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" typer=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 16", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example2.org/\" type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 17", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example2.org/\" type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns2, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 18", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns2, 'type' )", new XMLList( ) );
        Assert.equal( "filterXMLListHasAttribute NS 19", "XmlUtil.filterXMLListHasAttribute( new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" type=\"bar\">hej</fop:foo><fop:foo xmlns:fop=\"http://www.example.org/\" xmlns:fip=\"http://www.example2.org/\" fip:type=\"hest\" fop:type=\"bar\">med</fop:foo>' ), __XmlUtil_ut.ns1, 'type' )", new XMLList( '<fop:foo xmlns:fop=\"http://www.example.org/\" xmlns:fip=\"http://www.example2.org/\" fip:type=\"hest\" fop:type=\"bar\">med</fop:foo>' ) );

        //////////////////////////////////////////////////////////////////////
        // Test function to get the first non-trivial subelement
        Assert.equal( "getFirstNonTrivialSubElement 1", "XmlUtil.getFirstNonTrivialSubElement( <foo></foo> )", undefined );
        Assert.equal( "getFirstNonTrivialSubElement 2", "XmlUtil.getFirstNonTrivialSubElement( <foo>bar</foo> )", undefined );
        Assert.equal( "getFirstNonTrivialSubElement 3", "XmlUtil.getFirstNonTrivialSubElement( <foo>bar<bar/></foo> )", <bar/> );
        Assert.equal( "getFirstNonTrivialSubElement 4", "XmlUtil.getFirstNonTrivialSubElement( <foo> <bar/></foo> )", <bar/> );
        Assert.equal( "getFirstNonTrivialSubElement 5", "XmlUtil.getFirstNonTrivialSubElement( <foo> <bar/> </foo> )", <bar/> );
        Assert.equal( "getFirstNonTrivialSubElement 6", "XmlUtil.getFirstNonTrivialSubElement( <foo><bar/> </foo> )", <bar/> );
        Assert.equal( "getFirstNonTrivialSubElement 7", "XmlUtil.getFirstNonTrivialSubElement( <foo> <bar><barre/></bar> </foo> )", <bar><barre/></bar> );
        Assert.equal( "getFirstNonTrivialSubElement 8", "XmlUtil.getFirstNonTrivialSubElement( <foo> <bar><barre/></bar> <bar><barrer/></bar> </foo> )", <bar><barre/></bar> );
        // And, with namespaces.
        Assert.equal( "getFirstNonTrivialSubElement NS 1", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'></fop:foo> )", undefined );
        Assert.equal( "getFirstNonTrivialSubElement NS 2", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'>bar</fop:foo> )", undefined );
        Assert.equal( "getFirstNonTrivialSubElement NS 3", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'>bar<fop:bar/></fop:foo> )", <fop:bar xmlns:fop='http://www.example.org/'/> );
        Assert.equal( "getFirstNonTrivialSubElement NS 4", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar/></fop:foo> )", <fop:bar xmlns:fop='http://www.example.org/'/> );
        Assert.equal( "getFirstNonTrivialSubElement NS 5", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar/> </fop:foo> )", <fop:bar xmlns:fop='http://www.example.org/'/> );
        Assert.equal( "getFirstNonTrivialSubElement NS 6", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> </fop:foo> )", <fop:bar xmlns:fop='http://www.example.org/'><fop:barre/></fop:bar> );
        Assert.equal( "getFirstNonTrivialSubElement NS 7", "XmlUtil.getFirstNonTrivialSubElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> <fop:bar><fop:barrer/></fop:bar> </fop:foo> )", <fop:bar xmlns:fop='http://www.example.org/'><fop:barre/></fop:bar> );

        //////////////////////////////////////////////////////////////////////
        // Test function to get the nt'h non-trivial subelement
        Assert.equal( "getNonTrivialChildElement 1", "XmlUtil.getNonTrivialChildElement( <foo></foo>, 0 )", undefined );
        Assert.equal( "getNonTrivialChildElement 2", "XmlUtil.getNonTrivialChildElement( <foo>bar</foo>, 0 )", undefined );
        Assert.equal( "getNonTrivialChildElement 3", "XmlUtil.getNonTrivialChildElement( <foo>bar<bar/></foo>, 0 )", <bar/> );
        Assert.equal( "getNonTrivialChildElement 4", "XmlUtil.getNonTrivialChildElement( <foo> <bar/></foo>, 0 )", <bar/> );
        Assert.equal( "getNonTrivialChildElement 5", "XmlUtil.getNonTrivialChildElement( <foo> <bar/> </foo>, 0 )", <bar/> );
        Assert.equal( "getNonTrivialChildElement 6", "XmlUtil.getNonTrivialChildElement( <foo><bar/> </foo>, 0 )", <bar/> );
        Assert.equal( "getNonTrivialChildElement 7", "XmlUtil.getNonTrivialChildElement( <foo> <bar><barre/></bar> </foo>, 0 )", <bar><barre/></bar> );
        Assert.equal( "getNonTrivialChildElement 8", "XmlUtil.getNonTrivialChildElement( <foo> <bar><barre/></bar> <bar><barrer/></bar> </foo>, 0 )", <bar><barre/></bar> );
        // And, with namespaces.
        Assert.equal( "getNonTrivialChildElement NS 1", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'></fop:foo>, 0 )", undefined );
        Assert.equal( "getNonTrivialChildElement NS 2", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'>bar</fop:foo>, 0 )", undefined );
        Assert.equal( "getNonTrivialChildElement NS 3", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'>bar<fop:bar/></fop:foo>, 0 )", <fop:bar xmlns:fop='http://www.example.org/'/> );
        Assert.equal( "getNonTrivialChildElement NS 4", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar/></fop:foo>, 0 )", <fop:bar xmlns:fop='http://www.example.org/'/> );
        Assert.equal( "getNonTrivialChildElement NS 5", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar/> </fop:foo>, 0 )", <fop:bar xmlns:fop='http://www.example.org/'/> );
        Assert.equal( "getNonTrivialChildElement NS 6", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> </fop:foo>, 0 )", <fop:bar xmlns:fop='http://www.example.org/'><fop:barre/></fop:bar> );
        Assert.equal( "getNonTrivialChildElement NS 7", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> <fop:bar><fop:barrer/></fop:bar> </fop:foo>, 0 )", <fop:bar xmlns:fop='http://www.example.org/'><fop:barre/></fop:bar> );
        // Get another element.
        Assert.equal( "getNonTrivialChildElement N1", "XmlUtil.getNonTrivialChildElement( <foo></foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N2", "XmlUtil.getNonTrivialChildElement( <foo>bar</foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N3", "XmlUtil.getNonTrivialChildElement( <foo>bar<bar/></foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N4", "XmlUtil.getNonTrivialChildElement( <foo> <bar/></foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N5", "XmlUtil.getNonTrivialChildElement( <foo> <bar/> </foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N6", "XmlUtil.getNonTrivialChildElement( <foo><bar/> </foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N7", "XmlUtil.getNonTrivialChildElement( <foo> <bar><barre/></bar> </foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N8", "XmlUtil.getNonTrivialChildElement( <foo> <bar><barre/></bar> <bar><barrer/></bar> </foo>, 1 )", <bar><barrer/></bar> );
        Assert.equal( "getNonTrivialChildElement N9", "XmlUtil.getNonTrivialChildElement( <foo> <bar/> <bar><barrer/></bar> <fisk1/> </foo>, 2 )", <fisk1/> );
        Assert.equal( "getNonTrivialChildElement N10", "XmlUtil.getNonTrivialChildElement( <hep> <fisk1/> <fisk2/></hep>, 1 )", <fisk2/> );
        Assert.equal( "getNonTrivialChildElement N11", "XmlUtil.getNonTrivialChildElement( <hep> <fisk1/> <fisk2/> <fisk3/></hep>, 2 )", <fisk3/> );

        // And, with namespaces.
        Assert.equal( "getNonTrivialChildElement N NS 1", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'></fop:foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N NS 2", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'>bar</fop:foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N NS 3", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'>bar<fop:bar/></fop:foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N NS 4", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar/></fop:foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N NS 5", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar/> </fop:foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N NS 6", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> </fop:foo>, 1 )", undefined );
        Assert.equal( "getNonTrivialChildElement N NS 7", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> <fop:bar><fop:barrer/></fop:bar> </fop:foo>, 1 )", <fop:bar xmlns:fop='http://www.example.org/'><fop:barrer/></fop:bar> );
        Assert.equal( "getNonTrivialChildElement N NS 8", "XmlUtil.getNonTrivialChildElement( <fop:foo xmlns:fop='http://www.example.org/'> <fop:bar><fop:barre/></fop:bar> <fop:bar><fop:barrer/></fop:bar> <fop:fisk1><fop:fisk2/></fop:fisk1></fop:foo>, 2 )", <fop:fisk1 xmlns:fop='http://www.example.org/'><fop:fisk2/></fop:fisk1> );


        //////////////////////////////////////////////////////////////////////
        // Test function to determine of a given element has toplevel type matching a namespace uri and local name.

        Assert.equal( "hasUriLocalName 1", "XmlUtil.hasUriLocalName( <foo/>, undefined, 'foo')", true );
        Assert.equal( "hasUriLocalName 2", "XmlUtil.hasUriLocalName( <foo/>, undefined, 'bar')", false );
        Assert.equal( "hasUriLocalName NS 1", "XmlUtil.hasUriLocalName( <fop:foo xmlns:fop='http://www.example.org/'/>, __XmlUtil_ut.ns1, 'foo')", true );
        Assert.equal( "hasUriLocalName NS 2", "XmlUtil.hasUriLocalName( <fop:foo xmlns:fop='http://www.example.org/'/>, undefined, 'foo')", false );
        Assert.equal( "hasUriLocalName NS 3", "XmlUtil.hasUriLocalName( <bar:foo xmlns:bar='http://www.example.org/'/>, undefined, 'foo')", false );
        Assert.equal( "hasUriLocalName NS 4", "XmlUtil.hasUriLocalName( <bar:foo xmlns:bar='http://www.example.org/'/>, __XmlUtil_ut.ns1, 'foo')", true );
        Assert.equal( "hasUriLocalName NS 5", "XmlUtil.hasUriLocalName( <bar:foo xmlns:bar='http://www.example.org/'/>, __XmlUtil_ut.ns1, 'bar')", false );
        Assert.equal( "hasUriLocalName NS 6", "XmlUtil.hasUriLocalName( <fop:foo xmlns:fop='http://www.example.org/'/>, __XmlUtil_ut.ns2, 'foo')", false );
        Assert.equal( "hasUriLocalName NS 7", "XmlUtil.hasUriLocalName( <bar:foo xmlns:bar='http://www.example.org/'/>, __XmlUtil_ut.ns2, 'foo')", false );
        Assert.equal( "hasUriLocalName NS 8", "XmlUtil.hasUriLocalName( <bar:foo xmlns:bar='http://www.example.org/'/>, __XmlUtil_ut.ns2, 'bar')", false );

        //////////////////////////////////////////////////////////////////////
        // Test function for matching root and elemen
        Assert.equal( "matchesRootAndElement 1", "XmlUtil.matchesRootAndElement( <foo/>, undefined, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement 2", "XmlUtil.matchesRootAndElement( <foo><bar/></foo>, undefined, 'foo', 'bar' )", true );
        Assert.equal( "matchesRootAndElement 3", "XmlUtil.matchesRootAndElement( <foo><bar/></foo>, undefined, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement 4", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, undefined, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement 5", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, undefined, 'foo', 'bar' )", true );
        Assert.equal( "matchesRootAndElement 6", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, undefined, 'bar', 'baz' )", false );
        Assert.equal( "matchesRootAndElement 7", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, undefined, 'bar', 'foo' )", false );

        Assert.equal( "matchesRootAndElement NNSNS 1", "XmlUtil.matchesRootAndElement( <foo/>, __XmlUtil_ut.ns1, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NNSNS 2", "XmlUtil.matchesRootAndElement( <foo><bar/></foo>, __XmlUtil_ut.ns1, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NNSNS 3", "XmlUtil.matchesRootAndElement( <foo><bar/></foo>, __XmlUtil_ut.ns1, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NNSNS 4", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, __XmlUtil_ut.ns1, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NNSNS 5", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, __XmlUtil_ut.ns2, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NNSNS 6", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, __XmlUtil_ut.ns2, 'bar', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NNSNS 7", "XmlUtil.matchesRootAndElement( <foo><bar><baz/></bar></foo>, __XmlUtil_ut.ns1, 'bar', 'foo' )", false );

        Assert.equal( "matchesRootAndElement NSNS 1", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'/>, __XmlUtil_ut.ns1, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NSNS 2", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar/></fop:foo>, __XmlUtil_ut.ns1, 'foo', 'bar' )", true );
        Assert.equal( "matchesRootAndElement NSNS 3", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><bar/></fop:foo>, __XmlUtil_ut.ns1, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NSNS 4", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar/></fop:foo>, __XmlUtil_ut.ns1, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NSNS 5", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns1, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NSNS 6", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns1, 'foo', 'bar' )", true );
        Assert.equal( "matchesRootAndElement NSNS 7", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns1, 'bar', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NSNS 8", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns1, 'bar', 'foo' )", false );
        Assert.equal( "matchesRootAndElement NSNS 9", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'/>, __XmlUtil_ut.ns1, 'foo', 'bar' )", false );

        Assert.equal( "matchesRootAndElement NSNS 10", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar/></fop:foo>, __XmlUtil_ut.ns2, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NSNS 11", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example2.org/'><fop:bar/></fop:foo>, __XmlUtil_ut.ns2, 'foo', 'bar' )", true );
        Assert.equal( "matchesRootAndElement NSNS 12", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><bar/></fop:foo>, __XmlUtil_ut.ns2, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NSNS 13", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar/></fop:foo>, __XmlUtil_ut.ns2, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NSNS 14", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns2, 'foo', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NSNS 15", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns2, 'foo', 'bar' )", false );
        Assert.equal( "matchesRootAndElement NSNS 16", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns2, 'bar', 'baz' )", false );
        Assert.equal( "matchesRootAndElement NSNS 17", "XmlUtil.matchesRootAndElement( <fop:foo xmlns:fop='http://www.example.org/'><fop:bar><fop:baz/></fop:bar></fop:foo>, __XmlUtil_ut.ns2, 'bar', 'foo' )", false );

        delete this.__XmlUtil_ut;
    } );


// This is part of commit
