curl --location 'http://localhost:8086/ws' \
--header 'Content-Type: text/xml;charset=UTF-8' \
--data '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tra="http://example.com/learning/transfer"
                  xmlns:sec="http://example.com/learning/security">
    <soapenv:Header>
        <sec:PartnerSecurity>
            <sec:clientId>partner-mobile</sec:clientId>
            <sec:timestamp>2125-05-09T00:00:00Z</sec:timestamp>
            <sec:signature>qK7lNV7lPHYWyMkv2A6xrHLFYiLjvLreMyi+CeWk3zw=</sec:signature>
        </sec:PartnerSecurity>
    </soapenv:Header>
    <soapenv:Body>
        <tra:TransferRequest>
            <tra:requestId>REQ-001</tra:requestId>
            <tra:fromAccount>970400001</tra:fromAccount>
            <tra:toAccount>970400002</tra:toAccount>
            <tra:amount>500000</tra:amount>
            <tra:currency>VND</tra:currency>
        </tra:TransferRequest>
    </soapenv:Body>
</soapenv:Envelope>
'