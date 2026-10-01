package com.aitutor.service.impl;

import com.aitutor.common.BizException;
import com.aitutor.config.AppProperties;
import com.aitutor.service.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 文档在线预览：PPT/PPTX/PDF 渲染为 PNG 图片序列。
 *
 * 渲染引擎：LibreOffice（soffice --headless）转 PDF + PDFBox 逐页渲染 PNG。
 *   - 相比 Apache POI 的 slide.draw()，LibreOffice 能正确处理 SmartArt/图表/动画/复杂形状，
 *     转换速度快、首屏渲染稳定，适合教师上传后多学生并发预览的场景。
 *
 * 并发策略：
 *   - 结果磁盘缓存：按「文件名+大小+最后修改时间」定位缓存目录，同一文件只渲染一次。
 *   - 进程内互斥：同一文件首次渲染时，后续请求复用同一个 Future，等待结果而非再次拉起 soffice，
 *     避免高并发下多个 soffice 进程互相抢占 profile 导致转换失败。
 *
 * 失败降级：LibreOffice 不可用或转换失败时，抛出明确的 BizException，由前端引导用户下载原文件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {

    private final AppProperties appProperties;

    /** 进程内转换互斥：缓存键 -> 正在进行的转换任务 */
    private final Map<String, java.util.concurrent.CompletableFuture<List<String>>> inFlight = new ConcurrentHashMap<>();

    @Override
    public boolean support(String ext) {
        if (ext == null) return false;
        String e = ext.toLowerCase();
        return e.equals("ppt") || e.equals("pptx") || e.equals("pdf");
    }

    @Override
    public List<String> toImages(String filePath) {
        File file = resolveFile(filePath);
        if (!file.exists()) {
            throw new BizException(404, "文件不存在：" + filePath);
        }
        String ext = extOf(file.getName());
        if (!support(ext)) {
            throw new BizException(400, "该文件类型不支持在线预览，请下载查看");
        }

        String hash = cacheHash(file);

        // 1. 命中磁盘缓存，直接返回
        List<String> cached = tryLoadCache(hash);
        if (cached != null) {
            return cached;
        }

        // 2. 未命中缓存：同一文件只允许一个转换任务在跑，其余请求等待复用结果
        java.util.concurrent.CompletableFuture<List<String>> future = inFlight.computeIfAbsent(hash, k ->
                java.util.concurrent.CompletableFuture.supplyAsync(() -> doRender(file, hash))
        );
        try {
            List<String> urls = future.get(180, TimeUnit.SECONDS);
            return urls;
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("文档预览转换超时：{}", file.getName());
            throw new BizException(500, "文档预览生成超时，请稍后重试");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BizException) {
                throw (BizException) cause;
            }
            log.error("文档预览转换失败", cause);
            throw new BizException(500, "文档预览生成失败，请下载原文件查看");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(500, "文档预览生成被中断，请重试");
        } finally {
            // 任务结束后移除互斥记录，允许文件替换后重新转换
            if (future.isDone()) {
                inFlight.remove(hash, future);
            }
        }
    }

    /**
     * 执行转换：LibreOffice 转 PDF → PDFBox 渲染 PNG → 落盘并返回 URL 列表。
     */
    private List<String> doRender(File file, String hash) {
        try {
            File pdf = file;
            String ext = extOf(file.getName());
            // PPT/PPTX 需先转 PDF；PDF 直接用
            if (!ext.equals("pdf")) {
                pdf = convertToPdfWithLibreOffice(file);
                if (pdf == null || !pdf.exists()) {
                    throw new BizException(500, "PPT 转换失败：请确认服务器已安装 LibreOffice");
                }
            }
            List<BufferedImage> images = renderPdf(pdf);
            if (images.isEmpty()) {
                throw new BizException(500, "文档内容为空，无法生成预览");
            }
            return saveImages(images, hash);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档预览转换失败", e);
            throw new BizException(500, "文档预览生成失败：" + e.getMessage());
        }
    }

    /**
     * 调用 LibreOffice 无头模式将 PPT/PPTX 转为 PDF。
     * 返回生成的 PDF 文件；失败返回 null。
     */
    private File convertToPdfWithLibreOffice(File source) {
        Path profile = null;
        try {
            File outDir = Files.createTempDirectory("lo-preview").toFile();
            String soffice = appProperties.getSofficePath();
            // 为每个转换任务指定独立 profile 目录，避免并发时共享 profile 抢占锁
            profile = Files.createTempDirectory("lo-profile");
            ProcessBuilder pb = new ProcessBuilder(
                    soffice,
                    "--headless",
                    "-env:UserInstallation=file://" + profile.toAbsolutePath(),
                    "--convert-to", "pdf",
                    "--outdir", outDir.getAbsolutePath(),
                    source.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("LibreOffice 转换超时：{}", source.getName());
                return null;
            }
            if (p.exitValue() != 0) {
                log.warn("LibreOffice 转换退出码非 0：{}", p.exitValue());
                return null;
            }
            String base = source.getName().substring(0, source.getName().lastIndexOf('.'));
            File pdf = new File(outDir, base + ".pdf");
            return pdf.exists() ? pdf : null;
        } catch (Exception e) {
            log.warn("LibreOffice 转换异常：{}", e.getMessage());
            return null;
        } finally {
            deleteQuietly(profile);
        }
    }

    /** 递归删除临时目录，失败忽略 */
    private void deleteQuietly(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignore) {}
                    });
        } catch (Exception ignore) {
        }
    }

    /**
     * 从磁盘缓存目录读取已渲染的图片 URL；缓存不完整则返回 null。
     */
    private List<String> tryLoadCache(String hash) {
        try {
            Path cacheDir = Paths.get(appProperties.getUploadDir(), "preview-cache", hash);
            Path manifest = cacheDir.resolve(".manifest");
            if (!Files.exists(cacheDir) || !Files.exists(manifest)) {
                return null;
            }
            List<String> urls = new ArrayList<>();
            int i = 1;
            while (Files.exists(cacheDir.resolve("slide_" + i + ".png"))) {
                urls.add("/uploads/preview-cache/" + hash + "/slide_" + i + ".png");
                i++;
            }
            return urls.isEmpty() ? null : urls;
        } catch (Exception e) {
            log.warn("读取预览缓存失败，忽略并重新渲染", e);
            return null;
        }
    }

    private String cacheHash(File file) {
        String key = file.getName() + "_" + file.length() + "_" + file.lastModified();
        return String.valueOf(key.hashCode());
    }

    private File resolveFile(String filePath) {
        File f = new File(filePath);
        if (f.exists()) return f;
        Path p = Paths.get(appProperties.getUploadDir());
        return p.resolve(filePath.replaceFirst("^/uploads/", "")).toFile();
    }

    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private List<BufferedImage> renderPdf(File file) throws Exception {
        List<BufferedImage> result = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(file)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = doc.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, appProperties.getPreviewDpi());
                result.add(img);
            }
        }
        return result;
    }

    private List<String> saveImages(List<BufferedImage> images, String hash) throws Exception {
        Path dir = Paths.get(appProperties.getUploadDir(), "preview-cache", hash);
        Files.createDirectories(dir);
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String name = "slide_" + (i + 1) + ".png";
            File out = dir.resolve(name).toFile();
            ImageIO.write(images.get(i), "png", out);
            urls.add("/uploads/preview-cache/" + hash + "/slide_" + (i + 1) + ".png");
        }
        try {
            Files.write(dir.resolve(".manifest"), ("complete_" + images.size()).getBytes());
        } catch (Exception ignore) {
        }
        return urls;
    }
}
