package com.aitutor.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RAG 检索结果片段
 */
@Data
@AllArgsConstructor
public class RagHit {
    private String title;
    private String category;
    private String content;
    private double score;
}
