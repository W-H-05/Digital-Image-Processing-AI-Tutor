package com.aitutor.service.impl;

import com.aitutor.common.BizException;
import com.aitutor.common.Constants;
import com.aitutor.entity.LessonPack;
import com.aitutor.mapper.LessonPackMapper;
import com.aitutor.service.LessonPackService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonPackServiceImpl implements LessonPackService {

    private final LessonPackMapper lessonPackMapper;

    @Override
    public List<LessonPack> list(boolean teacher) {
        QueryWrapper<LessonPack> qw = new QueryWrapper<>();
        if (!teacher) {
            qw.eq("status", Constants.PACK_PUBLISHED);
        }
        qw.orderByAsc("id");
        return lessonPackMapper.selectList(qw);
    }

    @Override
    public LessonPack getById(Long id) {
        LessonPack pack = lessonPackMapper.selectById(id);
        if (pack == null) throw new BizException(404, "课次包不存在");
        return pack;
    }

    @Override
    public LessonPack create(LessonPack pack) {
        if (pack.getTitle() == null || pack.getTitle().isBlank()) {
            throw new BizException(400, "课次包标题不能为空");
        }
        if (pack.getCourseId() == null) {
            pack.setCourseId(1L);
        }
        if (pack.getStatus() == null) {
            pack.setStatus(Constants.PACK_DRAFT);
        }
        lessonPackMapper.insert(pack);
        return pack;
    }

    @Override
    public LessonPack update(Long id, LessonPack pack) {
        LessonPack exist = getById(id);
        pack.setId(id);
        pack.setCourseId(exist.getCourseId());
        lessonPackMapper.updateById(pack);
        return lessonPackMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        lessonPackMapper.deleteById(id);
    }

    @Override
    public void publish(Long id) {
        LessonPack pack = getById(id);
        pack.setStatus(Constants.PACK_PUBLISHED);
        lessonPackMapper.updateById(pack);
    }

    @Override
    public void offline(Long id) {
        LessonPack pack = getById(id);
        pack.setStatus(Constants.PACK_OFFLINE);
        lessonPackMapper.updateById(pack);
    }

    @Override
    public LessonPack copy(Long id) {
        LessonPack src = getById(id);
        LessonPack copy = new LessonPack();
        BeanUtils.copyProperties(src, copy, "id", "createTime", "updateTime");
        copy.setTitle(src.getTitle() + "（副本）");
        copy.setStatus(Constants.PACK_DRAFT);
        lessonPackMapper.insert(copy);
        return copy;
    }
}
