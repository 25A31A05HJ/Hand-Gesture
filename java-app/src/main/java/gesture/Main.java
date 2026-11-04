package gesture;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hand Gesture-Controlled Presentation System (Java + OpenCV).
 *
 * 1 finger  = draw flowers      2 fingers = select toolbar item (hold still)
 * 3 fingers = erase             open palm swipe right/left = next / previous slide
 * fist      = pause
 */
public class Main {

    static final Color LIGHT_YELLOW = new Color(0xFFF6C2);
    static final Color DARK_PINK = new Color(0xC2185B);
    static final String[] ITEMS = {"Clear", "Pink", "Purple", "Blue", "Orange", "Red", "Eraser", "Save"};
    static final Color[] ITEM_COLORS = {
            new Color(0x555555), new Color(0xE91E63), new Color(0xAB47BC), new Color(0x42A5F5),
            new Color(0xFF9800), new Color(0xE53935), new Color(0x888888), new Color(0x2E7D32)};
    static final int TB_X = 16, TB_Y = 70, TB_W = 130, TB_H = 44, TB_GAP = 8;

    private final View view = new View();
    private final JCheckBox sendKeys = new JCheckBox("Send arrow keys to my presentation app (switch to it after ticking)");
    private volatile boolean running = true;
    private Robot robot;
    private int slide = 1;
    private String message = "";
    private long messageUntil = 0;

    public static void main(String[] args) {
        OpenCV.loadLocally();
        SwingUtilities.invokeLater(Main::new);
    }

    public Main() {
        try { robot = new Robot(); } catch (Exception e) { robot = null; }

        JFrame frame = new JFrame("Gesture Presenter - flowers, drawing and slide control");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(LIGHT_YELLOW);
        sendKeys.setBackground(LIGHT_YELLOW);
        sendKeys.setForeground(DARK_PINK);
        view.setPreferredSize(new Dimension(960, 540));
        frame.add(view, BorderLayout.CENTER);
        frame.add(sendKeys, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { running = false; }
        });

        Thread t = new Thread(this::loop, "camera-loop");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        VideoCapture cap = new VideoCapture(0);
        if (!cap.isOpened()) {
            JOptionPane.showMessageDialog(null, "Could not open the webcam. Close other apps using it and allow camera access.");
            return;
        }
        cap.set(Videoio.CAP_PROP_FRAME_WIDTH, 960);
        cap.set(Videoio.CAP_PROP_FRAME_HEIGHT, 540);

        Mat frame = new Mat();
        HandDetector detector = new HandDetector();
        FlowerLayer layer = null;
        Deque<double[]> history = new ArrayDeque<>();
        int[] recent = new int[5];
        int ri = 0;
        double sx = -1, sy = -1;
        long lastSwipe = 0;
        int dwell = 0, lastItem = -1;
        long fpsMark = System.nanoTime();
        int frames = 0;
        double fps = 0;

