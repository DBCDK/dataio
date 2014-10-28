================================
dataIO filestore design dokument
================================

.. |date| date::

:author: Jan Bauer Nielsen <jbn@dbc.dk>
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
filestore komponent.


Beskrivelse
===========

Filestore komponentens funktion er at tilbyde et lagerteknologi uafhængigt
interface til at gemme og udhente data grupperet på fil niveau.

Data der uploades til filestore tildeles et ID, som efterfølgende kan
benyttes til at identificere og fremhente de pågældende data, og som
forbliver uændret i hele systemets levetid.

Filestore er primært tænkt som en komponent, hvori de forskellige dataIO
høster komponenter gemmer originaldata, som så senere hen læses i
forbindelse med job udførelse, men implementation bør ikke forhindre
alternative brugsscenarier. For at facilitere udførelse af jobs med
meget store (multi gigabyte) datafiler tilknyttet, streames data ud
af filestore som sekvenser af bytes, sådan at læsning på klientsiden
kan påbegyndes inden den fulde sekvens er overført.

I forbindelse med dybe/permanente links ind i filestore anbefales det at
benytte en URN struktur i stil med *urn:dataio-fs:{id}*, således at
afhængighed til server navn, portnummer og deslige kan undgås.

Teknisk beskrivelse
~~~~~~~~~~~~~~~~~~~

Det er filestore implementationens ansvar at Vedligeholde en mapning
mellem tildelt ID og faktisk lager placering på en sådan vis, at
lagerteknologien problemfrit kan udskiftes uden at påvirke klienterne,
og i den nuværende implementation håndteres dette ved hjælp af en metadata
tabel i en PostgreSQL database.

For nuværende gemmes de egentlige data i filsystemet med følgende simple
katalogstruktur::

    root
       |
       |__year (YYYY)
             |
             |__month (MM)
                    |
                    |__day (DD)

Drift beskrivelse
~~~~~~~~~~~~~~~~~

Filestore komponenten pakkes og distribueres som et Java EE7 EAR arkiv.

Til at vedligeholde den interne mapning mellem tildelt ID og lagerplacering
fordrer komponenten at der i applikationsserveren forefindes en aktiv JDBC
resource med JNDI navn (bemærk det store S i fileStore, det vil vi på sigt
gerne have ændret til lille s for konsekvensens skyld)::

    jdbc/dataio/fileStore

Placeringen i filsystemet (*root* på figuren) hentes fra en custom string
resource i applikationsserveren med JNDI navn::

    path/dataio/filestore/home


RESTful API
===========

.. code-block:: rst

    POST files

Streamer data givet som **application/octet-stream** ind som ny fil i fil lageret.

Mulige returværdier:

    **HTTP 201 CREATED** svar med en *Location* header indeholdende URL værdien for den nye fil resource

    **HTTP 500 INTERNAL_SERVER_ERROR** svar i tilfælde af uventet intern fejl

.. code-block:: rst

    GET files/{id}

Mulige returværdier:

    **HTTP 200 OK** svar med fil data som **application/octet-stream** stream

    **HTTP 404 NOT_FOUND** i tilfælde af at fil ID'et ikke kunne findes

    **HTTP 500 INTERNAL_SERVER_ERROR** svar i tilfælde af uventet intern fejl

