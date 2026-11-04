package gesture;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Moments;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/** Finds the largest skin-coloured blob and counts extended fingers using convexity defects. */
public class HandDetector {

    public static class Result {
        public boolean found = false;
        public int fingers = 0;
        public double tipX, tipY;   // top-most point (fingertip), in the image passed to detect()
        public double palmX, palmY; // centre of the hand blob
    }

    private final Mat ycrcb = new Mat();
    private final Mat mask = new Mat();
    private final Mat hierarchy = new Mat();
    private final Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));

    public Result detect(Mat bgr) {
        Result r = new Result();
        Mat blur = new Mat();
        Imgproc.GaussianBlur(bgr, blur, new Size(7, 7), 0);
        Imgproc.cvtColor(blur, ycrcb, Imgproc.COLOR_BGR2YCrCb);
        blur.release();

        Core.inRange(ycrcb, new Scalar(0, 135, 85), new Scalar(255, 180, 135), mask);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
        Imgproc.dilate(mask, mask, kernel, new Point(-1, -1), 2);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat work = mask.clone();
        Imgproc.findContours(work, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        work.release();

        MatOfPoint best = null;
        double bestArea = 0;
        for (MatOfPoint c : contours) {
            double a = Imgproc.contourArea(c);
            if (a > bestArea) { bestArea = a; best = c; }
        }
        if (best == null || bestArea < 6000) return r;

        Point[] pts = best.toArray();
        if (pts.length < 5) return r;

        Moments m = Imgproc.moments(best);
        if (m.get_m00() == 0) return r;
        r.palmX = m.get_m10() / m.get_m00();
        r.palmY = m.get_m01() / m.get_m00();

        Point top = pts[0];
        for (Point p : pts) if (p.y < top.y) top = p;
        r.tipX = top.x;
        r.tipY = top.y;

        int defectCount = 0;
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(best, hull);
        if (hull.rows() > 3) {
            MatOfInt4 defects = new MatOfInt4();
            Imgproc.convexityDefects(best, hull, defects);
            if (!defects.empty()) {
                int[] d = defects.toArray();
                double minDepth = Math.sqrt(bestArea) * 0.18;
                for (int i = 0; i + 3 < d.length; i += 4) {
                    Point s = pts[d[i]];
                    Point e = pts[d[i + 1]];
                    Point f = pts[d[i + 2]];
                    double depth = d[i + 3] / 256.0;
                    double a = dist(s, e), b = dist(s, f), c = dist(e, f);
                    if (b == 0 || c == 0) continue;
                    double cos = (b * b + c * c - a * a) / (2 * b * c);
                    double angle = Math.acos(Math.max(-1, Math.min(1, cos)));
                    if (angle <= Math.PI / 2 && depth > minDepth && f.y < r.palmY + 40) defectCount++;
                }
            }
            defects.release();
        }
        hull.release();

        if (defectCount > 0) {
            r.fingers = Math.min(5, defectCount + 1);
        } else {
            double radius = Math.sqrt(bestArea / Math.PI);
            double reach = Math.hypot(r.tipX - r.palmX, r.tipY - r.palmY);
            r.fingers = reach > radius * 1.35 ? 1 : 0;
        }
        r.found = true;
        return r;
    }

    private static double dist(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }
}
