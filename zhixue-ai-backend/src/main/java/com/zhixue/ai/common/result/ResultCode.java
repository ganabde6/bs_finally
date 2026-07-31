package com.zhixue.ai.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或token失效"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    // 业务错误码 1xxx
    AI_SERVICE_ERROR(1001, "AI服务调用失败"),
    RISK_WARNING(1002, "考试风控预警"),
    USER_NOT_EXIST(1003, "用户不存在"),
    PASSWORD_ERROR(1004, "密码错误"),
    ACCOUNT_DISABLED(1005, "账号已禁用"),
    PAPER_NOT_FOUND(1006, "试卷不存在"),
    ANSWER_ALREADY_SUBMIT(1007, "已提交过作答"),
    QUESTION_TYPE_NOT_SUPPORT(1008, "题型暂不支持"),
    CONTENT_RISK_BLOCK(1009, "内容触发风控拦截");

    private final Integer code;
    private final String message;
}
