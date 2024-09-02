Egenskab: EBOG5 På baggrund af data fra dmat service skal der dannes en opdateret post til RR

På baggrund af data fra dmat service og en vedhæftet post skal der dannes en opdateret post i RR

Scenarie: json object and attached record from dmat service can be converted to an updated marc record for RR update service - revived for ereol, record with 845 (MS-4236)
        # This example shows that a record sent with data in a json object from dmat service can be updated to a marc record for RR update service, type EBOG5 - revived, field 845: all subfields are transferred to updated record
        Når JavaScript funktionen localAcctestRunner fra script dmatToUpdate.js kaldes med post fra fil 190015.dmat.9788743053545-135310794.json.addi med agency 190015 og format dmat
        Så bliver resultatet addi som i fil 870970.135310794.dm2.addi

    Scenarie: json object and attached record from dmat service can be converted to an updated marc record for RR update service - update with 990b and f07 after BKMV
        # This example shows that a record sent with data in an json object from dmat service can be updated to a marc record for RR update service, type EBOG5 - updated after BKMV
        # Work flow modified after test implemented. All data in 990 and f07 regarding LU are handled manually. No need to update record
        Når JavaScript funktionen localAcctestRunner fra script dmatToUpdate.js kaldes med post fra fil 190015.dmat.9788794232111-134591773.json.addi med agency 190015 og format dmat
        Så bliver resultatet addi som i fil 870970.134591773.dm2.addi
