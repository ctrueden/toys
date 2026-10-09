import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Plots N random points around a circle, connects every pair with a line
 * segment, and counts how many pairs of segments cross. Points can be
 * dragged with the mouse; the count updates live. Right-click on empty
 * space adds a point; right-click on a point removes it.
 * <p>
 * Usage: {@code java PointIntersections.java N}
 */
public class PointIntersections extends JPanel {

	private static final int SIZE = 700;
	private static final double POINT_RADIUS = 6;
	private static final double GRAB_RADIUS = 10;
	private static final double EPS = 1e-9;

	private final List<Point2D.Double> points = new ArrayList<>();
	private final List<Point2D.Double> crossings = new ArrayList<>();
	private int dragIndex = -1;

	public PointIntersections(int n) {
		setPreferredSize(new Dimension(SIZE, SIZE));
		setBackground(Color.WHITE);

		Random r = new Random();
		double cx = SIZE / 2.0, cy = SIZE / 2.0, radius = SIZE * 0.4;
		for (int i = 0; i < n; i++) {
			double theta = 2 * Math.PI * r.nextDouble();
			points.add(new Point2D.Double(cx + radius * Math.cos(theta), cy + radius * Math.sin(theta)));
		}
		recalculate();

		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int hit = findPoint(e.getX(), e.getY());
				if (SwingUtilities.isRightMouseButton(e)) {
					if (hit >= 0) points.remove(hit);
					else points.add(new Point2D.Double(e.getX(), e.getY()));
					recalculate();
					repaint();
					return;
				}
				dragIndex = hit;
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (dragIndex < 0) return;
				points.get(dragIndex).setLocation(e.getX(), e.getY());
				recalculate();
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragIndex = -1;
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	private int findPoint(double x, double y) {
		// Note: search backwards so the topmost-drawn point wins.
		for (int i = points.size() - 1; i >= 0; i--) {
			if (points.get(i).distance(x, y) <= GRAB_RADIUS) return i;
		}
		return -1;
	}

	/** Finds every crossing between two segments that share no endpoint. */
	private void recalculate() {
		crossings.clear();
		int n = points.size();
		List<int[]> segs = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) segs.add(new int[] {i, j});
		}
		for (int s = 0; s < segs.size(); s++) {
			int[] a = segs.get(s);
			for (int t = s + 1; t < segs.size(); t++) {
				int[] b = segs.get(t);
				if (a[0] == b[0] || a[0] == b[1] || a[1] == b[0] || a[1] == b[1]) continue;
				Point2D.Double p = intersect(points.get(a[0]), points.get(a[1]),
					points.get(b[0]), points.get(b[1]));
				if (p != null) crossings.add(p);
			}
		}
	}

	/** Returns the intersection point of segments p1p2 and p3p4, or null. */
	private static Point2D.Double intersect(Point2D.Double p1, Point2D.Double p2,
		Point2D.Double p3, Point2D.Double p4)
	{
		double d1x = p2.x - p1.x, d1y = p2.y - p1.y;
		double d2x = p4.x - p3.x, d2y = p4.y - p3.y;
		double denom = d1x * d2y - d1y * d2x;
		if (Math.abs(denom) < EPS) return null; // Parallel or collinear.
		double ex = p3.x - p1.x, ey = p3.y - p1.y;
		double t = (ex * d2y - ey * d2x) / denom;
		double u = (ex * d1y - ey * d1x) / denom;
		if (t < 0 || t > 1 || u < 0 || u > 1) return null;
		return new Point2D.Double(p1.x + t * d1x, p1.y + t * d1y);
	}

	private static long choose4(long n) {
		return n < 4 ? 0 : n * (n - 1) * (n - 2) * (n - 3) / 24;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Reference circle.
		double radius = SIZE * 0.4;
		g2.setColor(new Color(225, 225, 225));
		//g2.draw(new Ellipse2D.Double(SIZE / 2.0 - radius, SIZE / 2.0 - radius, 2 * radius, 2 * radius));

		// Segments between all pairs.
		g2.setColor(new Color(70, 110, 180, 160));
		g2.setStroke(new BasicStroke(1f));
		int n = points.size();
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) g2.draw(new Line2D.Double(points.get(i), points.get(j)));
		}

		// Crossings.
		g2.setColor(new Color(220, 60, 60));
		double cr = 2.5;
		for (Point2D.Double p : crossings) {
			g2.fill(new Ellipse2D.Double(p.x - cr, p.y - cr, 2 * cr, 2 * cr));
		}

		// Points.
		for (int i = 0; i < n; i++) {
			Point2D.Double p = points.get(i);
			g2.setColor(i == dragIndex ? Color.ORANGE : Color.BLACK);
			g2.fill(new Ellipse2D.Double(p.x - POINT_RADIUS, p.y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS));
		}

		// Stats.
		g2.setColor(Color.BLACK);
		g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		g2.drawString("Crossings: " + crossings.size(), 12, 24);
		g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		g2.drawString("N = " + n + ", C(N,4) = " + choose4(n), 12, 42);
	}

	public static void main(String[] args) {
		int n = args.length >= 1 ? Integer.parseInt(args[0]) : 5;
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Point Intersections");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new PointIntersections(n));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
