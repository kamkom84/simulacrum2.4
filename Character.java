package classesSeparated;

import java.util.Timer;
import java.util.TimerTask;

public abstract class Character {
    protected double x;
    protected double y;
    protected double angle = 0;
    protected String team;
    protected String role;
    protected double currentAngle;
    protected int health;
    private int bodyRadius = 10;
    private boolean active = true;
    private boolean showHealth = false;
    private String type;

    public Character(double startX, double startY, String team, String role) {
        this.x = startX;
        this.y = startY;
        this.team = team;
        this.role = role.toLowerCase();
        this.health = 10;
        this.type = type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public double distanceToAnt(Character otherAnt) {
        if (otherAnt == null) {
            return -1;
        }

        double dx = this.x - otherAnt.getX();
        double dy = this.y - otherAnt.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double getCurrentAngle() {
        return currentAngle;
    }

    public void setCurrentAngle(double angle) {
        this.currentAngle = angle;
    }

    public void decreaseHealth(int amount) {
        health -= amount;
        if (health < 0) {
            health = 0;
        }
    }

    public int getHealth() {
        return health;
    }

    public int getBodyRadius() {
        return bodyRadius;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health <= 0) {
            this.health = 0;
            this.setActive(false);
        }
    }

    public void showHealthTemporarily() {
        this.showHealth = true;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                showHealth = false;
            }
        }, 500);
    }

    public abstract String getType();

}
