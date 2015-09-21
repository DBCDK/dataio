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

/** @file Module that adds support for xml conversions of Record objects. */

// Only very limited internal error checking is performed. Basically, it is 
// assumed, that the XML input is legal. If not, weird results may occur.

use( "XmlNamespaces" ); // Provides XML namespaces
use( "XmlUtil" ); // Provides XML creation functions
use( "MarcClasses" ); // Need the basic record classes.
use( "Marc" ); // This module uses the eachField method from Marc
use( "UnitTest" ); // UnitTest support

EXPORTED_SYMBOLS = [ "MarcXchange" ];

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////


/**
 * Module to handle conversion from a marc record represented as MarcXchange XML to a 
 * Javascript Marc Record Object (as defined in the MarcClasses module) or the other way around. 
 *
 * The module contains two methods marcXchangeToMarcRecord and marcRecordToMarcXchange.
 * 
 * @type {namespace}
 * @name MarcXchange
 * @see MarcClasses
 * @namespace */
var MarcXchange = function( ) {

    // The object returned.
    var that = {};
    // Shorthand for some namespaces.
    var marcx = XmlNamespaces.marcx;
    var mx = XmlNamespaces.mx;


    /**
     * Convert a MarcXchange XML string or object to a Javascript Marc Record Object.
     *
     * This function take a string with an MarcXChange XML object, and converts it to a Record object.
     *
     * **Note:** This function at the moment only handles input from danMARC2 records represented as
     * MarcXchange. If a marc21 MarcXchange record is passed to the function, control fields and the information
     * from the leader will not be represented in the output Record object
     * use marcXchangeM21ToMarcRecord to do this
     * 
     * @type {method}
     * @method
     * @syntax MarcXchange.marcXchangeToMarcRecord( xml )
     * @param {string|XML} xml String or XML representation of an marcXchange record
     * @return {Record} JavaScript Marc Object
     * @see Record
     * @example
// Create a Marc Record object from an xml MarcXChange string.     
MarcXchange.marcXchangeToMarcRecord( xml )
     * @name MarcXchange.marcXchangeToMarcRecord */
    that.marcXchangeToMarcRecord = function( xml ) {

        // prevent warn from XmlUtil if allready xml
        if ( typeof( xml ) === 'xml' ) {
            var inputXml = xml;
        } else {
            var inputXml = XmlUtil.fromString( xml );
        }

        var record = new Record( );

        var child;
        var subchild;
        var field;
        var subfield;

        //Log.debug("marcXchangeToMarcRecord:", inputXml);

        for each( child in inputXml.marcx::datafield ) {
            //Log.debug( "Found child: " + child.toXMLString() );
            //Log.debug( "Creating field: " + child.@tag + ", " + child.@ind1 + child.@ind2 );
            field = new Field( String( child.@tag ), String( child.@ind1 ) + String( child.@ind2 ) );
            for each( subchild in child.marcx::subfield ) {
                //Log.debug( "Found subchild: " + subchild.toXMLString() );
                //Log.debug( "Creating subfield: " + subchild.@code + ", " + subchild );
                subfield = new Subfield( String( subchild.@code ), String( subchild ) );
                field.append( subfield );
            }
            //Log.debug( field + "\n" );
            record.append( field );
        }

        Log.debug( "Returned record is:\n" + record ); // all field numbers starts in position 0
        return record;
    };

    /**
     * Convert a MarcXchange XML string with MARC21 record to a Javascript Marc Record Object.
     *
     *
     * @type {method}
     * @method
     * @syntax MarcXchange.marcXchangeToMarcRecord( xml )
     * @param {string|XML} xml String or XML representation of an marcXchange record
     * @return {Record} JavaScript Marc Object
     * @see Record
     * @example
    **/
    that.marcXchangeM21ToMarcRecord = function( xml ) {

        // prevent warn from XmlUtil if allready xml
        if ( typeof( xml ) === 'xml' ) {
            var inputXml = xml;
        } else {
            var inputXml = XmlUtil.fromString( xml );
        }

        var record = new Record( );

        var child;
        var subchild;
        var field;
        var subfield;
        var controlTag = false;
        
        try {
            var recordFormat = String(inputXml.@format); 
            if (recordFormat !== "MARC21") {
                throw new Error ("marcXchangeM21ToMarcRecord : don't know how to convert record Format : = ", recordFormat );
            }
            //Log.debug("marcXchangeM21ToMarcRecord:", inputXml);
            var leader = String(inputXml.mx::leader);
            Log.debug("Leader:", leader);
            record.recordStatus = leader.slice( 5, 6 );
            record.implementationCodes = leader.slice( 6, 10 );
            record.forUserSystems = leader.slice( 17, 20 );
            
            for each( child in inputXml.mx::controlfield ) {
                //Log.debug( "Found child: " + child.toXMLString() );
                Log.debug( "Creating field: " + child.@tag + ", " + child.@ind1 + child.@ind2 );
                field = new Field( String( child.@tag ), "" );
                field.value = String(child);
                    Log.debug( "Found child: ", child.toXMLString() );
                    Log.debug( "Created field: ",  field );
                record.append( field );
            }
            for each( child in inputXml.mx::datafield ) {
                //Log.debug( "Found child: " + child.toXMLString() );
                field = new Field( String( child.@tag ), String( child.@ind1 ) + String( child.@ind2 ) );
                for each( subchild in child.mx::subfield ) {
                    subfield = new Subfield( String( subchild.@code ), String( subchild ) );
                    field.append( subfield );
                }
                //Log.debug( field + "\n" );
                record.append( field );
            }
            Log.debug( "Returned record is:\n" + record ); // all field numbers starts in position 0
            return record;
        } finally {
            Log.trace( "Leaving marcXchangeM21ToMarcRecord");
        }
    };


    /**
     * Convert a Javascript Marc Record Object to a MarcXchange XML object.
     *
     * This function creates an XML object in MarcXChange format,
     * representing the Record object passed to it.
     *
     * The format parameter controls if the generated MarcXChange XML
     * reflects a normal Danmarc2 record, or an MARC21 record. This
     * influences the namespace of the XML, and is also set in the
     * `format` attribute of the resulting XML.
     * 
     * The type parameter contains information about the level of the record
     * which is specifically relevant for danMARC2 records as a record can either be 
     * a main, section or volume description of a record. 
     * The possible input for the type parameter are:
     * 
     * * "Bibliographic" (single record or a merged version of a main+section+volume record)
     * * "BibliographicMain" (main record)
     * * "BibliographicSection" (section record)
     * * "BibliographicVolume" (volume record)
     * 
     * @type {method}
     * @method
     * @see Record
     * @syntax MarcXchange.marcRecordToMarcXchange( record, format, type )
     * @param {Record} record A marc record object
     * @param {string} format marcXchange format of the record, either "MARC21" or "danMARC2".
     * @param {string} type marcXchange type of the record, either "Bibliographic", "BibliographicMain", "BibliographicSection" or "BibliographicVolume".
     * @return {XML} An XML object containing a marcXchange record
     * @example
// Convert an Marc classes record instance to an XML instance.
var x = MarcXchange.marcRecordToMarcXchange( some_record_object );
     * @name MarcXchange.marcRecordToMarcXchange */
    that.marcRecordToMarcXchange = function( record, format, type ) {
        Log.info( "Entering: marcRecordToMarcXchange function" );
        // This will be the result of the function.
        var xml = new XML( "<record/>" );

        // Choose the namespace based on the format parameter.
        if ( format === "MARC21" ) {
            var ns = mx;
        } else {
            var ns = marcx;
        }
        xml.setNamespace( ns );
        xml.@format = format;
        xml.@type = type;
        var leader = "00000" + record.recordStatus + record.implementationCodes + "2200000" + record.forUserSystems + "4500"; //00000nam a22000000c 4500
        xml.ns::leader = leader;

        var element;

        record.eachField( /./, function( field ) {
                if ( field.isControlField( ) ) {
                    // Control fields only have a name and a value.
                    element = <controlfield/>;
                    element.setNamespace( ns );
                    element.@tag = field.name;
                    element.* += field.value;
                } else {
                    // Normal fields have name indicator and subfields
                    element = <datafield/>;
                    element.setNamespace( ns );
                    element.@tag = field.name;
                    element.@ind1 = field.indicator.charAt( 0 );
                    element.@ind2 = field.indicator.charAt( 1 );
                    field.eachSubField( /./, function( field, subField ) {
                            var subfield = <subfield/>;
                            subfield.setNamespace( ns );
                            subfield.@code += subField.name;
                            subfield.* += subField.value;
                            element.* += subfield;
                        } );
                }
                xml.* += element;
            } );

        Log.info( "Leaving: marcRecordToMarcXchange function" );

        return xml;

    };

    // Finally, return the object that will become the MarcXchange object.
    return that;
}( );


