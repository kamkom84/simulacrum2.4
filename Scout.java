package classesSeparated;

import javax.swing.Timer;
import java.awt.*;
import java.util.List;
import static java.awt.geom.Point2D.distance;

public class Scout extends Character {
    private int points = 50;////////////////////////////////////////////////////////////////////
    private static final int MAX_POINTS = 50;///////////////////////////////////////////////////
    private static final int MIN_POINTS = 30;///////////////////////////////////////////////////
    private long lastPointReductionTime = System.currentTimeMillis();
    private long lastShootTime = 0;
    private static final int POINT_REDUCTION_INTERVAL = 60 * 1000;
    private static final int SHOOT_INTERVAL = 1000;
    private static final int MAX_BULLET_DISTANCE = 60;
    private int kills = 0;
    private ScoutGame scoutGame;
    private boolean returningToBase = false;
    private boolean recharging = false;
    private boolean isActive = true;
    private long rechargeStartTime = 0;
    private static final int RECHARGE_DURATION = 10 * 1000;
    private double currentAngle;
//    private Worker currentTargetWorker = null;
    private Resource currentTargetResource = null;
    private int resourceIndex = 0;
    private boolean isExploding = false;
    private long explosionStartTime = 0;
    private static final int EXPLOSION_DURATION = 5000;
    private static final int EXPLOSION_RADIUS = 25;
//    private int bodyRadius = 10;
    private ScoutGame game;
    private static final int BACK_STEP_DISTANCE = 150;
    private double speed;

    public Scout(double startX, double startY, String team, ScoutGame game) {
        super(startX, startY, team, "scout");
        this.scoutGame = game;
        this.currentAngle = Math.random() * 360;
        this.game = game;
        this.speed = 0.2;///////////////////////////////////////////////////////////////////////////
    }

    public void update(Point baseCenter, Resource[] resources) {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();
        managePointsReduction(currentTime);

        if (isExploding && currentTime - explosionStartTime > EXPLOSION_DURATION) {
            isExploding = false;
        }

        List<Worker> nearbyWorkers = scoutGame.getEnemyWorkersInRange(this, team, EXPLOSION_RADIUS);
        if (nearbyWorkers.size() >= 3 && !isExploding) {
            triggerExplosion(nearbyWorkers);
        }

        if (points <= MIN_POINTS && !returningToBase && !recharging) {
            returningToBase = true;
        }

        if (returningToBase) {
            if (scoutGame.allWorkersAtBase(scoutGame.getAllWorkers().toArray(new Worker[0]),
                    (int) baseCenter.getX(), (int) baseCenter.getY()) &&
                    scoutGame.allResourcesDepleted()) {

                moveToStartPosition(baseCenter);
                returningToBase = false;
                isActive = false;
            } else {
                moveToBase(baseCenter, currentTime, resources);
            }
        } else if (recharging) {
            handleRecharging(currentTime);
        } else if (!isExploding) {
            Worker targetWorker = scoutGame.findClosestEnemyWorkerWithinRange(this, team, 100);
            if (targetWorker != null) {
                handleTargetWorker(targetWorker, currentTime, resources);
            } else {
                patrolResources(resources);
            }
        }

        keepWithinBounds(scoutGame.getWidth(), scoutGame.getHeight());
    }

    public void moveToStartPosition(Point baseCenter) {
        this.x = baseCenter.x;
        this.y = baseCenter.y - 100;
        this.setCurrentAngle(90);
    }

    private void handleTargetWorker(Worker targetWorker, long currentTime, Resource[] resources) {
        double dx = targetWorker.getX() - this.x;
        double dy = targetWorker.getY() - this.y;
        this.currentAngle = Math.toDegrees(Math.atan2(dy, dx));

        double distanceToTarget = distanceTo(targetWorker);
        if (distanceToTarget > 50) {
            moveDirectlyToAvoidingResources((int) targetWorker.getX(), (int) targetWorker.getY(), resources);
        } else {
            handleShooting(targetWorker, currentTime);
        }
    }

