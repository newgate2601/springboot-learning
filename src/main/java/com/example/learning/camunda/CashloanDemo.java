package com.example.learning.camunda;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CashloanDemo {
    public static final String LAB_PROCESS_KEY = "CAKE_FEATURE_LAB";
    private static final Logger log = LoggerFactory.getLogger(CashloanDemo.class);
    private final RestTemplate http = new RestTemplate();
    private final String baseUrl;
    private final String workerId = "cashloan-demo-" + UUID.randomUUID();

    public CashloanDemo(@Value("${camunda.demo.base-url:http://localhost:8080/engine-rest}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployOnStartup() {
        deploy("cashloan-feature-lab.bpmn", "cashloan-feature-lab");
    }

    private void deploy(String filename, String deploymentName) {
        try {
            byte[] bpmn;
            try (java.io.InputStream input = getClass().getResourceAsStream("/processes/" + filename)) {
                bpmn = input.readAllBytes();
            }
            ByteArrayResource resource = new ByteArrayResource(bpmn) {
                @Override public String getFilename() { return filename; }
            };
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("deployment-name", deploymentName);
            form.add("enable-duplicate-filtering", "true");
            form.add("deploy-changed-only", "true");
            form.add("data", resource);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            http.postForEntity(baseUrl + "/deployment/create", new HttpEntity<>(form, headers), String.class);
            log.info("Camunda demo deployed: {}", deploymentName);
        } catch (Exception e) {
            log.warn("Could not deploy {} at {}. Start Camunda Run and restart the app. Reason: {}", deploymentName, baseUrl, e.getMessage());
        }
    }

    public String startLab(Boolean segmentSuccess, Boolean precheckPassed, String segmentType) {
        return startLab(segmentSuccess, precheckPassed, segmentType, true);
    }

    public String startLab(Boolean segmentSuccess, Boolean precheckPassed,
                           String segmentType, Boolean autoCorrelate) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("segmentSuccess", variable(segmentSuccess == null || segmentSuccess));
        variables.put("precheckPassed", variable(precheckPassed == null || precheckPassed));
        variables.put("segmentType", variable(segmentType == null || segmentType.isEmpty() ? "VTP_ON_NET" : segmentType));
        variables.put("autoCorrelate", variable(autoCorrelate == null || autoCorrelate));
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variables", variables);
        JsonNode response = http.postForObject(baseUrl + "/process-definition/key/" + LAB_PROCESS_KEY + "/start", request, JsonNode.class);
        String id = response.path("id").asText();
        log.info("Started {}: processInstanceId={}, inputs={}", LAB_PROCESS_KEY, id, variables);
        return id;
    }

    public Map<String, Object> status(String id) {
        JsonNode activities = http.getForObject(baseUrl + "/history/activity-instance?processInstanceId=" + id, JsonNode.class);
        List<String> visited = new ArrayList<>();
        String outcome = null;
        if (activities != null) {
            for (JsonNode activity : activities) {
                String activityId = activity.path("activityId").asText();
                visited.add(activityId);
                if (activity.path("activityType").asText().endsWith("EndEvent") &&
                        ("lab_rejected_segment".equals(activityId) || "lab_rejected_precheck".equals(activityId) ||
                         "lab_skip_scoring".equals(activityId) || "lab_ready_for_scoring".equals(activityId))) {
                    outcome = activityId;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", id);
        result.put("outcome", outcome == null ? "RUNNING" : outcome);
        result.put("visitedActivities", visited);
        return result;
    }

    public Map<String, Object> labStatus(String id) {
        Map<String, Object> result = status(id);
        JsonNode tasks = http.getForObject(baseUrl + "/task?processInstanceId=" + id, JsonNode.class);
        if (tasks != null && tasks.isArray() && tasks.size() > 0) {
            result.put("waitingTaskId", tasks.get(0).path("id").asText());
            result.put("waitingTaskName", tasks.get(0).path("name").asText());
        }
        JsonNode history = http.getForObject(baseUrl + "/history/variable-instance?processInstanceId=" + id, JsonNode.class);
        Map<String, Object> variables = new LinkedHashMap<>();
        if (history != null) for (JsonNode item : history) variables.put(item.path("name").asText(), item.path("value"));
        result.put("variables", variables);
        return result;
    }

    public void completeLabReview(String id) {
        JsonNode tasks = http.getForObject(baseUrl + "/task?processInstanceId=" + id + "&taskDefinitionKey=lab_manual_review", JsonNode.class);
        if (tasks == null || tasks.size() == 0) throw new IllegalStateException("No review task for process " + id);
        http.postForEntity(baseUrl + "/task/" + tasks.get(0).path("id").asText() + "/complete", new LinkedHashMap<>(), String.class);
        log.info("[{}] Completed lab manual review", id);
    }

    @Scheduled(fixedDelayString = "${camunda.demo.poll-ms:1000}")
    public void work() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workerId", workerId);
        request.put("maxTasks", 5);
        request.put("usePriority", true);
        request.put("topics", new Object[] {
                topic("lab-segment", "requestSegmentSuccess", "requestAutoCorrelate", "requestType"),
                topic("lab-precheck", "requestPrecheck")
        });
        try {
            JsonNode tasks = http.postForObject(baseUrl + "/external-task/fetchAndLock", request, JsonNode.class);
            if (tasks == null) return;
            for (JsonNode task : tasks) complete(task);
        } catch (Exception e) {
            log.debug("Camunda worker unavailable: {}", e.getMessage());
        }
    }

    private void complete(JsonNode task) {
        String topic = task.path("topicName").asText();
        JsonNode input = task.path("variables");
        String instanceId = task.path("processInstanceId").asText();
        Map<String, Object> output = new LinkedHashMap<>();
        if ("lab-segment".equals(topic)) {
            boolean success = input.path("requestSegmentSuccess").path("value").asBoolean(true);
            String segmentType = input.path("requestType").path("value").asText("VTP_ON_NET");
            log.info("[{}] Phân loại segment: success={}, segmentType={}, mappedInput={}, extensionProperties={}",
                    instanceId, success, segmentType, input.path("requestType").path("value").asText(""), task.path("extensionProperties"));
            output.put("isSegmentEvaluatedSuccess", variable(success));
            output.put("segmentType", variable(segmentType));
        } else if ("lab-precheck".equals(topic)) {
            boolean passed = input.path("requestPrecheck").path("value").asBoolean(true);
            log.info("[{}] Precheck: isPrecheckPassed={}, mappedInput={}, extensionProperties={}",
                    instanceId, passed, input.path("requestPrecheck").path("value").asText(""), task.path("extensionProperties"));
            output.put("isPrecheckPassed", variable(passed));
        } else return;
        output.put("labExtensionFeature", variable(task.path("extensionProperties").path("demoFeature").asText("missing")));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workerId", workerId);
        body.put("variables", output);
        try {
            http.postForEntity(baseUrl + "/external-task/" + task.path("id").asText() + "/complete", body, String.class);
        } catch (Exception e) {
            log.error("[{}] Could not complete {}: {}", instanceId, topic, e.getMessage());
            return;
        }
        log.info("[{}] Completed {}", instanceId, topic);
        if ("lab-segment".equals(topic)) {
            if (input.path("requestAutoCorrelate").path("value").asBoolean(true)) {
                try {
                    correlateLabSegment(instanceId);
                } catch (Exception e) {
                    log.error("[{}] Completed lab-segment but could not correlate LabSegmentResult: {}. " +
                            "Use the manual correlation API to continue the process.", instanceId, e.getMessage());
                }
            } else {
                log.info("[{}] Waiting for manual LabSegmentResult correlation", instanceId);
            }
        }
    }

    public void correlateLabSegment(String id) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messageName", "LabSegmentResult");
        message.put("processInstanceId", id);
        http.postForEntity(baseUrl + "/message", message, String.class);
        log.info("[{}] Correlated LabSegmentResult", id);
    }

    private static Map<String, Object> topic(String name, String... variables) {
        Map<String, Object> topic = new LinkedHashMap<>();
        topic.put("topicName", name);
        topic.put("lockDuration", 10000);
        topic.put("includeExtensionProperties", true);
        topic.put("variables", variables);
        return topic;
    }

    private static Map<String, Object> variable(Object value) {
        Map<String, Object> variable = new LinkedHashMap<>();
        variable.put("value", value);
        variable.put("type", value instanceof Boolean ? "Boolean" : "String");
        return variable;
    }
}