// Unittest, based on testmarcclasses.js script, from june 2012
UnitTest.addFixture( "marc.MarcXchange module, marcXchangeToMarcRecord", function( ) {
        // handle namespaces
        var dkabm = XmlNamespaces.dkabm;
        var ac = XmlNamespaces.ac;
        var dkdcplus = XmlNamespaces.dkdcplus;
        var oss = XmlNamespaces.oss;
        var dc = XmlNamespaces.dc;
        var dcterms = XmlNamespaces.dcterms;
        var xsi = XmlNamespaces.xsi;
        var marcx = XmlNamespaces.marcx;
        var of = XmlNamespaces.of;
        var ofo = XmlNamespaces.ofo;
        var bibdk = XmlNamespaces.bibdk;
        var os = XmlNamespaces.os;

        var input = new XML( "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\
  <marcx:datafield tag=\"001\" ind1=\"0\" ind2=\"0\">\
  <marcx:subfield code=\"a\">21921173</marcx:subfield><marcx:subfield code=\"b\">870970</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"009\" ind1=\"0\" ind2=\"0\">\
  <marcx:subfield code=\"a\">a</marcx:subfield><marcx:subfield code=\"g\">xx</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"021\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">87-11-15086-6</marcx:subfield>\
  <marcx:subfield code=\"c\">hf.</marcx:subfield><marcx:subfield code=\"d\">kr. 248,00</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"100\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">Engell</marcx:subfield><marcx:subfield code=\"h\">Hans</marcx:subfield>\
  <marcx:subfield code=\"4\">aut</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"245\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">P\u00e5 Slotsholmen</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"250\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">2. udgave</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"260\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">[Kbh.]</marcx:subfield>\
  <marcx:subfield code=\"b\">Aschehoug</marcx:subfield><marcx:subfield code=\"k\">Norhaven, Viborg</marcx:subfield>\
  <marcx:subfield code=\"c\">1997</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"300\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">455 sider</marcx:subfield>\
  <marcx:subfield code=\"b\">ill.</marcx:subfield></marcx:datafield>\
  <marcx:datafield tag=\"526\" ind1=\"0\" ind2=\"0\">\
  <marcx:subfield code=\"i\">Samh\u00f8rende</marcx:subfield><marcx:subfield code=\"t\">P\u00e5 Slotsholmen</marcx:subfield>\
  <marcx:subfield code=\"t\">Farvel til Slotsholmen</marcx:subfield></marcx:datafield>\
  </marcx:record>" );

        var tostringoutput = "001 00 *a 21921173 *b 870970 \n" +
            "009 00 *a a *g xx \n" +
            "021 00 *a 87-11-15086-6 *c hf. *d kr. 248,00 \n" +
            "100 00 *a Engell *h Hans *4 aut \n" +
            "245 00 *a P\xE5 Slotsholmen \n" +
            "250 00 *a 2. udgave \n" +
            "260 00 *a [Kbh.] *b Aschehoug *k Norhaven, Viborg *c 1997 \n" +
            "300 00 *a 455 sider *b ill. \n" +
            "526 00 *i Samh\xF8rende *t P\xE5 Slotsholmen *t Farvel til Slotsholmen \n";

        // Create record
        __marcXchangeToMarcRecord__r = MarcXchange.marcXchangeToMarcRecord( input );
        Assert.equal( "toString", '__marcXchangeToMarcRecord__r.toString()', tostringoutput );
        delete __marcXchangeToMarcRecord__r;

    } );