    private void triggerExplosion(List<Worker> workers) {
        isExploding = true;
        explosionStartTime = System.currentTimeMillis();

        for (Worker worker : workers) {
            worker.setInactive();
            worker.setColor(Color.GRAY);
            incrementKills();
        }

        scoutGame.addExplosionEffect(this.x, this.y, EXPLOSION_RADIUS, Color.RED, EXPLOSION_DURATION);
    }

    public void moveBackFrom(int defenderX, int defenderY) {
        double angleAwayFromDefender = Math.atan2(this.y - defenderY, this.x - defenderX);

        this.x += BACK_STEP_DISTANCE * Math.cos(angleAwayFromDefender);
        this.y += BACK_STEP_DISTANCE * Math.sin(angleAwayFromDefender);
    }

    public double distanceTo(Worker worker) {
        double dx = this.x - worker.getX();
        double dy = this.y - worker.getY();
        return Math.hypot(dx, dy);
    }

    private void patrolResources(Resource[] resources) {
        if (resources.length == 0) {
            moveRandomly();
            return;
        }

        if (currentTargetResource == null || reachedResource(currentTargetResource)) {
            currentTargetResource = resources[resourceIndex];
            resourceIndex = (resourceIndex + 1) % resources.length;
        }

        moveDirectlyToAvoidingResources((int) currentTargetResource.getX(), (int) currentTargetResource.getY(), resources);
    }

    private boolean reachedResource(Resource resource) {
        return distance(this.x, this.y, resource.getX(), resource.getY()) < 10;
    }

