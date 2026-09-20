package com.example.learning.camunda.cashloan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API demo cho process CAKE_CASHLOAN — hoàn toàn tách biệt với CashloanDemoController
 * (lab cũ /api/v1/camunda-demo). Base path riêng: /api/v1/cake-cashloan-demo.
 *
 * Vì process có rất nhiều điểm chờ (7 User Task + gần chục Receive Task chờ message khác
 * nhau), controller không expose một API riêng cho từng điểm như lab cũ, mà dùng 2 API
 * tổng quát: complete task theo taskDefinitionKey, và correlate message theo tên message.
 * Luôn gọi GET /{id} trước để biết đang cần gọi API nào tiếp theo.
 */
@RestController
@RequestMapping("/api/v1/cake-cashloan-demo")
public class CakeCashloanController {

    private final CakeCashloanService service;

    public CakeCashloanController(CakeCashloanService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody(required = false) Map<String, Object> variables) {
        String id = service.start(variables);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processInstanceId", id);
        response.put("statusUrl", "/api/v1/cake-cashloan-demo/" + id);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> status(@PathVariable String id) {
        return service.status(id);
    }

    @PostMapping("/{id}/tasks/{taskDefinitionKey}/complete")
    public Map<String, Object> completeTask(@PathVariable String id,
                                             @PathVariable String taskDefinitionKey,
                                             @RequestBody(required = false) Map<String, Object> variables) {
        service.completeTask(id, taskDefinitionKey, variables);
        return service.status(id);
    }

    @PostMapping("/{id}/correlate")
    public Map<String, Object> correlate(@PathVariable String id, @RequestBody CorrelateRequest request) {
        service.correlateMessage(id, request.messageName, request.variables);
        return service.status(id);
    }

    public static class CorrelateRequest {
        public String messageName;
        public Map<String, Object> variables;
    }
}
