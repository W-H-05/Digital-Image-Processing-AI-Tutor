package com.aitutor.common;

/**
 * 通用常量
 */
public interface Constants {

    String ROLE_TEACHER = "TEACHER";
    String ROLE_STUDENT = "STUDENT";

    /** 课次包状态 */
    String PACK_DRAFT = "DRAFT";
    String PACK_PUBLISHED = "PUBLISHED";
    String PACK_OFFLINE = "OFFLINE";

    /** 材料类型 */
    String MT_TEACHING_PLAN = "教案";
    String MT_KEY_POINTS = "重难点";
    String MT_KNOWLEDGE = "知识点";
    String MT_PPT = "PPT";
    String MT_COURSEWARE = "交互课件";
    String MT_IMAGE = "图片素材";
    String MT_HOMEWORK = "作业";
    String MT_OTHER = "其他";
    String MT_TEACHER_NOTE = "备课笔记";

    /** 材料分组 */
    String G_PRE = "课前预习";
    String G_IN = "课中讲解";
    String G_POST = "课后作业";
    String G_EXT = "拓展阅读";
}
