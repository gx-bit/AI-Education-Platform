package com.edu.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Component @RequiredArgsConstructor
public class AgentNarrator {
  private final ObjectMapper json;
  @Value("${agent.ai-base-url:}") private String baseUrl;
  @Value("${agent.ai-api-key:}") private String apiKey;
  @Value("${agent.ai-model:deepseek-chat}") private String model;

  @SuppressWarnings("unchecked")
  public String summarize(String goal,String interest,String level,int weeks,int hours,List<Map<String,Object>> courses){
    String fallback="Agent 已调用课程推荐工具，并根据目标、兴趣、水平和可用时间拆解任务。确认计划后才会开始记录进度。";
    if(baseUrl==null||baseUrl.isBlank()||apiKey==null||apiKey.isBlank())return fallback;
    try{
      String names=courses.stream().map(c->String.valueOf(c.get("title"))).filter(s->!"null".equals(s)).limit(6).reduce((a,b)->a+"、"+b).orElse("暂无匹配课程");
      String prompt="你是教育平台学习规划Agent。请用80字以内中文说明规划依据，不夸大能力，不承诺结果。目标："+goal+"；兴趣："+interest+"；水平："+level+"；周期："+weeks+"周；每周："+hours+"小时；检索课程："+names;
      Map<String,Object> body=Map.of("model",model,"temperature",0.2,"messages",List.of(Map.of("role","system","content","你负责解释已经由业务系统生成的学习计划。"),Map.of("role","user","content",prompt)));
      Map<String,Object> response=RestClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer "+apiKey).build().post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
      List<?> choices=(List<?>)response.get("choices"); if(choices==null||choices.isEmpty())return fallback;
      Map<String,Object> choice=(Map<String,Object>)choices.get(0),message=(Map<String,Object>)choice.get("message");
      String text=String.valueOf(message.get("content")).trim();return text.isBlank()?fallback:text;
    }catch(Exception ignored){return fallback;}
  }

  public record Decision(String intent,String reply,boolean modelUsed) {}

  @SuppressWarnings("unchecked")
  public Decision decide(String message,List<Map<String,Object>> history){
    String fallback=message.matches(".*(进度|完成多少|计划情况).*")?"PROGRESS":message.matches(".*(推荐|课程|学习什么).*" )?"COURSE_SEARCH":"GENERAL";
    if(baseUrl==null||baseUrl.isBlank()||apiKey==null||apiKey.isBlank())return new Decision(fallback,"",false);
    try{
      String prompt="选择工具，只输出JSON：{\"intent\":\"PROGRESS|COURSE_SEARCH|GENERAL\",\"reply\":\"简短回复\"}。不得声称执行未执行的动作。历史："+json.writeValueAsString(history)+"；用户："+message;
      Map<String,Object> body=Map.of("model",model,"temperature",0.1,"messages",List.of(Map.of("role","system","content","你是受控学习Agent，只能选择给定工具。"),Map.of("role","user","content",prompt)));
      Map<String,Object> response=RestClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer "+apiKey).build().post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
      List<?> choices=(List<?>)response.get("choices");Map<String,Object> choice=(Map<String,Object>)choices.get(0),msg=(Map<String,Object>)choice.get("message");String raw=String.valueOf(msg.get("content")).replace("```json","").replace("```","").trim();
      Map<String,Object> parsed=json.readValue(raw,Map.class);String intent=String.valueOf(parsed.get("intent"));if(!Set.of("PROGRESS","COURSE_SEARCH","GENERAL").contains(intent))intent="GENERAL";
      return new Decision(intent,String.valueOf(parsed.getOrDefault("reply","")),true);
    }catch(Exception ignored){return new Decision(fallback,"",false);}
  }
}
