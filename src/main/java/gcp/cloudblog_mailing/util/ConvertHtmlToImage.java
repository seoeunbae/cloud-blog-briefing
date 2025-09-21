package gcp.cloudblog_mailing.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.simple.Graphics2DRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@Component
public class ConvertHtmlToImage {
    public BufferedImage convertHtmlToImage(String htmlContent, Integer count) {
        int width = 1000;
        int height = 1400 * count + 350;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        log.info("image width: {}, height: {}", width, height);
        log.info("image content: {}", htmlContent);
        Graphics2DRenderer renderer = new Graphics2DRenderer();
        renderer.setDocument(htmlContent);
        renderer.render(graphics);
        graphics.dispose();
        return image;
    }
}

