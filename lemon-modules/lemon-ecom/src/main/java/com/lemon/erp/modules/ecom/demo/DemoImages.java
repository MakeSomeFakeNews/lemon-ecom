package com.lemon.erp.modules.ecom.demo;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * 演示商品图片：用 Java2D 画简洁的产品示意图（浅色渐变底 + 品类图标 + 名称首字母 + 品牌），输出 PNG。
 * 只用英文字符，避免服务器没有中文字体时出现方框
 */
public final class DemoImages {

    /** 图标（品类示意） */
    public enum Icon {
        BATTERY, CABLE, PLUG, HEADPHONES, PHONE, KEYBOARD, LIGHT, CUP, BOX, PILLOW, MAT, DROP, PAW, LANTERN, CHAIR, BOTTLE, BONE, HEART
    }

    private static final int SIZE = 600;

    private DemoImages() {
    }

    /**
     * @param color  主色（#RRGGBB）
     * @param title  英文名称（取首字母）
     * @param brand  品牌（底部小字）
     */
    public static byte[] render(String color, Icon icon, String title, String brand) {
        Color main = Color.decode(color);
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            // 背景：主色的浅色渐变
            g.setPaint(new GradientPaint(0, 0, mix(main, Color.WHITE, 0.88), SIZE, SIZE, mix(main, Color.WHITE, 0.70)));
            g.fillRect(0, 0, SIZE, SIZE);
            // 产品底座阴影
            g.setColor(new Color(0, 0, 0, 22));
            g.fill(new Ellipse2D.Double(150, 455, 300, 36));
            // 图标
            g.setColor(main);
            g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawIcon(g, icon, main);
            // 名称首字母徽标
            String initials = initials(title.toLowerCase().startsWith(brand.toLowerCase() + " ") ? title.substring(brand.length() + 1) : title);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(470, 30, 100, 100));
            g.setColor(main);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, initials.length() > 1 ? 40 : 52));
            center(g, initials, 520, 80);
            // 品牌
            g.setColor(mix(main, Color.BLACK, 0.35));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            center(g, brand.toUpperCase(), SIZE / 2, 545);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void drawIcon(Graphics2D g, Icon icon, Color main) {
        Color light = mix(main, Color.WHITE, 0.55);
        Color dark = mix(main, Color.BLACK, 0.25);
        switch (icon) {
            case BATTERY -> {
                fillRound(g, main, 190, 130, 220, 330, 40);
                fillRound(g, dark, 255, 100, 90, 40, 14);
                g.setColor(Color.WHITE);
                Path2D bolt = new Path2D.Double();
                bolt.moveTo(315, 180); bolt.lineTo(255, 300); bolt.lineTo(300, 300); bolt.lineTo(280, 410); bolt.lineTo(350, 270); bolt.lineTo(305, 270);
                bolt.closePath();
                g.fill(bolt);
            }
            case CABLE -> {
                g.setColor(main);
                g.setStroke(new BasicStroke(22, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(200, 140); p.curveTo(100, 300, 500, 260, 400, 430);
                g.draw(p);
                fillRound(g, dark, 165, 90, 70, 80, 16);
                fillRound(g, dark, 365, 410, 70, 80, 16);
            }
            case PLUG -> {
                fillRound(g, main, 180, 170, 240, 240, 50);
                g.setColor(dark);
                g.fill(new RoundRectangle2D.Double(230, 100, 30, 90, 12, 12));
                g.fill(new RoundRectangle2D.Double(340, 100, 30, 90, 12, 12));
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
                center(g, "65W", 300, 300);
            }
            case HEADPHONES -> {
                g.setColor(main);
                g.setStroke(new BasicStroke(26, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(160, 110, 280, 300, 0, 180, Arc2D.OPEN));
                fillRound(g, dark, 135, 260, 90, 160, 40);
                fillRound(g, dark, 375, 260, 90, 160, 40);
            }
            case PHONE -> {
                fillRound(g, dark, 215, 95, 170, 360, 36);
                fillRound(g, light, 230, 125, 140, 290, 18);
                g.setColor(main);
                g.fill(new Ellipse2D.Double(285, 425, 30, 20));
            }
            case KEYBOARD -> {
                fillRound(g, main, 110, 200, 380, 200, 30);
                g.setColor(Color.WHITE);
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 7; c++) {
                        g.fill(new RoundRectangle2D.Double(135 + c * 49, 225 + r * 50, 38, 36, 8, 8));
                    }
                }
                g.fill(new RoundRectangle2D.Double(210, 375, 180, 0, 6, 6));
            }
            case LIGHT -> {
                g.setColor(light);
                g.fill(new Ellipse2D.Double(180, 110, 240, 240));
                g.setColor(main);
                g.fill(new Ellipse2D.Double(215, 145, 170, 170));
                fillRound(g, dark, 255, 330, 90, 70, 14);
                fillRound(g, dark, 270, 405, 60, 40, 12);
            }
            case CUP -> {
                fillRound(g, main, 215, 120, 170, 330, 30);
                fillRound(g, dark, 205, 100, 190, 50, 18);
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(240, 200, 24, 180, 12, 12));
            }
            case BOX -> {
                Path2D top = new Path2D.Double();
                top.moveTo(300, 120); top.lineTo(470, 200); top.lineTo(300, 280); top.lineTo(130, 200); top.closePath();
                g.setColor(light);
                g.fill(top);
                Path2D left = new Path2D.Double();
                left.moveTo(130, 200); left.lineTo(300, 280); left.lineTo(300, 460); left.lineTo(130, 380); left.closePath();
                g.setColor(main);
                g.fill(left);
                Path2D right = new Path2D.Double();
                right.moveTo(470, 200); right.lineTo(300, 280); right.lineTo(300, 460); right.lineTo(470, 380); right.closePath();
                g.setColor(dark);
                g.fill(right);
            }
            case PILLOW -> {
                fillRound(g, main, 120, 190, 360, 220, 110);
                g.setColor(light);
                g.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(170, 230, 260, 140, 200, 140, Arc2D.OPEN));
            }
            case MAT -> {
                fillRound(g, main, 120, 300, 360, 90, 20);
                g.setColor(dark);
                g.fill(new Ellipse2D.Double(380, 220, 150, 170));
                g.setColor(light);
                g.fill(new Ellipse2D.Double(415, 260, 80, 90));
            }
            case DROP -> {
                Path2D d = new Path2D.Double();
                d.moveTo(300, 100); d.curveTo(380, 220, 430, 290, 430, 340); d.curveTo(430, 420, 370, 460, 300, 460);
                d.curveTo(230, 460, 170, 420, 170, 340); d.curveTo(170, 290, 220, 220, 300, 100); d.closePath();
                g.setColor(main);
                g.fill(d);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(225, 300, 40, 70));
            }
            case PAW -> {
                g.setColor(main);
                g.fill(new Ellipse2D.Double(210, 260, 180, 170));
                g.fill(new Ellipse2D.Double(160, 180, 70, 90));
                g.fill(new Ellipse2D.Double(235, 120, 70, 95));
                g.fill(new Ellipse2D.Double(310, 120, 70, 95));
                g.fill(new Ellipse2D.Double(385, 180, 70, 90));
            }
            case LANTERN -> {
                g.setColor(dark);
                g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(240, 80, 120, 100, 0, 180, Arc2D.OPEN));
                fillRound(g, dark, 215, 140, 170, 40, 14);
                fillRound(g, light, 230, 180, 140, 210, 30);
                g.setColor(main);
                g.fill(new Ellipse2D.Double(265, 230, 70, 110));
                fillRound(g, dark, 215, 390, 170, 50, 14);
            }
            case CHAIR -> {
                g.setColor(main);
                g.fill(new RoundRectangle2D.Double(180, 130, 240, 170, 30, 30));
                g.fill(new RoundRectangle2D.Double(160, 290, 280, 60, 30, 30));
                g.setColor(dark);
                g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(190, 350, 150, 450));
                g.draw(new Line2D.Double(410, 350, 450, 450));
                g.draw(new Line2D.Double(190, 350, 450, 450));
                g.draw(new Line2D.Double(410, 350, 150, 450));
            }
            case BOTTLE -> {
                fillRound(g, dark, 260, 90, 80, 50, 14);
                fillRound(g, main, 225, 130, 150, 330, 50);
                g.setColor(light);
                g.fill(new RoundRectangle2D.Double(225, 250, 150, 80, 0, 0));
            }
            case BONE -> {
                g.setColor(main);
                g.fill(new RoundRectangle2D.Double(190, 260, 220, 70, 30, 30));
                for (int[] c : new int[][]{{140, 215}, {140, 295}, {400, 215}, {400, 295}}) {
                    g.fill(new Ellipse2D.Double(c[0], c[1], 85, 85));
                }
            }
            case HEART -> {
                Path2D h = new Path2D.Double();
                h.moveTo(300, 450); h.curveTo(120, 330, 120, 160, 220, 150); h.curveTo(270, 145, 295, 190, 300, 215);
                h.curveTo(305, 190, 330, 145, 380, 150); h.curveTo(480, 160, 480, 330, 300, 450); h.closePath();
                g.setColor(main);
                g.fill(h);
            }
            default -> fillRound(g, main, 180, 150, 240, 300, 40);
        }
    }

    private static void fillRound(Graphics2D g, Color c, double x, double y, double w, double h, double r) {
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    private static void center(Graphics2D g, String text, int cx, int baselineCenterY) {
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        int y = baselineCenterY + (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, x, y);
    }

    private static String initials(String title) {
        StringBuilder sb = new StringBuilder();
        for (String w : title.split("[\\s-]+")) {
            if (!w.isEmpty() && Character.isLetter(w.charAt(0)) && sb.length() < 2) {
                sb.append(Character.toUpperCase(w.charAt(0)));
            }
        }
        return sb.isEmpty() ? "D" : sb.toString();
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() * (1 - t) + b.getRed() * t), (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    /** 内存中的上传文件，交给物料图片上传接口（与页面上传同一逻辑） */
    public static MultipartFile file(String fileName, byte[] bytes) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(File dest) throws IOException {
                Files.write(dest.toPath(), bytes);
            }
        };
    }
}
