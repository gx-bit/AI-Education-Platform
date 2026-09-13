package com.edu.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.core.result.PageResult;
import com.edu.common.core.result.Result;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.ResultCode;
import com.edu.common.security.context.UserContext;
import com.edu.notification.entity.Notification;
import com.edu.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/admin/send")
    public Result<Integer> send(@RequestBody Map<String, Object> body) {
        if (!UserContext.isAdmin()) throw new BusinessException(ResultCode.FORBIDDEN);
        String title = Objects.toString(body.get("title"), "").trim();
        String content = Objects.toString(body.get("content"), "").trim();
        if (title.isEmpty() || content.isEmpty()) throw new BusinessException(ResultCode.PARAM_ERROR);
        List<Long> userIds;
        if ("all".equals(body.get("scope"))) {
            userIds = jdbcTemplate.queryForList("select id from edu_user.t_user where deleted=0 and status=1", Long.class);
        } else {
            Object userId = body.get("userId");
            if (userId == null) throw new BusinessException(ResultCode.PARAM_ERROR);
            userIds = List.of(Long.valueOf(userId.toString()));
        }
        userIds.forEach(userId -> {
            Notification n = new Notification(); n.setUserId(userId); n.setTitle(title); n.setContent(content);
            n.setType("system"); n.setIsRead(0); n.setCreatedAt(LocalDateTime.now()); notificationMapper.insert(n);
        });
        return Result.success(userIds.size());
    }

    /** 查询当前登录用户的通知列表 */
    @GetMapping("/my")
    public Result<PageResult<Notification>> my(
            @RequestParam(name = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return Result.success(queryNotifications(requireCurrentUserId(), unreadOnly, page, size));
    }

    /** 查询当前登录用户未读通知数 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = requireCurrentUserId();
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return Result.success(count);
    }

    /** 查询当前登录用户的一条通知详情 */
    @GetMapping("/{id}")
    public Result<Notification> detail(@PathVariable("id") Long id) {
        Long userId = requireCurrentUserId();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!userId.equals(notification.getUserId())) throw new BusinessException(ResultCode.FORBIDDEN);
        return Result.success(notification);
    }

    private PageResult<Notification> queryNotifications(Long userId, boolean unreadOnly, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(unreadOnly, Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreatedAt);

        Page<Notification> pageResult = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal(), pageResult.getRecords());
    }

    /** 标记单条通知为已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        Long userId = requireCurrentUserId();
        Notification existing = notificationMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!userId.equals(existing.getUserId())) throw new BusinessException(ResultCode.FORBIDDEN);
        Notification n = new Notification();
        n.setId(id);
        n.setIsRead(1);
        notificationMapper.updateById(n);
        return Result.success();
    }

    /** 标记用户所有通知为已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long targetUserId = requireCurrentUserId();
        Notification n = new Notification();
        n.setIsRead(1);
        notificationMapper.update(n,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, targetUserId)
                        .eq(Notification::getIsRead, 0));
        return Result.success();
    }

    /** 删除当前用户的一条通知 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = requireCurrentUserId();
        Notification existing = notificationMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!userId.equals(existing.getUserId())) throw new BusinessException(ResultCode.FORBIDDEN);
        notificationMapper.deleteById(id);
        return Result.success();
    }

    private Long requireCurrentUserId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Missing current user");
        }
        return userId;
    }
}
