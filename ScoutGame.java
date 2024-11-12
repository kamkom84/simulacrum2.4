package classesSeparated;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static java.awt.geom.Point2D.distance;

public class ScoutGame extends JFrame {
    private int blueBaseX, blueBaseY, redBaseX, redBaseY;
    private int blueBaseHealth = 0;
    private int redBaseHealth = 0;
    private final int baseWidth = 75;
    private final int baseHeight = 75;
    private Worker[] blueWorkers;
    private Worker[] redWorkers;
    private Resource[] resources;
    private Defender[] blueDefenders;
    private Defender[] redDefenders;
    private Scout blueScout;
    private Scout redScout;
    private List<Worker> allWorkers;
    private long startTime;
    private final int DEFENDER_SHIELD_RADIUS = (int) (baseWidth * 1.5);
    private boolean gameOver = false;
    private String winner = "";
    private int[] resourceValues;
    private boolean[] resourceOccupied;
    private int bulletStartX = -1;
    private int bulletStartY = -1;
    private int bulletEndX = -1;
    private int bulletEndY = -1;
    private List<ExplosionEffect> explosionEffects = new ArrayList<>();

    public ScoutGame() {
        allWorkers = new ArrayList<>();

        setTitle("simulacrum drone wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gd.setFullScreenWindow(this);

        setVisible(true);

        int screenWidth = getWidth();
        int screenHeight = getHeight();
        blueBaseX = 100;
        blueBaseY = screenHeight / 2 - baseHeight / 2;
        redBaseX = screenWidth - 100 - baseWidth;
        redBaseY = screenHeight / 2 - baseHeight / 2;

        int bodyRadius = 5;

        blueScout = new Scout(blueBaseX, blueBaseY, "blue", this);
        blueScout.activate();

        redScout = new Scout(redBaseX + baseWidth - 2 * bodyRadius, redBaseY, "red", this);
        redScout.activate();

        initializeResources();
        initializeWorkers();
        generateResources();

        blueDefenders = new Defender[3];
        redDefenders = new Defender[3];
        initializeDefenders();

        scheduleWorkerStarts();
        startTime = System.currentTimeMillis();

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int shieldRadius = (int) (baseWidth * 2.9);

                drawBasesAndResources(g2d, shieldRadius);
                drawWorkers(g2d);
                drawExplosions(g2d);

                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
                int hours = (int) (elapsedTime / (1000 * 60 * 60));

                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                g2d.setColor(Color.WHITE);
                g2d.drawString(timeText, 10, 30);

                FontMetrics fm = g2d.getFontMetrics();
                int xPosition = 10 + fm.stringWidth(timeText) + 50;

                g2d.setColor(Color.BLUE);
                g2d.drawString("Base: " + blueBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("Base: " + blueBaseHealth) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("Base: " + redBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("Base: " + redBaseHealth) + 50;

                g2d.setColor(Color.BLUE);
                g2d.drawString("Scout: " + blueScout.getPoints() + "-" + blueScout.getKills(), xPosition, 30);
                xPosition += fm.stringWidth("Scout: " + blueScout.getPoints() + "-" + blueScout.getKills()) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("Scout: " + redScout.getPoints() + "-" + redScout.getKills(), xPosition, 30);

                for (Worker worker : blueWorkers) {
                    if (worker != null && worker.isActive()) {
                        worker.draw(g2d);
                    }
                }

                for (Worker worker : redWorkers) {
                    if (worker != null && worker.isActive()) {
                        worker.draw(g2d);
                    }
                }

                for (Defender defender : blueDefenders) {
                    if (defender != null) {
                        defender.drawProjectiles(g2d);
                        defender.drawDirectionLine(g2d);
                    }
                }
                for (Defender defender : redDefenders) {
                    if (defender != null) {
                        defender.drawProjectiles(g2d);
                        defender.drawDirectionLine(g2d);
                    }
                }

                if (bulletStartX != -1 && bulletStartY != -1) {
                    g2d.setColor(Color.GREEN);
                    g2d.drawLine(bulletStartX, bulletStartY, bulletEndX, bulletEndY);
                }

                if (ScoutGame.this.gameOver) {
                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                    g2d.setColor(Color.YELLOW);
                    String winnerText = ScoutGame.this.winner;
                    int winnerX = (getWidth() - fm.stringWidth(winnerText)) / 2;
                    int winnerY = getHeight() / 2;
                    g2d.drawString(winnerText, winnerX, winnerY);
                }
            }

            private void drawExplosions(Graphics2D g2d) {
                long currentTime = System.currentTimeMillis();
                explosionEffects.removeIf(effect -> effect.isExpired(currentTime));
                for (ExplosionEffect effect : explosionEffects) {
                    effect.draw(g2d);
                }
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(Color.DARK_GRAY);

        JButton minimizeButton = new JButton("-");
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));
        controlPanel.add(minimizeButton);

        JButton fullscreenButton = new JButton("□");
        fullscreenButton.addActionListener(e -> {
            setUndecorated(!isUndecorated());
            setVisible(true);
            GraphicsDevice gdDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gdDevice.setFullScreenWindow(isUndecorated() ? this : null);
        });
        controlPanel.add(fullscreenButton);

        JButton closeButton = new JButton("X");
        closeButton.addActionListener(e -> System.exit(0));
        controlPanel.add(closeButton);

        add(controlPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        Timer timer = new Timer(150, e -> {
            if (blueScout.isActive()) {
                Point blueBaseCenter = new Point(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2);
                blueScout.update(blueBaseCenter, resources);
                blueScout.updatePosition();
            }
            if (redScout.isActive()) {
                Point redBaseCenter = new Point(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2);
                redScout.update(redBaseCenter, resources);
                redScout.updatePosition();
            }

            moveDefenders();
            moveWorkers();
            mainPanel.repaint();
        });
        timer.start();

        setVisible(true);
    }

    private void initializeResources() {
        resources = new Resource[350];//////////////////////////////////////////////////////////////////////////////////
        resourceValues = new int[resources.length];
        resourceOccupied = new boolean[resources.length];

        for (int i = 0; i < resources.length; i++) {
            resources[i] = new Resource(0, 0, 5000);
            resourceValues[i] = 10;
            resourceOccupied[i] = false;
        }
    }

    private void generateResources() {
        Random random = new Random();
        int panelWidth = Math.max(getContentPane().getWidth(), 800);
        int panelHeight = Math.max(getContentPane().getHeight(), 600);

        List<Point2D.Double> workerPositions = new ArrayList<>();
        for (Worker worker : blueWorkers) {
            workerPositions.add(new Point2D.Double(worker.getX(), worker.getY()));
        }
        for (Worker worker : redWorkers) {
            workerPositions.add(new Point2D.Double(worker.getX(), worker.getY()));
        }

        for (int i = 0; i < resources.length; i++) {
            int x, y;
            boolean positionIsValid;
            do {
                x = (int) (Math.random() * (panelWidth - 2 * baseWidth)) + baseWidth;
                y = (int) (Math.random() * (panelHeight - 2 * baseHeight)) + baseHeight;
                positionIsValid = !isNearBase(x, y) && !isNearWorkers(x, y, workerPositions);
            } while (!positionIsValid);

            resources[i] = new Resource(x, y, 100000);////////////////////////////////////////////////////////////////
        }
    }

    private boolean isNearWorkers(double x, double y, List<Point2D.Double> workerPositions) {
        int minDistance = 50;
        for (Point2D.Double workerPos : workerPositions) {
            if (distance(x, y, workerPos.x, workerPos.y) < minDistance) {
                return true;
            }
        }
        return false;
    }

    private void initializeWorkers() {
        int totalWorkers = 170;////////////////////////////////////////////////////////////////////////////////////////
        int workersPerColumn = 10;

        blueWorkers = new Worker[totalWorkers];
        redWorkers = new Worker[totalWorkers];

        int columnSpacing = 25;
        int rowSpacing = 25;

        for (int i = 0; i < totalWorkers; i++) {
            int columnIndex = i / workersPerColumn;
            int rowIndex = i % workersPerColumn;

            blueWorkers[i] = new Worker(
                    blueBaseX + baseWidth / 2 + columnIndex * columnSpacing,
                    blueBaseY + baseHeight + 100 + rowIndex * rowSpacing,
                    "blue",
                    resources,
                    resourceValues,
                    resourceOccupied,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );

            redWorkers[i] = new Worker(
                    redBaseX + baseWidth / 2 - columnIndex * columnSpacing,
                    redBaseY + baseHeight + 100 + rowIndex * rowSpacing,
                    "red",
                    resources,
                    resourceValues,
                    resourceOccupied,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );

            redWorkers[i].setAngle(180);
            allWorkers.add(blueWorkers[i]);
            allWorkers.add(redWorkers[i]);
        }
    }

    private boolean isNearBase(int x, int y) {
        int blueBaseCenterX = blueBaseX + baseWidth / 2;
        int blueBaseCenterY = blueBaseY + baseHeight / 2;
        int redBaseCenterX = redBaseX + baseWidth / 2;
        int redBaseCenterY = redBaseY + baseHeight / 2;
        int minDistance = 200;

        return distance(x, y, blueBaseCenterX, blueBaseCenterY) < minDistance ||
                distance(x, y, redBaseCenterX, redBaseCenterY) < minDistance;
    }

    private void initializeDefenders() {
        for (int i = 0; i < 3; i++) {
            blueDefenders[i] = new Defender(
                    blueBaseX + baseWidth / 2,
                    blueBaseY + baseHeight / 2,
                    "blue",
                    "defender",
                    i * Math.PI / 4,
                    this
            );
            redDefenders[i] = new Defender(
                    redBaseX + baseWidth / 2,
                    redBaseY + baseHeight / 2,
                    "red",
                    "defender",
                    Math.PI + i * Math.PI / 4,
                    this
            );
        }
    }

    private void moveDefenders() {
        for (Defender defender : blueDefenders) {
            if (defender != null) {
                defender.patrolAroundBase(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);
                defender.checkAndShootIfScoutInRange(redScout);
                defender.updateProjectiles(redScout); // Обновяване на патроните
            }
        }
        for (Defender defender : redDefenders) {
            if (defender != null) {
                defender.patrolAroundBase(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);
                defender.checkAndShootIfScoutInRange(blueScout);
                defender.updateProjectiles(blueScout); // Обновяване на патроните
            }
        }
    }

    private void drawBasesAndResources(Graphics2D g2d, int shieldRadius) {
        g2d.setColor(new Color(0, 100, 200));
        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.BLUE);
        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);

        g2d.setColor(new Color(0, 0, 255, 100));
        g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        g2d.setColor(new Color(200, 50, 50));
        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);

