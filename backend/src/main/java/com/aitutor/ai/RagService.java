package com.aitutor.ai;

import com.aitutor.entity.RagDocument;
import com.aitutor.mapper.RagDocumentMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索服务（内置内存关键词检索，零外部依赖）
 * 启动时加载全部知识库文档，基于中文 bigram + 英文单词构建倒排索引，
 * 用 BM25 打分返回 top-k 相关片段。结构上可后续替换为向量库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagService {

    private final RagDocumentMapper ragDocumentMapper;

    /** 全量文档 */
    private final List<RagDocument> documents = new ArrayList<>();
    /** 词 -> 文档索引列表 */
    private final Map<String, List<Integer>> invertedIndex = new HashMap<>();
    /** 文档 -> 词频 */
    private final List<Map<String, Integer>> termFreq = new ArrayList<>();
    private final Map<Integer, Integer> docLength = new HashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /** 从数据库重载全部文档并重建索引 */
    public synchronized void reload() {
        try {
            List<RagDocument> list = ragDocumentMapper.selectList(null);
            documents.clear();
            documents.addAll(list);
            buildIndex();
            log.info("RAG 知识库加载完成，共 {} 篇文档", documents.size());
        } catch (Exception e) {
            log.warn("RAG 知识库加载失败（可能表未初始化），稍后重试", e);
        }
    }

    /** 知识库文档总数 */
    public int count() {
        return documents.size();
    }

    private void buildIndex() {
        invertedIndex.clear();
        termFreq.clear();
        docLength.clear();
        for (int i = 0; i < documents.size(); i++) {
            Map<String, Integer> tf = tokenize(documents.get(i).getContent());
            termFreq.add(tf);
            docLength.put(i, tf.values().stream().mapToInt(Integer::intValue).sum());
            for (String term : tf.keySet()) {
                invertedIndex.computeIfAbsent(term, k -> new ArrayList<>()).add(i);
            }
        }
    }

    /**
     * 检索 top-k 相关片段
     */
    public List<RagHit> search(String query, int topK) {
        if (documents.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> qTerms = tokenize(query);
        if (qTerms.isEmpty()) {
            return Collections.emptyList();
        }

        double avgLen = docLength.values().stream().mapToDouble(Integer::doubleValue).average().orElse(1.0);
        Map<Integer, Double> scores = new HashMap<>();
        for (Map.Entry<String, Integer> qe : qTerms.entrySet()) {
            String term = qe.getKey();
            int qf = qe.getValue();
            List<Integer> posting = invertedIndex.get(term);
            if (posting == null) continue;
            int df = posting.size();
            double idf = Math.log(1.0 + (documents.size() - df + 0.5) / (df + 0.5));
            for (int docId : posting) {
                int tf = termFreq.get(docId).getOrDefault(term, 0);
                double k1 = 1.5, b = 0.75;
                double len = docLength.get(docId);
                double bm25 = idf * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * len / avgLen));
                scores.merge(docId, bm25 * qf, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    RagDocument d = documents.get(e.getKey());
                    return new RagHit(d.getTitle(), d.getCategory(), d.getContent(), e.getValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * 分词：中文按单字 + bigram，英文按单词，全部小写
     */
    private Map<String, Integer> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> map = new HashMap<>();
        String lower = text.toLowerCase();
        // 英文/数字单词
        for (String w : lower.split("[^a-z0-9]+")) {
            if (w.length() >= 2) {
                map.merge(w, 1, Integer::sum);
            }
        }
        // 中文单字 + bigram
        String cn = lower.replaceAll("[^\\u4e00-\\u9fa5]", "");
        for (int i = 0; i < cn.length(); i++) {
            map.merge(String.valueOf(cn.charAt(i)), 1, Integer::sum);
            if (i + 1 < cn.length()) {
                map.merge(cn.substring(i, i + 2), 1, Integer::sum);
            }
        }
        return map;
    }
}
