package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_material_view")
public class StudentMaterialView {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long lessonPackId;
    private Long materialId;
    private Integer viewDuration;
    private Integer pageNo;
    private String actionDetail;
    private LocalDateTime createTime;
}
