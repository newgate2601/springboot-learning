package com.example.learning.camunda;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CAMUNDA_DEMO_IT", matches = "true")
class CashloanDemoIntegrationTests {
    @Autowired private CashloanDemo demo;

    @Test
    void featureLabCoversMessagesMappingsListenersAndUserTask() throws InterruptedException {
        assertLabOutcome(false, true, "VTP_ON_NET", "lab_rejected_segment");
        assertLabOutcome(true, false, "VTP_ON_NET", "lab_rejected_precheck");
        assertLabOutcome(true, true, "VTP_OFF_NET", "lab_skip_scoring");

        String id = demo.startLab(true, true, "VTP_ON_NET");
        Map<String, Object> waiting = waitForLab(id, "lab_manual_review");
        assertNotNull(waiting.get("waitingTaskId"), waiting.toString());
        assertEquals("created", waiting.get("variables") instanceof Map ? ((Map<?, ?>) waiting.get("variables")).get("labReviewEvent").toString().replace("\"", "") : null);
        demo.completeLabReview(id);
        Map<String, Object> ended = waitForLab(id, "lab_ready_for_scoring");
        assertTrue(ended.get("visitedActivities").toString().contains("lab_submit_message"));
        Map<?, ?> variables = (Map<?, ?>) ended.get("variables");
        assertEquals("true", variables.get("labMessageThrown").toString());
        assertEquals("true", variables.get("segmentWorkerResult").toString());
        assertEquals("true", variables.get("workerResult").toString());
        assertEquals("ended", variables.get("labLifecycle").toString().replace("\"", ""));
        assertEquals("completed", variables.get("labReviewEvent").toString().replace("\"", ""));
        assertEquals("async-after-and-output-mapping", variables.get("labExtensionFeature").toString().replace("\"", ""));
    }

    @Test
    void featureLabCanWaitForManualMessageCorrelation() throws InterruptedException {
        String id = demo.startLab(true, true, "VTP_ON_NET", false);
        Map<String, Object> waiting = null;
        for (int i = 0; i < 100; i++) {
            demo.work();
            waiting = demo.labStatus(id);
            if (waiting.get("visitedActivities").toString().contains("lab_receive_segment")) break;
            Thread.sleep(100);
        }
        assertNotNull(waiting);
        assertTrue(waiting.get("visitedActivities").toString().contains("lab_receive_segment"), waiting.toString());
        assertEquals("RUNNING", waiting.get("outcome"));
        assertFalse(waiting.get("visitedActivities").toString().contains("lab_precheck"), waiting.toString());

        demo.correlateLabSegment(id);
        Map<String, Object> review = waitForLab(id, "lab_manual_review");
        assertNotNull(review.get("waitingTaskId"));
        demo.completeLabReview(id);
        assertEquals("lab_ready_for_scoring", waitForLab(id, "lab_ready_for_scoring").get("outcome"));
    }

    private void assertLabOutcome(boolean segment, boolean precheck, String type, String expected) throws InterruptedException {
        Map<String, Object> state = waitForLab(demo.startLab(segment, precheck, type), expected);
        assertFalse(state.get("variables").toString().contains("labExtensionFeature=\"missing\""), state.toString());
    }

    private Map<String, Object> waitForLab(String id, String expected) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            demo.work();
            Map<String, Object> state = demo.labStatus(id);
            if (expected.equals(state.get("outcome")) ||
                    ("lab_manual_review".equals(expected) && state.get("waitingTaskId") != null)) return state;
            Thread.sleep(100);
        }
        Map<String, Object> state = demo.labStatus(id);
        assertEquals(expected, state.get("outcome"), state.toString());
        return state;
    }

}
