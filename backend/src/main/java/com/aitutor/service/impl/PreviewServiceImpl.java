package com.aitutor.service.impl;

import com.aitutor.common.BizException;
import com.aitutor.config.AppProperties;
import com.aitutor.service.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文档在线预览：PPT/PPTX/PDF 渲染为 PNG 图片序列。
 *
 * 渲染策略（按保真度与速度排序）：
 *  1. PPT/PPTX 优先用 LibreOffice（soffice --headless）转 PDF，再用 PDFBox 逐页渲染 PNG。
 *     相比 POI 的 slide.draw()，LibreOffice 能正确处理 SmartArt/图表/动画/复杂形状，
 *     且转换速度更快、首屏渲染更稳定。
 *  2. LibreOffice 不可用或转换失败时，回退到 POI 直接绘制。
 *  3. PDF 直接用 PDFBox 渲染。
 *
 * 所有渲染结果按「文件名+大小+最后修改时间」做磁盘缓存，同一文件只渲染一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {

    private final AppProperties appProperties;
    private static final int SCALE = 2; // POI 回退渲染放大倍数

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
        try {
            // 磁盘缓存：命中则直接返回，避免重复全量渲染
            List<String> cached = tryLoadCache(file);
            if (cached != null) {
                return cached;
            }
            List<BufferedImage> images = renderImages(file, ext);
            return saveImages(images, file);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档预览转换失败", e);
            throw new BizException(500, "文档预览转换失败：" + e.getMessage());
        }
    }

    /**
     * 渲染主流程：PPT/PPTX 优先 LibreOffice 转 PDF，失败回退 POI；PDF 直接 PDFBox。
     */
    private List<BufferedImage> renderImages(File file, String ext) throws Exception {
        if (ext.equals("pdf")) {
            return renderPdf(file);
        }
        // PPT/PPTX：优先 LibreOffice
        try {
            File pdf = convertToPdfWithLibreOffice(file);
            if (pdf != null && pdf.exists()) {
                List<BufferedImage> images = renderPdf(pdf);
                if (!images.isEmpty()) {
                    return images;
                }
            }
        } catch (Exception e) {
            log.warn("LibreOffice 转换失败，回退到 POI 渲染：{}", e.getMessage());
        }
        // 回退：POI 直接绘制
        if (ext.equals("pptx")) {
            return renderPptx(file);
        }
        return renderPpt(file);
    }

    /**
     * 调用 LibreOffice 无头模式将 PPT/PPTX 转为 PDF。
     * 返回生成的 PDF 文件，失败返回 null。
     */
    private File convertToPdfWithLibreOffice(File source) {
        File outDir = null;
        try {
            outDir = Files.createTempDirectory("lo-preview").toFile();
            String soffice = appProperties.getSofficePath();
            ProcessBuilder pb = new ProcessBuilder(
                    soffice,
                    "--headless",
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
        }
    }

    /**
     * 计算源文件的缓存键：文件名 + 文件大小 + 最后修改时间，命中则直接返回已渲染的图片 URL。
     */
    private List<String> tryLoadCache(File file) {
        try {
            String hash = cacheHash(file);
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

    private List<BufferedImage> renderPptx(File file) throws Exception {
        List<BufferedImage> result = new ArrayList<>();
        try (XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(file))) {
            Dimension size = ppt.getPageSize();
            for (XSLFSlide slide : ppt.getSlides()) {
                BufferedImage img = new BufferedImage(
                        (int) size.getWidth() * SCALE, (int) size.getHeight() * SCALE,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.scale(SCALE, SCALE);
                slide.draw(g);
                g.dispose();
                result.add(img);
            }
        }
        return result;
    }

    private List<BufferedImage> renderPpt(File file) throws Exception {
        List<BufferedImage> result = new ArrayList<>();
        try (HSLFSlideShow ppt = new HSLFSlideShow(new FileInputStream(file))) {
            Dimension size = ppt.getPageSize();
            for (HSLFSlide slide : ppt.getSlides()) {
                BufferedImage img = new BufferedImage(
                        (int) size.getWidth() * SCALE, (int) size.getHeight() * SCALE,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.scale(SCALE, SCALE);
                slide.draw(g);
                g.dispose();
                result.add(img);
            }
        }
        return result;
    }

    private List<String> saveImages(List<BufferedImage> images, File sourceFile) throws Exception {
        String hash = cacheHash(sourceFile);
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
