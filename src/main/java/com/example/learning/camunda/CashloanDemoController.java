package com.example.learning.camunda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/camunda-demo")
public class CashloanDemoController {
    private final CashloanDemo demo;

    public CashloanDemoController(CashloanDemo demo) {
        this.demo = demo;
    }

    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody(required = false) StartRequest input) {
        if (input == null) input = new StartRequest();
        String id = demo.start(input.segmentSuccess, input.precheckPassed, input.segmentType);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processInstanceId", id);
        response.put("statusUrl", "/api/v1/camunda-demo/" + id);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> status(@PathVariable String id) {
        return demo.status(id);
    }

    @PostMapping("/lab/start")
    public Map<String, Object> startLab(@RequestBody(required = false) StartRequest input) {
        if (input == null) input = new StartRequest();
        String id = demo.startLab(input.segmentSuccess, input.precheckPassed, input.segmentType, input.autoCorrelate);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processInstanceId", id);
        response.put("statusUrl", "/api/v1/camunda-demo/lab/" + id);
        return response;
    }

    @GetMapping("/lab/{id}")
    public Map<String, Object> labStatus(@PathVariable String id) {
        return demo.labStatus(id);
    }

    @PostMapping("/lab/{id}/complete-review")
    public Map<String, Object> completeLabReview(@PathVariable String id) {
        demo.completeLabReview(id);
        return demo.labStatus(id);
    }

    @PostMapping("/lab/{id}/correlate-segment")
    public Map<String, Object> correlateLabSegment(@PathVariable String id) {
        demo.correlateLabSegment(id);
        return demo.labStatus(id);
    }

    public static class StartRequest {
        public Boolean segmentSuccess;
        public Boolean precheckPassed;
        public String segmentType;
        public Boolean autoCorrelate;
    }
}
