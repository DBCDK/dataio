Egenskab: 150041.credo

    Scenarie: preparation of xml record from credo
        # This example shows that a record from credo can be prepared for the Tickle Repo
        Når JavaScript funktionen prepareTickleRepoXmlFromCredo fra script credo_ticklerepo.js kaldes med post fra fil 150041.credo.enpost.xml med agency 150041 og format credo
        Så bliver resultatet addi som i fil 150041.credo.enpost.tickle.addi

    Scenarie: preparation of another xml record from credo
        # This example shows that another record from credo can be prepared for the Tickle Repo
        Når JavaScript funktionen prepareTickleRepoXmlFromCredo fra script credo_ticklerepo.js kaldes med post fra fil 150041.credo.topost.xml med agency 150041 og format credo
        Så bliver resultatet addi som i fil 150041.credo.topost.tickle.addi

    Scenarie: preparation of xml delete record from credo
        # This example shows that a delete record from credo can be prepared for the Tickle Repo
        Når JavaScript funktionen prepareTickleRepoXmlFromCredo fra script credo_ticklerepo.js kaldes med post fra fil 150041.credo.delete.xml med agency 150041 og format credo
        Så bliver resultatet addi som i fil 150041.credo.delete.tickle.addi

