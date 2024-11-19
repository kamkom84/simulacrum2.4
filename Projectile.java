package classesSeparated;

import java.awt.*;

public class Projectile {
    private int x, y;
    private int targetX, targetY;
    private int speed = 35;
    private boolean active;
    private final int maxDistance = 500;
    private int traveledDistance = 0;
    private Color color = Color.RED;

    public Projectile(int startX, int startY, int targetX, int targetY) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.active = true;
    }

    public void updatePosition() {
        if (!active) return;

        double angle = Math.atan2(targetY - y, targetX - x);
        int deltaX = (int) (speed * Math.cos(angle));
        int deltaY = (int) (speed * Math.sin(angle));

        x += deltaX;
        y += deltaY;

        traveledDistance += Math.hypot(deltaX, deltaY);

        if (Math.hypot(x - targetX, y - targetY) <= speed || traveledDistance >= maxDistance) {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void draw(Graphics g) {
        if (active) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(1));

            double angle = Math.atan2(targetY - y, targetX - x);
            int endX = (int) (x + 10 * Math.cos(angle));
            int endY = (int) (y + 10 * Math.sin(angle));

            g2d.drawLine(x, y, endX, endY);
        }
    }

    public boolean hasHit(Character target) {
        double distanceToTarget = Math.hypot(x - target.getX(), y - target.getY());
        if (distanceToTarget <= target.getBodyRadius()) {
            if (target instanceof Scout) {
                Scout scout = (Scout) target;
                scout.decreasePoints(1);
                scout.reverseDirection();
            }
            return true;
        }
        return false;
    }

}
