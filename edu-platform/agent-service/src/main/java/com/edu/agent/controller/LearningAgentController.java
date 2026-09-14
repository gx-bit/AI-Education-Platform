package com.edu.agent.controller;

import com.edu.agent.service.LearningAgentService;
import com.edu.agent.service.AgentChatService;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.Result;
import com.edu.common.core.result.ResultCode;
import com.edu.common.security.context.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/agent") @RequiredArgsConstructor
public class LearningAgentController {
  private final LearningAgentService service;
  private final AgentChatService chatService;
  public record PlanRequest(@NotBlank @Size(max=500) String goal,@NotBlank @Size(max=300) String interest,
    @Pattern(regexp="beginner|intermediate|advanced") String currentLevel,@Min(1) @Max(12) Integer weeks,
    @Min(1) @Max(40) Integer hoursPerWeek) {}
  public record ChatRequest(String conversationId,@NotBlank @Size(max=2000) String message) {}

  @PostMapping("/plans/preview") public Result<Map<String,Object>> preview(@Valid @RequestBody PlanRequest r){return Result.success(service.createDraft(user(),r));}
  @PostMapping("/plans/{id}/confirm") public Result<Map<String,Object>> confirm(@PathVariable String id){return Result.success(service.confirm(user(),id));}
  @GetMapping("/plans/current") public Result<Map<String,Object>> current(){return Result.success(service.current(user()));}
  @PostMapping("/tasks/{id}/complete") public Result<Map<String,Object>> complete(@PathVariable String id){return Result.success(service.changeTask(user(),id,true));}
  @PostMapping("/tasks/{id}/reopen") public Result<Map<String,Object>> reopen(@PathVariable String id){return Result.success(service.changeTask(user(),id,false));}
  @PostMapping("/chat") public Result<Map<String,Object>> chat(@Valid @RequestBody ChatRequest r){return Result.success(chatService.chat(user(),r.conversationId(),r.message()));}
  @GetMapping("/conversations/{id}/messages") public Result<?> history(@PathVariable String id){return Result.success(chatService.history(user(),id));}
  @GetMapping("/admin/metrics") public Result<Map<String,Object>> metrics(){if(!UserContext.isAdmin())throw new BusinessException(ResultCode.FORBIDDEN);return Result.success(chatService.metrics());}
  private Long user(){Long id=UserContext.getCurrentUserId();if(id==null)throw new BusinessException(ResultCode.UNAUTHORIZED);return id;}
}
