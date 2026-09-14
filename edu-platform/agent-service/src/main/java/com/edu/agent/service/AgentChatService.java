package com.edu.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AgentChatService {
  private final JdbcTemplate jdbc; private final LearningAgentService plans; private final AgentNarrator model; private final ObjectMapper json;

  public Map<String,Object> chat(Long uid,String conversationId,String message){
    long started=System.currentTimeMillis();String cid=conversation(uid,conversationId,message);save(cid,uid,"user",message,null,null,null,null);
    List<Map<String,Object>> history=jdbc.queryForList("SELECT role,content FROM t_agent_message WHERE conversation_id=? AND user_id=? ORDER BY created_at DESC LIMIT 12",cid,uid);Collections.reverse(history);
    AgentNarrator.Decision decision=model.decide(message,history);String tool=null,reply;Object result=null;
    if("PROGRESS".equals(decision.intent())){tool="get_current_plan";result=plans.current(uid);Map<?,?> p=(Map<?,?>)result;reply=Boolean.TRUE.equals(p.get("empty"))?"你还没有学习计划，可以先让我制定一份。":"当前计划进度 "+p.get("progress")+"%，已完成 "+p.get("completedTasks")+" / "+p.get("totalTasks")+" 个任务。";}
    else if("COURSE_SEARCH".equals(decision.intent())){tool="search_courses";result=plans.searchCourses(uid,message);int count=((List<?>)result).size();reply=count==0?"暂时没有检索到匹配课程，可以换一个更具体的方向。":"我从平台课程库检索到 "+count+" 门相关课程，结果已附在回复中。";}
    else reply=decision.reply().isBlank()?"我可以搜索课程、查询学习进度，也可以通过上方表单生成需要你确认的学习计划。":decision.reply();
    long latency=System.currentTimeMillis()-started;String modelName=decision.modelUsed()?"configured-llm":"deterministic-fallback";save(cid,uid,"assistant",reply,tool,result,modelName,latency);jdbc.update("UPDATE t_agent_conversation SET updated_at=NOW() WHERE id=?",cid);
    Map<String,Object> out=new LinkedHashMap<>();out.put("conversationId",cid);out.put("reply",reply);out.put("toolName",tool);out.put("toolResult",result);out.put("modelUsed",modelName);out.put("latencyMs",latency);return out;
  }
  public List<Map<String,Object>> history(Long uid,String cid){return jdbc.queryForList("SELECT id,role,content,tool_name toolName,tool_result toolResult,model_used modelUsed,latency_ms latencyMs,created_at createdAt FROM t_agent_message WHERE conversation_id=? AND user_id=? ORDER BY created_at",cid,uid);}
  public Map<String,Object> metrics(){Map<String,Object> out=new LinkedHashMap<>();out.put("conversations",jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_conversation",Long.class));out.put("messages",jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_message",Long.class));out.put("toolCalls",jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_message WHERE tool_name IS NOT NULL",Long.class));out.put("fallbackRate",jdbc.queryForObject("SELECT COALESCE(ROUND(100*SUM(model_used='deterministic-fallback')/NULLIF(COUNT(*),0),1),0) FROM t_agent_message WHERE role='assistant'",Double.class));out.put("averageLatencyMs",jdbc.queryForObject("SELECT COALESCE(ROUND(AVG(latency_ms)),0) FROM t_agent_message WHERE role='assistant'",Long.class));return out;}
  private String conversation(Long uid,String cid,String first){if(cid!=null&&!cid.isBlank()){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_conversation WHERE id=? AND user_id=?",Integer.class,cid,uid);if(n!=null&&n>0)return cid;}String id=UUID.randomUUID().toString();jdbc.update("INSERT INTO t_agent_conversation VALUES(?,?,?,?,?)",id,uid,first.length()>50?first.substring(0,50):first,LocalDateTime.now(),LocalDateTime.now());return id;}
  private void save(String cid,Long uid,String role,String content,String tool,Object result,String modelName,Long latency){String value=null;try{if(result!=null)value=json.writeValueAsString(result);}catch(Exception ignored){}jdbc.update("INSERT INTO t_agent_message VALUES(?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),cid,uid,role,content,tool,value,modelName,latency,LocalDateTime.now());}
}