UnitTest.addFixture( "marc.MarcXchange module, marcRecordToMarcXchange", function( ) {

        record = new Record( );
        field = new Field( "001", "00" );
        subfield = new Subfield( "a", "27907431" );
        field.append( subfield );
        subfield = new Subfield( "b", "870970" );
        field.append( subfield );
        record.append( field );

        record.recordStatus = "0";
        record.implementationCodes = "0000";
        record.forUserSystems = "000";

        submitter = "710100";




        output = new XML( '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">\
<marcx:leader>000000000022000000004500</marcx:leader>\
<marcx:datafield tag="001" ind1="0" ind2="0">\
<marcx:subfield code="a">27907431</marcx:subfield>\
<marcx:subfield code="b">870970</marcx:subfield>\
</marcx:datafield>\
</marcx:record>' );

        format = "danMARC2";
        type = "Bibliographic"

        marcx = XmlNamespaces.marcx;

        Assert.equal( "marcXchange from danmarc2 marc object", 'XmlUtil.fromString(String(MarcXchange.marcRecordToMarcXchange( record, format, type )))', XmlUtil.fromString( output.toString( ) ) );
        //The construction with from xml to string back to xml is only added ensure that the xml is constructed in the same way in dbc-jsshell and jscommon-shell - problem with sorting of attributes and namespace


        delete this.xml;
        delete this.record;
        delete this.field;
        delete this.subfield;
        delete this.submitter;
        delete this.output;
        delete this.marcx;
        delete this.format;
        delete this.type;

    } );

