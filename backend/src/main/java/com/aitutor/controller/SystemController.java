package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.config.RuntimeConfigService;
import com.aitutor.entity.QaRecord;
import com.aitutor.entity.SystemConfig;
import com.aitutor.entity.TeacherNote;
import com.aitutor.mapper.QaRecordMapper;
import com.aitutor.mapper.SystemConfigMapper;
import com.aitutor.mapper.TeacherNoteMapper;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 问答记录查询（教师）、教师备课笔记、系统配置、数据备份
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final QaRecordMapper qaRecordMapper;
    private final TeacherNoteMapper teacherNoteMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final RuntimeConfigService configService;

    /** 教师查看问答记录 */
    @GetMapping("/teacher/qa-records")
    public R<List<QaRecord>> qaRecords(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId,
                                       @RequestParam(value = "studentId", required = false) Long studentId,
                                       @RequestParam(value = "keyword", required = false) String keyword) {
        AuthUtil.requireTeacher();
        QueryWrapper<QaRecord> qw = new QueryWrapper<>();
        qw.eq(lessonPackId != null, "lesson_pack_id", lessonPackId)
          .eq(studentId != null, "user_id", studentId)
          .like(keyword != null && !keyword.isBlank(), "question", keyword)
          .orderByDesc("id");
        return R.ok(qaRecordMapper.selectList(qw));
    }

    /** 高频问题排行 */
    @GetMapping("/teacher/qa-records/hot")
    public R<List<Map<String, Object>>> hotQuestions(@RequestParam(value = "lessonPackId", required = false) Long lessonPackId) {
        AuthUtil.requireTeacher();
        QueryWrapper<QaRecord> qw = new QueryWrapper<>();
        qw.eq(lessonPackId != null, "lesson_pack_id", lessonPackId);
        List<QaRecord> records = qaRecordMapper.selectList(qw);
        Map<String, Integer> counter = new HashMap<>();
        for (QaRecord r : records) {
            String q = r.getQuestion() == null ? "" : r.getQuestion().trim();
            if (q.length() > 30) q = q.substring(0, 30);
            counter.merge(q, 1, Integer::sum);
        }
        return R.ok(counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("question", e.getKey(), "count", e.getValue()))
                .collect(java.util.stream.Collectors.toList()));
    }

    /** 学生查看自己的问答记录 */
    @GetMapping("/student/qa-records")
    public R<List<QaRecord>> myQaRecords() {
        Long userId = AuthUtil.currentUserId();
        return R.ok(qaRecordMapper.selectList(
                new QueryWrapper<QaRecord>().eq("user_id", userId).orderByDesc("id")));
    }

    /** 教师备课笔记 */
    @GetMapping("/teacher/notes/{packId}")
    public R<List<TeacherNote>> notes(@PathVariable Long packId) {
        AuthUtil.requireTeacher();
        return R.ok(teacherNoteMapper.selectList(
                new QueryWrapper<TeacherNote>().eq("lesson_pack_id", packId).orderByDesc("id")));
    }

    @PostMapping("/teacher/notes")
    public R<TeacherNote> saveNote(@RequestBody TeacherNote note) {
        Long teacherId = AuthUtil.currentUserId();
        note.setTeacherId(teacherId);
        teacherNoteMapper.insert(note);
        return R.ok(note);
    }

    /** 系统配置（教师读写，密钥脱敏） */
    @GetMapping("/system/config")
    public R<Map<String, String>> config() {
        AuthUtil.requireTeacher();
        return R.ok(configService.all());
    }

    /** 更新系统配置（键值对），更新后动态生效 */
    @PutMapping("/system/config")
    public R<Void> updateConfig(@RequestBody Map<String, String> configs) {
        AuthUtil.requireTeacher();
        for (Map.Entry<String, String> e : configs.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            // 密钥脱敏占位符不回写（保持原值）
            if (value != null && value.contains("****")) {
                continue;
            }
            SystemConfig exist = systemConfigMapper.selectOne(
                    new QueryWrapper<SystemConfig>().eq("config_key", key));
            if (exist != null) {
                exist.setConfigValue(value);
                systemConfigMapper.updateById(exist);
            } else {
                SystemConfig c = new SystemConfig();
                c.setConfigKey(key);
                c.setConfigValue(value);
                systemConfigMapper.insert(c);
            }
        }
        configService.reload();
        return R.ok();
    }

    // ==================== 数据备份 ====================

    /** 执行数据库备份（mysqldump 导出到备份目录） */
    @PostMapping("/system/backup")
    public R<Map<String, Object>> backup() {
        AuthUtil.requireTeacher();
        String backupPath = configService.get("backup.path", "./backup");
        File dir = new File(backupPath);
        if (!dir.exists()) dir.mkdirs();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "ai_tutor_" + ts + ".sql";
        File target = new File(dir, fileName);

        // 从运行时配置读取数据库信息（.env 注入，运行时也可覆盖）
        String dbHost = configService.get("db.host", System.getProperty("DB_HOST", "localhost"));
        String dbPort = configService.get("db.port", System.getProperty("DB_PORT", "3306"));
        String dbName = configService.get("db.name", System.getProperty("DB_NAME", "ai_tutor"));
        String dbUser = configService.get("db.username", System.getProperty("DB_USERNAME", "root"));
        String dbPassword = configService.get("db.password", System.getProperty("DB_PASSWORD", "root"));

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "D:/mysql-8.0.20/mysql-8.0.20-winx64/mysql-8.0.20-winx64/bin/mysqldump.exe",
                    "-h", dbHost, "-P", dbPort, "-u", dbUser, "-p" + dbPassword,
                    "--default-character-set=utf8mb4",
                    "--single-transaction", "--routines", "--triggers",
                    dbName);
            pb.redirectErrorStream(true);
            pb.redirectOutput(target);
            Process p = pb.start();
            p.waitFor();
            long size = target.length();
            log.info("数据库备份完成：{}（{} 字节）", target.getAbsolutePath(), size);

            // 清理旧备份
            int keepCount = configService.getInt("backup.keepCount", 10);
            File[] files = dir.listFiles(f -> f.getName().endsWith(".sql"));
            if (files != null && files.length > keepCount) {
                java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (int i = keepCount; i < files.length; i++) {
                    files[i].delete();
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("size", size);
            result.put("time", LocalDateTime.now().toString());
            return R.ok(result);
        } catch (Exception e) {
            log.error("备份失败", e);
            return R.fail(500, "备份失败：" + e.getMessage());
        }
    }

    /** 备份文件列表 */
    @GetMapping("/system/backup/list")
    public R<List<Map<String, Object>>> backupList() {
        AuthUtil.requireTeacher();
        String backupPath = configService.get("backup.path", "./backup");
        File dir = new File(backupPath);
        List<Map<String, Object>> result = new ArrayList<>();
        File[] files = dir.listFiles(f -> f.getName().endsWith(".sql"));
        if (files != null) {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : files) {
                Map<String, Object> m = new HashMap<>();
                m.put("fileName", f.getName());
                m.put("size", f.length());
                m.put("time", new java.util.Date(f.lastModified()).toString());
                result.add(m);
            }
        }
        return R.ok(result);
    }

    /** 下载备份文件 */
    @GetMapping("/system/backup/download/{fileName}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable String fileName) {
        AuthUtil.requireTeacher();
        String backupPath = configService.get("backup.path", "./backup");
        File file = new File(backupPath, fileName);
        if (!file.exists()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(file);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
