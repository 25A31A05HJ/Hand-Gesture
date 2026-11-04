package gesture;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/** Transparent layer. Moving the fingertip stamps small coloured flowers instead of drawing a line. */
public class FlowerLayer {
    private final BufferedImage img;
    private final Graphics2D g;
    private final Random rnd = new Random();
    private double lastX = -1, lastY = -1;
    private Color color = new Color(0xE91E63);
    private boolean eraser = false;
    private final int size = 18;

    public FlowerLayer(int w, int h) {
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public BufferedImage image() { return img; }

    public void setColor(Color c) { color = c; eraser = false; }

    public void setEraser(boolean e) { eraser = e; }

    public boolean isEraser() { return eraser; }

    /** Call every frame while the index finger is drawing. */
    public void stamp(int x, int y) {
        if (eraser) { erase(x, y); return; }
        if (lastX >= 0 && Math.hypot(x - lastX, y - lastY) < size * 1.6) return;
        flower(x, y);
        lastX = x;
        lastY = y;
    }

    /** Call when the finger stops drawing so the next stroke starts fresh. */
    public void lift() { lastX = -1; lastY = -1; }

    public void erase(int x, int y) {
        g.setComposite(AlphaComposite.Clear);
        g.fillOval(x - 35, y - 35, 70, 70);
        g.setComposite(AlphaComposite.SrcOver);
    }

    public void clear() {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setComposite(AlphaComposite.SrcOver);
        lift();
    }

    private void flower(int x, int y) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rnd.nextDouble() * Math.PI * 2);
        g.setColor(color);
        for (int i = 0; i < 6; i++) {
            g.rotate(Math.PI / 3);
            g.fillOval(-size / 5, -size, 2 * size / 5, size);
        }
        g.setColor(new Color(0xFFD54F));
        g.fillOval(-size / 4, -size / 4, size / 2, size / 2);
        g.setTransform(old);
    }

    public void savePng(File f) {
        try {
            BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D og = out.createGraphics();
            og.drawImage(img, 0, 0, null);
            og.dispose();
            ImageIO.write(out, "png", f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