UnitTest.addFixture( "marc.MarcXchange module, marcRecordToMarcXchange", function( ) {

        xml = XmlUtil.fromString( "<dkabm:record xmlns:dkabm=\"http://biblstandard.dk/abm/namespace/dkabm/\"></dkabm:record>" );
        marc21Record = new Record( );

        marc21Field = new Field( "001", "" );
        marc21Field.value = "27982603";
        marc21Record.append( marc21Field );

        marc21Field = new Field( "008", "" );
        marc21Field.value = "091027s2009    dk |||||| ||| 00||udan||";
        marc21Record.append( marc21Field );

        marc21Field = new Field( "020", "  " );
        marc21Subfield = new Subfield( "a", "9788776795214 (hf.)" );
        marc21Field.append( marc21Subfield );
        marc21Subfield = new Subfield( "c", "kr. 99,95" );
        marc21Field.append( marc21Subfield );
        marc21Record.append( marc21Field );

        marc21Field = new Field( "245", "10" );
        marc21Subfield = new Subfield( "a", "Asterix og Obelix foedselsdag : " );
        marc21Field.append( marc21Subfield );
        marc21Subfield = new Subfield( "b", "den gyldne gaestebog" );
        marc21Field.append( marc21Subfield );
        marc21Record.append( marc21Field );

        marc21Field = new Field( "260", "  " );
        marc21Subfield = new Subfield( "b", "Egmont Serieforlaget, " );
        marc21Field.append( marc21Subfield );
        marc21Subfield = new Subfield( "c", "2009" );
        marc21Field.append( marc21Subfield );
        marc21Record.append( marc21Field );

        marc21Field = new Field( "700", "1 " );
        marc21Subfield = new Subfield( "a", "Uderzo, Albert" );
        marc21Field.append( marc21Subfield );
        marc21Subfield = new Subfield( "4", "aut" );
        marc21Field.append( marc21Subfield );
        marc21Subfield = new Subfield( "4", "drm" );
        marc21Field.append( marc21Subfield );
        marc21Record.append( marc21Field );

        marc21Record.recordStatus = "n";
        marc21Record.implementationCodes = "am a";
        marc21Record.forUserSystems = "0c ";

        format = "MARC21";
        type = "Bibliographic"

        //mx = new Namespace ("mx", "http://www.loc.gov/MARC21/slim");
        mx = XmlNamespaces.mx;

        output = new XML( '<record format="MARC21" type="Bibliographic"></record>' );
        output.setNamespace( mx );
        leader = new XML( '<leader>00000nam a22000000c 4500</leader>' );
        leader.setNamespace( mx );
        output.appendChild( leader );

        controlfield = new XML( '<controlfield tag="001">27982603</controlfield>' );
        controlfield.setNamespace( mx );
        output.appendChild( controlfield );

        controlfield = new XML( '<controlfield tag="008">091027s2009    dk |||||| ||| 00||udan||</controlfield>' );
        controlfield.setNamespace( mx );
        output.appendChild( controlfield );

        datafield = new XML( '<mx:datafield xmlns:mx="http://www.loc.gov/MARC21/slim">\
<mx:subfield code="a">9788776795214 (hf.)</mx:subfield>\
<mx:subfield code="c">kr. 99,95</mx:subfield>\
</mx:datafield>' );
        //datafield.setNamespace( mx );
        datafield.@tag  = "020"; 
        datafield.@ind1 = " ";
        datafield.@ind2 = " ";
        output.appendChild( datafield );

        datafield = new XML( '<mx:datafield xmlns:mx="http://www.loc.gov/MARC21/slim">\
<mx:subfield code="a">Asterix og Obelix foedselsdag : </mx:subfield>\
<mx:subfield code="b">den gyldne gaestebog</mx:subfield>\
</mx:datafield>' );
        //datafield.setNamespace( mx );
        datafield.@tag = "245";
        datafield.@ind1 = "1";
        datafield.@ind2 = "0";
        output.appendChild( datafield );

        datafield = new XML( '<mx:datafield xmlns:mx="http://www.loc.gov/MARC21/slim">\
<mx:subfield code="b">Egmont Serieforlaget, </mx:subfield>\
<mx:subfield code="c">2009</mx:subfield>\
</mx:datafield>' );
        //datafield.setNamespace( mx );
        datafield.@tag = "260";
        datafield.@ind1 = " ";
        datafield.@ind2 = " ";
        output.appendChild( datafield );

        datafield = new XML( '<mx:datafield xmlns:mx="http://www.loc.gov/MARC21/slim" >\
<mx:subfield code="a">Uderzo, Albert</mx:subfield>\
<mx:subfield code="4">aut</mx:subfield>\
<mx:subfield code="4">drm</mx:subfield>\
</mx:datafield>' );
        //datafield.setNamespace( mx );
        datafield.@tag  = "700"
        datafield.@ind1 = "1";
        datafield.@ind2 = " ";
        output.appendChild( datafield );
        Log.debug("xml=", output.toXMLString());
        Assert.equal( "marcXchange from marc21 marc object", 'MarcXchange.marcRecordToMarcXchange( marc21Record, format, type )', output );
        
        // test for reversing the process
        Assert.equal( "marc21 marcXchange to marc object", 'MarcXchange.marcXchangeM21ToMarcRecord( output, format, type )',  marc21Record);

        delete this.marc21Record;
        delete this.marc21Field;
        delete this.marc21Subfield;
        delete this.controlfield;
        delete this.datafield;
        delete this.xml;
        delete this.format;
        delete this.type;
        delete this.leader;
        delete this.output;
        delete this.mx;

    } );