        try {
            while (running) {
                if (!cap.read(frame) || frame.empty()) { Thread.sleep(10); continue; }
                Core.flip(frame, frame, 1);
                int w = frame.cols(), h = frame.rows();

                if (layer == null) {
                    layer = new FlowerLayer(w, h);
                    view.setLayer(layer, w, h);
                }

                // The hand must stay inside the pink box (keeps your face out of the skin detector).
                Rect roi = new Rect((int) (w * 0.45), 0, (int) (w * 0.55), (int) (h * 0.85));
                Mat sub = frame.submat(roi);
                HandDetector.Result r = detector.detect(sub);
                sub.release();

                String status = "Show your hand inside the pink box";
                int hover = -1;

                if (r.found) {
                    recent[ri++ % recent.length] = r.fingers;
                    int g = mode(recent);
                    double nx = r.tipX / roi.width * w;
                    double ny = r.tipY / roi.height * h;
                    if (sx < 0) { sx = nx; sy = ny; } else { sx = sx * 0.6 + nx * 0.4; sy = sy * 0.6 + ny * 0.4; }
                    int px = (int) sx, py = (int) sy;
                    view.setCursor(px, py);

                    if (g == 1) {
                        status = layer.isEraser() ? "ERASING" : "DRAWING FLOWERS";
                        layer.stamp(px, py);
                        history.clear();
                        dwell = 0;
                    } else if (g == 2) {
                        status = "SELECT - hold your fingertip on a button";
                        layer.lift();
                        hover = itemAt(px, py);
                        if (hover >= 0 && hover == lastItem) dwell++; else dwell = 0;
                        if (hover >= 0 && dwell >= 12) {
                            activate(hover, layer);
                            dwell = -15;
                        }
                        history.clear();
                    } else if (g == 3) {
                        status = "ERASING";
                        layer.lift();
                        layer.erase(px, py);
                        history.clear();
                    } else if (g >= 4) {
                        status = "PALM - swipe right for next, left for previous";
                        layer.lift();
                        long now = System.currentTimeMillis();
                        history.addLast(new double[]{now, r.palmX});
                        while (!history.isEmpty() && now - history.peekFirst()[0] > 600) history.removeFirst();
                        if (now - lastSwipe > 1100 && history.size() > 3) {
                            double dx = history.peekLast()[1] - history.peekFirst()[1];
                            if (Math.abs(dx) > roi.width * 0.30) {
                                changeSlide(dx > 0);
                                lastSwipe = now;
                                history.clear();
                            }
                        }
                    } else {
                        status = "PAUSED (fist)";
                        layer.lift();
                        history.clear();
                    }
                    lastItem = hover;
                } else {
                    layer.lift();
                    view.setCursor(-1, -1);
                    history.clear();
                    dwell = 0;
                    lastItem = -1;
                }

                frames++;
                long nowN = System.nanoTime();
                if (nowN - fpsMark > 1_000_000_000L) {
                    fps = frames * 1e9 / (nowN - fpsMark);
                    frames = 0;
                    fpsMark = nowN;
                }

                String shown = System.currentTimeMillis() < messageUntil ? message : status;
                view.update(toImage(frame), roi, shown, slide, fps, hover, Math.max(0, dwell) / 12.0);
            }
        } catch (InterruptedException ignored) {
            // exit
        } finally {
            cap.release();
        }
    }

    private void changeSlide(boolean next) {
        slide = Math.max(1, slide + (next ? 1 : -1));
        message = next ? "NEXT SLIDE  >>  " + slide : "<<  PREVIOUS SLIDE  " + slide;
        messageUntil = System.currentTimeMillis() + 900;
        if (sendKeys.isSelected() && robot != null) {
            int key = next ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT;
            robot.keyPress(key);
            robot.keyRelease(key);
        }
    }

    private void activate(int item, FlowerLayer layer) {
        switch (item) {
            case 0: layer.clear(); break;
            case 6: layer.setEraser(true); break;
            case 7:
                layer.savePng(new File("flower-drawing.png"));
                message = "Saved flower-drawing.png";
                messageUntil = System.currentTimeMillis() + 1500;
                break;
            default: layer.setColor(ITEM_COLORS[item]); break;
        }
    }

    private static int itemAt(int x, int y) {
        for (int i = 0; i < ITEMS.length; i++) {
            int top = TB_Y + i * (TB_H + TB_GAP);
            if (x >= TB_X && x <= TB_X + TB_W && y >= top && y <= top + TB_H) return i;
        }
        return -1;
    }

    private static int mode(int[] a) {
        int[] c = new int[6];
        for (int v : a) c[Math.max(0, Math.min(5, v))]++;
        int best = 0;
        for (int i = 1; i < 6; i++) if (c[i] > c[best]) best = i;
        return best;
    }

    private static BufferedImage toImage(Mat m) {
        BufferedImage img = new BufferedImage(m.cols(), m.rows(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        m.get(0, 0, data);
        return img;
    }

    /** Draws camera image, flowers, toolbar and status bar. */
    static class View extends JPanel {
        private volatile BufferedImage cam;
        private volatile FlowerLayer layer;
        private volatile Rect roi = new Rect(0, 0, 0, 0);
        private volatile String status = "Starting camera...";
        private volatile int slide = 1, hover = -1, curX = -1, curY = -1;
        private volatile double fps = 0, progress = 0;

        void setLayer(FlowerLayer l, int w, int h) {
            layer = l;
            SwingUtilities.invokeLater(() -> {
                setPreferredSize(new Dimension(w, h));
                java.awt.Window win = SwingUtilities.getWindowAncestor(this);
                if (win != null) win.pack();
            });
        }

        void setCursor(int x, int y) { curX = x; curY = y; }

        void update(BufferedImage c, Rect r, String s, int sl, double f, int hv, double prog) {
            cam = c; roi = r; status = s; slide = sl; fps = f; hover = hv; progress = prog;
            repaint();
        }

        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            Graphics2D g = (Graphics2D) gr;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(LIGHT_YELLOW);
            g.fillRect(0, 0, getWidth(), getHeight());
            BufferedImage c = cam;
            if (c == null) {
                g.setColor(DARK_PINK);
                g.setFont(new Font("SansSerif", Font.BOLD, 22));
                g.drawString(status, 30, 50);
                return;
            }
            g.drawImage(c, 0, 0, null);
            FlowerLayer l = layer;
            if (l != null) g.drawImage(l.image(), 0, 0, null);

            Rect rr = roi;
            g.setColor(DARK_PINK);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(rr.x, rr.y, rr.width, rr.height);

            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            for (int i = 0; i < ITEMS.length; i++) {
                int top = TB_Y + i * (TB_H + TB_GAP);
                g.setColor(new Color(255, 246, 194, 235));
                g.fillRoundRect(TB_X, top, TB_W, TB_H, 16, 16);
                g.setColor(i == hover ? new Color(0xFF4081) : DARK_PINK);
                g.setStroke(new BasicStroke(i == hover ? 4f : 2f));
                g.drawRoundRect(TB_X, top, TB_W, TB_H, 16, 16);
                g.setColor(ITEM_COLORS[i]);
                g.fillOval(TB_X + 10, top + TB_H / 2 - 7, 14, 14);
                g.setColor(new Color(0x4A0D2A));
                g.drawString(ITEMS[i], TB_X + 34, top + TB_H / 2 + 6);
                if (i == hover) {
                    g.setColor(new Color(0xE91E63));
                    g.fillRect(TB_X + 6, top + TB_H - 6, (int) ((TB_W - 12) * Math.min(1.0, progress)), 4);
                }
            }

            if (curX >= 0) {
                g.setColor(Color.WHITE);
                g.fillOval(curX - 9, curY - 9, 18, 18);
                g.setColor(DARK_PINK);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(curX - 9, curY - 9, 18, 18);
            }

            g.setColor(DARK_PINK);
            g.fillRect(0, 0, getWidth(), 44);
            g.setColor(LIGHT_YELLOW);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString(status, 16, 29);
            String right = String.format("Slide %d   FPS %.1f", slide, fps);
            g.drawString(right, getWidth() - g.getFontMetrics().stringWidth(right) - 16, 29);
        }
    }
}
