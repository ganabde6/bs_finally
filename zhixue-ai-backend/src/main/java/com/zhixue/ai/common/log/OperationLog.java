package com.zhixue.ai.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解:标注在需要记录操作日志的接口方法上,
 * 由 SysLogAspect 自动记录操作人、模块、操作、参数、IP 与耗时
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 所属模块,如"用户管理" */
    String module();

    /** 操作描述,如"新增用户" */
    String operation();
}
