package com.edu.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentNarratorTest {
  private AgentNarrator offline(){AgentNarrator n=new AgentNarrator(new ObjectMapper());ReflectionTestUtils.setField(n,"baseUrl","");ReflectionTestUtils.setField(n,"apiKey","");return n;}
  @Test void routesProgressWithoutModel(){var d=offline().decide("我的计划完成多少了？",List.of());assertEquals("PROGRESS",d.intent());assertFalse(d.modelUsed());}
  @Test void routesCourseSearchWithoutModel(){var d=offline().decide("推荐 Java 微服务课程",List.of());assertEquals("COURSE_SEARCH",d.intent());assertFalse(d.modelUsed());}
  @Test void doesNotPretendGeneralRequestUsedAModel(){var d=offline().decide("你好",List.of());assertEquals("GENERAL",d.intent());assertFalse(d.modelUsed());}
}
