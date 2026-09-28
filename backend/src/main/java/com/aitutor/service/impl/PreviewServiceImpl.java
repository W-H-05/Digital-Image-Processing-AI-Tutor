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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档在线预览：PPT/PPTX/PDF 渲染为 PNG 图片序列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {

    private final AppProperties appProperties;
    private static final int SCALE = 2; // 渲染放大倍数

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
            List<BufferedImage> images;
            if (ext.equals("pdf")) {
                images = renderPdf(file);
            } else if (ext.equals("pptx")) {
                images = renderPptx(file);
            } else if (ext.equals("ppt")) {
                images = renderPpt(file);
            } else {
                throw new BizException(400, "不支持的文件类型");
            }
            return saveImages(images, file.getName());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档预览转换失败", e);
            throw new BizException(500, "文档预览转换失败：" + e.getMessage());
        }
    }

    private File resolveFile(String filePath) {
        File f = new File(filePath);
        if (f.exists()) return f;
        // 尝试相对 upload-dir
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
                BufferedImage img = renderer.renderImageWithDPI(i, 120);
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

    private List<String> saveImages(List<BufferedImage> images, String sourceName) throws Exception {
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path dir = Paths.get(appProperties.getUploadDir(), "preview", dateDir, uid);
        Files.createDirectories(dir);
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String name = "slide_" + (i + 1) + ".png";
            File out = dir.resolve(name).toFile();
            ImageIO.write(images.get(i), "png", out);
            urls.add("/uploads/preview/" + dateDir + "/" + uid + "/" + name);
        }
        return urls;
    }
}