UnitTest.addFixture( "marc.MarcXchange module, marcRecordToMarcXchange and marcXchangeToMarcRecord", function( ) {

        record = new Record( );
        field = new Field( "001", "00" );
        subfield = new Subfield( "a", "27907431" );
        field.append( subfield );
        subfield = new Subfield( "b", "870970" );
        field.append( subfield );
        record.append( field );
        submitter = "710100";

        format = "danMARC2";
        type = "Bibliographic"

        marcx = XmlNamespaces.marcx;

        tmpOutput = MarcXchange.marcRecordToMarcXchange( record, format, type );

        Assert.equal( "from danmarc2 marc record object to marcXchange back to marc record object", 'MarcXchange.marcXchangeToMarcRecord(tmpOutput).toString()', record.toString( ) );

        marcXchange = new XML( '<marcx:record format="danMARC2" type="Bibliographic" xmlns:marcx="info:lc/xmlns/marcxchange-v1">\
<marcx:leader>00000c    2200000   4500</marcx:leader>\
<marcx:datafield tag="001" ind1="0" ind2="0">\
<marcx:subfield code="a">27907431</marcx:subfield>\
<marcx:subfield code="b">870970</marcx:subfield>\
</marcx:datafield>\
</marcx:record>' );

        tmpOutput = MarcXchange.marcXchangeToMarcRecord( marcXchange );

        Assert.equal( "from marcXchange to danmarc2 marc record object back to marcXchange", 'XmlUtil.fromString(String(MarcXchange.marcRecordToMarcXchange(tmpOutput, format, type)))', XmlUtil.fromString( marcXchange.toString( ) ) );
        //The construction with from xml to string back to xml is only added ensure that the xml is constructed in the same way in dbc-jsshell and jscommon-shell - problem with sorting of attributes and namespace


        delete this.xml;
        delete this.record;
        delete this.field;
        delete this.subfield;
        delete this.submitter;
        delete this.marcXchange;
        delete this.marcx;
        delete this.tmpOutput;
        delete this.format;
        delete this.type;

    } );
