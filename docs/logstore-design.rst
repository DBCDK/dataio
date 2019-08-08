================================
dataIO log-store design dokument
================================

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
logstore komponent.

Beskrivelse
===========

Logstore komponentens funktion er at indsamle log omkring dataIO systemets
forretningslogik-baserede processering samt at forsyne data-drift personalet
med en mulighed for at tilgå disse log data på nem og hurtig vis.

Teknisk beskrivelse
~~~~~~~~~~~~~~~~~~~

Log data indsamles via specialiseret **logback** appender, som persisterer log
events til PostgreSQL tabel. Disse log events kan på et senere tidspunkt
udhentes igen igennem logstore servicens RESTful interface.

Alle log events i dataIO systemet, som omhandler forretningslogik
processeringen markeres med kontekstuel information i *Mapped Diagnostic
Context* i form af en tracking ID MDC nøgle kaldet **logStoreTrackingId**,
hvis værdi skal antage følgende form::

    {jobId}-{chunkId}-{itemId}

Dette gør det muligt i logstore at knytte en given log event til et item i
en chunk i et job (se job-store design dokument for en beskrivelse af
job-chunk-item modellen).

Drift beskrivelse
~~~~~~~~~~~~~~~~~

Logstore komponenten pakkes og distribueres som et Java EE7 WAR arkiv.

For at kunne persistere log events fordrer denne komponent, at der i
applikationsserveren forefindes en aktiv JDBC resource med nedenstående
JNDI navn, som peger på logstore databasen::

    jdbc/dataio/logstore

Bemærk desuden at databasen skal være oprettet med tabeller inden
processeringsmaskinen påbegynder sin logning.

For at konfigurere log-store servicens egen logning skal der i
applikationsserveren eksistere en environment variabel med navn::

    LOGSTORE_URL

hvis værdi skal være en URL, der peger på en logback *include* blok.

Se desuden **job-processor** design dokument for eksempel på logback
konfiguration, som indeholder den særlige logstore appender.

RESTful API
===========

.. code-block:: rst

    GET logentries/jobs/{jobId}/chunks/{chunkId}/items/{itemId}

Mulige returværdier:

    **HTTP 200 OK** svar med log indgange som **text/plain**

    **HTTP 404 NOT_FOUND** svar i tilfælde af at ingen log indgange kunne findes

    **HTTP 500 INTERNAL_SERVER_ERROR** svar i tilfælde af uventet intern fejl

