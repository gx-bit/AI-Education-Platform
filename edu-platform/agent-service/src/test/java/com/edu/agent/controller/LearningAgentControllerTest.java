package com.edu.agent.controller;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LearningAgentControllerTest {
  @Test void requestKeepsHumanConfirmationBoundary(){var r=new LearningAgentController.PlanRequest("通过秋招","Java 后端","beginner",8,14);assertEquals(8,r.weeks());assertEquals("通过秋招",r.goal());}
}
