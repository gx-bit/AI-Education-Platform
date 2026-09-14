package com.edu.agent.service;

import com.edu.agent.controller.LearningAgentController.PlanRequest;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class LearningAgentService {
  private final JdbcTemplate jdbc; private final ObjectMapper json; private final AgentNarrator narrator;
  @Value("${agent.recommendation-base-url}") private String recommendationUrl;

  @Transactional public Map<String,Object> createDraft(Long uid,PlanRequest r){
    int weeks=r.weeks()==null?4:r.weeks(),hours=r.hoursPerWeek()==null?7:r.hoursPerWeek();
    String level=r.currentLevel()==null?"beginner":r.currentLevel(),pid=UUID.randomUUID().toString();
    List<Map<String,Object>> courses=recommend(uid,r.interest(),r.goal(),level,Math.min(6,weeks*2));
    LocalDateTime now=LocalDateTime.now(); String title=cut(r.goal(),100)+" · AI 学习计划";
    String summary=narrator.summarize(r.goal(),r.interest(),level,weeks,hours,courses);
    jdbc.update("INSERT INTO t_learning_plan VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",pid,uid,title,r.goal(),r.interest(),level,weeks,hours,"DRAFT",summary,now,now);
    int minutes=Math.max(30,hours*60/3);
    for(int w=1;w<=weeks;w++){
      Map<String,Object> c=courses.isEmpty()?Map.of():courses.get((w-1)%courses.size());
      task(pid,uid,w,c,"学习核心内容","完成推荐课程对应章节，记录关键概念和两个仍不清楚的问题。",minutes,now);
      task(pid,uid,w,c,"项目实践与验证","把知识映射到 AI 智慧教育平台，完成一次可运行修改或故障排查。",minutes,now);
      task(pid,uid,w,c,"复盘与面试输出","口述调用链、技术取舍与异常场景，整理为项目面试问答。",Math.max(30,minutes/2),now);
    }
    audit(uid,pid,"CREATE_PLAN_DRAFT","WAITING_CONFIRMATION",true,r,Map.of("courses",courses.size(),"tasks",weeks*3));
    return load(uid,pid);
  }

  @Transactional public Map<String,Object> confirm(Long uid,String pid){
    int n=jdbc.update("UPDATE t_learning_plan SET status='ACTIVE',updated_at=NOW() WHERE id=? AND user_id=? AND status='DRAFT'",pid,uid);
    if(n==0)throw new BusinessException("计划不存在、已经确认或无权操作");
    jdbc.update("UPDATE t_learning_plan SET status='ARCHIVED',updated_at=NOW() WHERE user_id=? AND id<>? AND status='ACTIVE'",uid,pid);
    audit(uid,pid,"CONFIRM_PLAN","SUCCEEDED",false,Map.of(),Map.of()); return load(uid,pid);
  }

  public Map<String,Object> current(Long uid){
    List<String> ids=jdbc.query("SELECT id FROM t_learning_plan WHERE user_id=? ORDER BY FIELD(status,'ACTIVE','DRAFT','ARCHIVED'),updated_at DESC LIMIT 1",(rs,n)->rs.getString(1),uid);
    return ids.isEmpty()?Map.of("empty",true):load(uid,ids.get(0));
  }

  @Transactional public Map<String,Object> changeTask(Long uid,String tid,boolean done){
    int n=jdbc.update("UPDATE t_learning_task t JOIN t_learning_plan p ON p.id=t.plan_id SET t.status=?,t.completed_at=?,t.updated_at=NOW() WHERE t.id=? AND t.user_id=? AND p.status='ACTIVE'",done?"COMPLETED":"TODO",done?LocalDateTime.now():null,tid,uid);
    if(n==0)throw new BusinessException("任务不存在、计划未确认或无权操作");
    String pid=jdbc.queryForObject("SELECT plan_id FROM t_learning_task WHERE id=?",String.class,tid);
    audit(uid,pid,done?"COMPLETE_TASK":"REOPEN_TASK","SUCCEEDED",false,Map.of("taskId",tid),Map.of()); return load(uid,pid);
  }

  private Map<String,Object> load(Long uid,String pid){
    List<Map<String,Object>> plans=jdbc.queryForList("SELECT id,title,goal,interest,current_level currentLevel,weeks,hours_per_week hoursPerWeek,status,agent_summary agentSummary,created_at createdAt,updated_at updatedAt FROM t_learning_plan WHERE id=? AND user_id=?",pid,uid);
    if(plans.isEmpty())throw new BusinessException(ResultCode.NOT_FOUND);
    List<Map<String,Object>> tasks=jdbc.queryForList("SELECT id,week_no weekNo,CAST(course_id AS CHAR) courseId,course_title courseTitle,title,description,estimated_minutes estimatedMinutes,status,completed_at completedAt FROM t_learning_task WHERE plan_id=? AND user_id=? ORDER BY week_no,created_at",pid,uid);
    long done=tasks.stream().filter(t->"COMPLETED".equals(t.get("status"))).count(); Map<String,Object> out=new LinkedHashMap<>(plans.get(0));
    out.put("tasks",tasks);out.put("completedTasks",done);out.put("totalTasks",tasks.size());out.put("progress",tasks.isEmpty()?0:(int)Math.round(done*100.0/tasks.size()));return out;
  }

  @SuppressWarnings("unchecked") private List<Map<String,Object>> recommend(Long uid,String interest,String goal,String level,int limit){
    try{Map<String,Object> res=RestClient.builder().baseUrl(recommendationUrl).build().post().uri("/api/recommendation/courses").header("X-User-Id",String.valueOf(uid)).contentType(MediaType.APPLICATION_JSON).body(Map.of("interest",interest,"goal",goal,"level",level,"limit",limit,"sessionId","agent-"+uid)).retrieve().body(Map.class);
      Object data=res==null?null:res.get("data"),items=data instanceof Map<?,?> m?m.get("courses"):null;return items instanceof List<?> l?(List<Map<String,Object>>)(List<?>)l:List.of();
    }catch(Exception ignored){return List.of();}
  }
  public List<Map<String,Object>> searchCourses(Long uid,String query){return recommend(uid,query,query,"beginner",5);}
  private void task(String pid,Long uid,int week,Map<String,Object> c,String title,String desc,int min,LocalDateTime now){Object cid=c.get("id");jdbc.update("INSERT INTO t_learning_task(id,plan_id,user_id,week_no,course_id,course_title,title,description,estimated_minutes,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,'TODO',?,?)",UUID.randomUUID().toString(),pid,uid,week,cid==null?null:Long.valueOf(String.valueOf(cid)),c.get("title"),title,desc,min,now,now);}
  private void audit(Long uid,String pid,String type,String status,boolean confirm,Object in,Object out){jdbc.update("INSERT INTO t_agent_action_log VALUES(?,?,?,?,?,?,?,?,NOW())",UUID.randomUUID().toString(),uid,pid,type,status,confirm,toJson(in),toJson(out));}
  private String toJson(Object v){try{return json.writeValueAsString(v);}catch(Exception e){return "{}";}}
  private String cut(String v,int n){return v.length()<=n?v:v.substring(0,n);}
}
