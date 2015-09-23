====================================
dataIO job-processor design dokument
====================================

.. |date| date::

:author: jbn
:date: |date|

.. header::

    .. class:: headertable

    +---------------+---------------------+---+
    |               |.. class:: centered  |   |
    |               |                     |   |
    |Side ###Page###|Afsnit  ###Section###|   |
    +---------------+---------------------+---+

.. contents::

.. section-numbering::

.. raw:: pdf

   PageBreak oneColumn

Formålet med dette dokument er at give en beskrivelse af dataIO systemets
job-processor komponent.

Beskrivelse
===========

Job-processor komponentens funktion er at udføre en eller flere på hinanden
følgende transformationer på alle items i en given chunk (se job-store design
dokument for beskrivelse af chunk og item begreberne). Resultatet af den sidste
transformation for hvert item sendes tilbage til job-store som resultat af
**processing** fasen.

Teknisk beskrivelse
~~~~~~~~~~~~~~~~~~~

Transformationerne udtrykkes som flows bestående af flowkomponenter.
Den enkelte flowkomponent repræsenterer forretningslogik implementeret i
JavaScript (se flow-store design dokument for beskrivelse af flow
begreberne).

**Her mangler beskrivelse af grænsesnit mellem platform og forretningslogik
(fastlægges i forbindelse med US#430)**

Al logning fra forretningslogikken gemmes automatisk i log-store servicen.

Drift beskrivelse
~~~~~~~~~~~~~~~~~

Job-processor komponenten pakkes og distribueres som et Java EE7 EAR arkiv.

For at kunne sende resultater tilbage til job-store og for at kunne fremhente
flow ressourcer knyttet til et givent job forudsættes det, at der i
applikationsserveren eksisterer en custom string resource med JNDI navn::

    url/dataio/jobstore/rs

hvis værdi skal være en URL, der peger på **job-store** komponentens RESTful
API.

For at kunne persistere log events for forretningslogikken fordrer denne
komponent, at der i applikationsserveren forefindes en aktiv JDBC resource med
nedenstående JNDI navn, som peger på logstore databasen::

    jdbc/dataio/logstore

Bemærk desuden at databasen skal være oprettet med tabeller inden
processeringen påbegynder sin logning.

For at konfigurere processorens logning skal der i applikationsserveren
eksistere en custom string resource med JNDI navn::

    url/dataio/jobprocessor/logback

hvis værdi skal være en URL, der peger på en logback *include* blok, der
som minimum skal indeholde en appender med følgende konfiguration::

  <appender name="LOGSTORE"
    class="dk.dbc.dataio.logstore.logback.LogStoreMergingJdbcAppender">
    <filter class="dk.dbc.dataio.logstore.logback.MdcKeyExistsFilter">
      <MDCKey>logStoreTrackingId</MDCKey>
      <OnMatch>ACCEPT</OnMatch>
      <onMismatch>DENY</onMismatch>
    </filter>

    <connectionSource class="ch.qos.logback.core.db.JNDIConnectionSource">
      <jndiLocation>jdbc/dataio/logstore</jndiLocation>
    </connectionSource>
  </appender>
