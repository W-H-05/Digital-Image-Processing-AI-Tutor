package com.aitutor.service;

import com.aitutor.entity.LessonPack;

import java.util.List;

public interface LessonPackService {

    /** 课次包列表：学生只看到已发布，教师看到全部 */
    List<LessonPack> list(boolean teacher);

    LessonPack getById(Long id);

    LessonPack create(LessonPack pack);

    LessonPack update(Long id, LessonPack pack);

    void delete(Long id);

    void publish(Long id);

    void offline(Long id);

    LessonPack copy(Long id);
}