    private void moveDirectlyToAvoidingResources(int targetX, int targetY, Resource[] resources) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);

        double scoutRadius = 5;
        double minimumDistance = 30;

        if (distance > 2) {
            double intendedAngle = Math.toDegrees(Math.atan2(dy, dx));
            double adjustedAngle = intendedAngle;
            boolean obstacleDetected = false;

            for (Resource resource : resources) {
                double distanceToResource = distance(resource.getX(), resource.getY(), x, y);
                double requiredAvoidanceDistance = resource.getRadius() + scoutRadius + minimumDistance;

                if (distanceToResource < requiredAvoidanceDistance) {
                    obstacleDetected = true;
                    double angleToResource = Math.toDegrees(Math.atan2(resource.getY() - y, resource.getX() - x));
                    adjustedAngle += (angleToResource > intendedAngle) ? -30 : 30;
                    break;
                }
            }

            if (!obstacleDetected) {
                adjustedAngle = intendedAngle;
            }

            this.currentAngle = adjustedAngle;
            x += Math.cos(Math.toRadians(currentAngle));
            y += Math.sin(Math.toRadians(currentAngle));
        }
    }

    private void moveToBase(Point baseCenter, long currentTime, Resource[] resources) {
        moveDirectlyToAvoidingResources((int) baseCenter.getX(), (int) baseCenter.getY(), resources);
        if (distance(baseCenter.getX(), baseCenter.getY(), x, y) < 5) {
            recharging = true;
            returningToBase = false;
            rechargeStartTime = currentTime;
            scoutGame.addPointsToScoutBase(team, points);
            points = MAX_POINTS;
        }
    }

    private void handleRecharging(long currentTime) {
        if (currentTime - rechargeStartTime >= RECHARGE_DURATION) {
            recharging = false;
        }
    }

    private void managePointsReduction(long currentTime) {
        if (currentTime - lastPointReductionTime >= POINT_REDUCTION_INTERVAL) {
            points = Math.max(points - 1, 0);
            lastPointReductionTime = currentTime;
        }
    }

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        kills++;
    }

    private void handleShooting(Worker targetWorker, long currentTime) {
        if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
            int startX = (int) this.x;
            int startY = (int) this.y;
            final int[] bulletX = {(int) (startX + Math.cos(Math.toRadians(this.currentAngle)) * this.getBodyRadius() * 2)};
            final int[] bulletY = {(int) (startY + Math.sin(Math.toRadians(this.currentAngle)) * this.getBodyRadius() * 2)};

            int targetX = (int) targetWorker.getX();
            int targetY = (int) targetWorker.getY();

            double angleToTarget = Math.atan2(targetY - bulletY[0], targetX - bulletX[0]);
            final double[] travelledDistance = {0};

            Timer timer = new Timer(50, e -> {
                int currentEndX = (int) (bulletX[0] + Math.cos(angleToTarget) * 10);
                int currentEndY = (int) (bulletY[0] + Math.sin(angleToTarget) * 10);

                scoutGame.drawShot(bulletX[0], bulletY[0], currentEndX, currentEndY);

                bulletX[0] += Math.cos(angleToTarget) * 5;
                bulletY[0] += Math.sin(angleToTarget) * 5;
                travelledDistance[0] += 5;

                if (distance(bulletX[0], bulletY[0], targetX, targetY) <= targetWorker.getBodyRadius()) {
                    targetWorker.takeDamage(1);

                    if (targetWorker.getHealth() <= 0) {
                        incrementKills();
                    }

                    ((Timer) e.getSource()).stop();
                }

                if (travelledDistance[0] >= MAX_BULLET_DISTANCE) {
                    ((Timer) e.getSource()).stop();
                }
            });

            timer.setRepeats(true);
            timer.start();

            lastShootTime = currentTime;
        }
    }

    public void drawKills(Graphics2D g2d, int xPosition, int yPosition) {  //TO DO
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(team.equals("blue") ? Color.RED : Color.BLUE);

        String scoreText = points + " - " + kills;
        g2d.drawString(scoreText, xPosition, yPosition);
    }

    private void keepWithinBounds(int panelWidth, int panelHeight) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > panelWidth - 10) x = panelWidth - 10;
        if (y > panelHeight - 10) y = panelHeight - 10;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public int getPoints() {
        return points;
    }

    public double getCurrentAngle() {
        return currentAngle;
    }

    public int getBodyRadius() {
        return 5;
    }

    public void reverseDirection() {
        currentAngle += 180;
        if (currentAngle >= 360) {
            currentAngle -= 360;
        }
    }

    public void decreasePoints(int amount) {
        points -= amount;
        if (points < 0) points = 0;
        System.out.println("Scout's points decreased to: " + points);
        reverseDirection();
        updatePosition();
    }

    public void updatePosition() {
        double deltaX = Math.cos(Math.toRadians(currentAngle));
        double deltaY = Math.sin(Math.toRadians(currentAngle));
        x += deltaX * speed;
        y += deltaY * speed;

        if (x < 0) {
            x = 0;
            reverseDirection();
        } else if (x > scoutGame.getWidth()) {
            x = scoutGame.getWidth();
            reverseDirection();
        }

        if (y < 0) {
            y = 0;
            reverseDirection();
        } else if (y > scoutGame.getHeight()) {
            y = scoutGame.getHeight();
            reverseDirection();
        }

        System.out.println("Scout's position updated: (" + x + ", " + y + ")");
    }

    private void moveRandomly() {
        double angleChange = (Math.random() - 0.5) * 20;
        currentAngle += angleChange;

        if (currentAngle < 0) currentAngle += 360;
        if (currentAngle >= 360) currentAngle -= 360;

        double speed = 1;
        x += speed * Math.cos(Math.toRadians(currentAngle));
        y += speed * Math.sin(Math.toRadians(currentAngle));
    }

    public void decreaseHealth(int amount) {
        health -= amount;
        if (health < 0) {
            health = 0;
        }
        System.out.println("Scout health: " + health);
    }

    public int getHealth() {
        return health;
    }

    public ScoutGame getGame() {
        return game;
    }

    @Override
    public String getType() {
        return "Scout";
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setCurrentAngle(double angle) {
        this.currentAngle = angle;
    }

}