        g2d.setColor(new Color(255, 0, 0, 100));
        g2d.drawOval(redBaseX - (shieldRadius - baseWidth) / 2, redBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        for (Resource resource : resources) {
            g2d.setColor(resource.getValue() <= 0 ? new Color(169, 169, 169) : new Color(255, 223, 0));
            g2d.fillOval((int) resource.getX() - 20, (int) resource.getY() - 20, 40, 40);
            g2d.setColor(Color.BLACK);
            g2d.drawOval((int) resource.getX() - 20, (int) resource.getY() - 20, 40, 40);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(String.valueOf(resource.getValue()), (int) resource.getX() - 10, (int) resource.getY() + 5);
        }
    }

    private void drawWorkers(Graphics2D g2d) {
        drawWorkersWithLine(g2d, blueScout);
        drawWorkersWithLine(g2d, redScout);

        for (Worker worker : blueWorkers) {
            drawWorkersWithLine(g2d, worker);
        }
        for (Worker worker : redWorkers) {
            drawWorkersWithLine(g2d, worker);
        }
        for (Defender defender : blueDefenders) {
            drawWorkersWithLine(g2d, defender);
        }
        for (Defender defender : redDefenders) {
            drawWorkersWithLine(g2d, defender);
        }
    }

    private void drawWorkersWithLine(Graphics2D g2d, Character ant) {
        if (ant == null) return;

        int bodyRadius = 5;
        int lineLength;

        // Проверка за типа на обекта и задаване на цвят
        if (ant instanceof Scout) {
            lineLength = bodyRadius * 2;
            g2d.setColor(ant.team.equals("blue") ? Color.BLUE : Color.RED); // Скаутите са сини или червени
        } else if (ant instanceof Worker) {
            lineLength = bodyRadius;

            // Задаваме цвета на работника според отбора
            if (ant.team.equals("blue")) {
                g2d.setColor(Color.BLUE); // Син за работниците на "blue" отбора
            } else {
                g2d.setColor(Color.RED); // Червен за работниците на "red" отбора
            }

            // Рисуваме тялото на работника със зададения цвят
            g2d.fillOval((int) (ant.getX() - bodyRadius), (int) (ant.getY() - bodyRadius), bodyRadius * 2, bodyRadius * 2);

            // Възстановяваме белия цвят само за номера, след което пак задаваме цвета за тялото
            Worker worker = (Worker) ant;
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.setColor(Color.WHITE); // Бял цвят за номера
            g2d.drawString(String.valueOf(worker.getId()), (int) worker.getX() - 5, (int) worker.getY() - 10);

            // Възстановяваме оригиналния цвят за тялото на работника
            if (ant.team.equals("blue")) {
                g2d.setColor(Color.BLUE);
            } else {
                g2d.setColor(Color.RED);
            }
        } else if (ant instanceof Defender) {
            bodyRadius *= 1.5;
            lineLength = bodyRadius;
            g2d.setColor(ant.team.equals("blue") ? new Color(0, 0, 180) : new Color(180, 0, 0)); // По-тъмен син за "blue" защитници, по-тъмен червен за "red"
        } else {
            return;
        }

        // Рисуване на тялото на персонажа (работник, защитник или скаут)
        g2d.fillOval((int) (ant.getX() - bodyRadius), (int) (ant.getY() - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        // Определяне на ъгъл и рисуване на посоката (жълта чертичка за работници)
        double angle = ant.getCurrentAngle();
        if (ant instanceof Defender && ant.team.equals("red") && angle == 0) {
            angle = 180;
        }

        int x1 = (int) ant.getX();
        int y1 = (int) ant.getY();
        int x2 = x1 + (int) (lineLength * Math.cos(Math.toRadians(angle)));
        int y2 = y1 + (int) (lineLength * Math.sin(Math.toRadians(angle)));

        // Избиране на цвят за чертичката
        if (ant instanceof Scout) {
            g2d.setColor(Color.GREEN); // Зелен за скаута
        } else if (ant instanceof Worker || ant instanceof Defender) {
            g2d.setColor(Color.YELLOW); // Жълт за работници и защитници
        }

        g2d.drawLine(x1, y1, x2, y2); // Рисуване на чертичката
    }

    private void scheduleWorkerStarts() {
        int initialDelay = 30000;
        int interval = 30000;

        for (int i = 0; i < blueWorkers.length; i++) {
            int delay = initialDelay + (i * interval);
            final int workerIndex = i;

            Timer blueWorkerTimer = new Timer(delay, e -> {
                blueWorkers[workerIndex].activate();
                ((Timer) e.getSource()).stop();
            });
            blueWorkerTimer.setRepeats(false);
            blueWorkerTimer.start();

            Timer redWorkerTimer = new Timer(delay, e -> {
                redWorkers[workerIndex].activate();
                ((Timer) e.getSource()).stop();
            });
            redWorkerTimer.setRepeats(false);
            redWorkerTimer.start();
        }
    }

    private void moveWorkers() {
        if (gameOver) return;

        boolean anyActiveWorkers = false;

        for (Worker worker : blueWorkers) {
            if (worker != null) {
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, redScout);
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        for (Worker worker : redWorkers) {
            if (worker != null) {
                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout);
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        if (!anyActiveWorkers && allWorkersStarted() && allResourcesDepleted() && !gameOver) {
            gameOver = true;
            determineWinner();
        }
    }

    public boolean allResourcesDepleted() {
        for (Resource resource : resources) {
            if (resource.getValue() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean allWorkersStarted() {
        for (Worker worker : blueWorkers) {
            if (worker != null && !worker.hasStarted()) {
                return false;
            }
        }
        for (Worker worker : redWorkers) {
            if (worker != null && !worker.hasStarted()) {
                return false;
            }
        }
        return true;
    }

    private void determineWinner() {
        int blueWorkersAlive = countAliveWorkers(blueWorkers);
        int redWorkersAlive = countAliveWorkers(redWorkers);

        if (blueWorkersAlive > redWorkersAlive) {
            winner = "Blue team wins!";
        } else if (redWorkersAlive > blueWorkersAlive) {
            winner = "Red team wins!";
        } else {
            winner = "No Winner!";
        }

        gameOver = true;
        System.out.println("Game Over. " + winner);
    }

    private int countAliveWorkers(Worker[] workers) {
        int count = 0;
        for (Worker worker : workers) {
            if (worker != null && worker.isActive()) {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScoutGame::new);
    }

    public List<Worker> getAllWorkers() {
        return allWorkers;
    }

    public int getBlueBaseHealth() {
        return blueBaseHealth;
    }

    public void setBlueBaseHealth(int health) {
        this.blueBaseHealth = health;
    }

    public int getRedBaseHealth() {
        return redBaseHealth;
    }

    public void setRedBaseHealth(int health) {
        this.redBaseHealth = health;
    }

    public void addPointsToScoutBase(String team, int points) {
        if (team.equals("blue")) {
            blueBaseHealth += points;
        } else if (team.equals("red")) {
            redBaseHealth += points;
        }
    }

    public Worker findClosestEnemyWorkerWithinRange(Scout scout, String scoutTeam, double maxRange) {
        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
        Worker closestWorker = null;
        double closestDistance = maxRange;

        for (Worker worker : enemyWorkers) {
            if (!worker.isActive()) {
                continue;
            }

            double distance = scout.distanceTo(worker);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWorker = worker;
            }
        }

        return closestWorker;
    }

    public List<Worker> getEnemyWorkersInRange(Scout scout, String scoutTeam, double range) {
        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
        List<Worker> nearbyWorkers = new ArrayList<>();

        for (Worker worker : enemyWorkers) {
            if (worker.isActive() && scout.distanceTo(worker) <= range) {
                nearbyWorkers.add(worker);
            }
        }
        return nearbyWorkers;
    }

    public void addExplosionEffect(double x, double y, int radius, Color color, int duration) {
        explosionEffects.add(new ExplosionEffect(x, y, radius, color, duration));
    }

    public void drawShot(int startX, int startY, int endX, int endY) {
        this.bulletStartX = startX;
        this.bulletStartY = startY;
        this.bulletEndX = endX;
        this.bulletEndY = endY;
        repaint();

        Timer timer = new Timer(50, e -> {
            bulletStartX = bulletStartY = bulletEndX = bulletEndY = -1;
            repaint();
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

}

class ExplosionEffect {
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
