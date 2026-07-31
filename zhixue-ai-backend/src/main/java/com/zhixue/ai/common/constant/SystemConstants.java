package com.zhixue.ai.common.constant;

/**
 * 系统常量
 */
public class SystemConstants {

    /** 角色编码 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_SCHOOL_ADMIN = "SCHOOL_ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    /** 试卷类型 */
    public static final int PAPER_TYPE_HOMEWORK = 1;
    public static final int PAPER_TYPE_EXAM = 2;

    /** 试卷状态 */
    public static final int PAPER_STATUS_DRAFT = 0;
    public static final int PAPER_STATUS_PUBLISHED = 1;
    public static final int PAPER_STATUS_FINISHED = 2;

    /** 作答状态 */
    public static final int ANSWER_STATUS_NOT_SUBMIT = 0;
    public static final int ANSWER_STATUS_SUBMITTED = 1;
    public static final int ANSWER_STATUS_CORRECTED = 2;
    public static final int ANSWER_STATUS_REVIEWED = 3;

    /** 批改类型 */
    public static final int CORRECT_TYPE_AI = 1;
    public static final int CORRECT_TYPE_MANUAL = 2;

    /** 题型 */
    public static final int Q_TYPE_SINGLE = 1;
    public static final int Q_TYPE_MULTI = 2;
    public static final int Q_TYPE_JUDGE = 3;
    public static final int Q_TYPE_FILL = 4;
    public static final int Q_TYPE_SHORT = 5;
    public static final int Q_TYPE_ESSAY = 6;
    public static final int Q_TYPE_CALC = 7;

    private SystemConstants() {}
}
