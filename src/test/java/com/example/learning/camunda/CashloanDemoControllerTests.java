package com.example.learning.camunda;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashloanDemoControllerTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startRequestUsesTheBpmnVariableNames() throws Exception {
        String json = """
                {
                  "segmentSuccess": false,
                  "precheckPassed": true,
                  "segmentType": "VTP_OFF_NET"
                }
                """;

        CashloanDemoController.StartRequest request =
                objectMapper.readValue(json, CashloanDemoController.StartRequest.class);

        assertFalse(request.segmentSuccess);
        assertTrue(request.precheckPassed);
        assertEquals("VTP_OFF_NET", request.segmentType);
    }

    @Test
    void startRequestRejectsUnknownVariableNames() {
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(
                "{\"unknownField\":false}", CashloanDemoController.StartRequest.class));
    }
}
