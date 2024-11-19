package classesSeparated;

import java.awt.*;

public class ExplosionEffect {
    private final double x, y;
    private final int radius;
    private final Color color;
    private final long endTime;

    public ExplosionEffect(double x, double y, int radius, Color color, int duration) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.endTime = System.currentTimeMillis() + duration;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
        g2d.fillOval((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
    }

    public boolean isExpired(long currentTime) {
        return currentTime > endTime;
    }

}
