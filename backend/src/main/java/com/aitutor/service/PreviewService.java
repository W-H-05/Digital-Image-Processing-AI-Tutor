package com.aitutor.service;

import java.util.List;

/**
 * 文档在线预览服务：PPT/PPTX/PDF 转图片序列
 */
public interface PreviewService {

    /**
     * 将文档（PPT/PPTX/PDF）转为图片序列，返回图片 URL 列表。
     * @param filePath 材料文件的磁盘绝对路径（或相对 upload-dir 的路径）
     * @return 图片 URL 列表
     */
    List<String> toImages(String filePath);

    /**
     * 支持预览的文件扩展名
     */
    boolean support(String ext);
}
