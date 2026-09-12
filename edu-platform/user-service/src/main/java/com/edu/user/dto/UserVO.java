package com.edu.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 用户视图对象（对外展示，脱敏）
 */
@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
