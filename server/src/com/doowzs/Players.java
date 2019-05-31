package com.doowzs;

import java.awt.Graphics;
import java.awt.*;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class Players {
    private static Logger logger = Logger.getLogger(Players.class.getName());
    public static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();

    public synchronized static String[] getPlayersList() {
        int sz = Players.players.size();
        String[] list = new String[sz];
        for (int i = 0; i < sz; ++i) {
            Player player = Players.players.get(i);
            list[i] = player.name + " (" + player.score + ")";
        }
        return list;
    }

    public synchronized static Player addPlayer(String name) {
        Player player = new Player(name);
        players.add(player);
        logger.info("Player " + name + " registered."
                + " UUID is " + player.uuid.toString() + ".");
        return player;
    }

    public synchronized static Player getPlayerByUUID(UUID uuid) {
        for (Player player : players) {
            if (player.uuid.equals(uuid)) return player;
        }
        return null;
    }

    public synchronized static void removePlayer(Player player) {
        logger.info("Player " + player.name + " leaved.");
        players.remove(player);
    }
}

class Player {
    public UUID uuid;
    public int score;
    public String name;
    public Color color;
    public OnMapStatus status;

    public String toString() {
        return this.name;
    }

    public Player(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.color = Settings.getRandomColor();
        this.status = new OnMapStatus();
    }

    public void move(String spd, String sgn, String ang) {
        double speed = 0.0;
        switch (spd) {
            case "ACC":
                speed = + Settings.playerSpeed;
                break;
            case "BAK":
                speed = - Settings.playerSpeed / 2;
                break;
            default:
                speed = 0.0;
        }
        double sign = sgn.equals("POS") ? -1.2 : +1.2;
        int intAng = Integer.valueOf(ang) / 10;
        double angle = sign * intAng;
        this.status.update(speed, angle);
    }

    public void shoot() {
        this.score += Settings.shootScore;
        Bullet bullet = new Bullet(this);
        Players.bullets.add(bullet);
    }

    public void draw(Graphics g) {
        int sz = Settings.drawSize;
        double a = this.status.angle;
        double x = this.status.x + Settings.gapSize;
        double y = this.status.y + Settings.gapSize;

        final double b = 7.5;
        final double c = 0.6;
        int px[] = new int[]{
                (int) (x + sz * c * Math.cos(Math.toRadians(a - b))),
                (int) (x + sz * c * Math.sin(Math.toRadians(b)) * Math.cos(Math.toRadians(a - 90))),
                (int) (x + sz * c * Math.sin(Math.toRadians(b)) * Math.cos(Math.toRadians(a + 90))),
                (int) (x + sz * c * Math.cos(Math.toRadians(a + b))),
        };
        int py[] = new int[]{
                (int) (y + sz * c * Math.sin(Math.toRadians(a - b))),
                (int) (y + sz * c * Math.sin(Math.toRadians(b)) * Math.sin(Math.toRadians(a - 90))),
                (int) (y + sz * c * Math.sin(Math.toRadians(b)) * Math.sin(Math.toRadians(a + 90))),
                (int) (y + sz * c * Math.sin(Math.toRadians(a + b))),
        };

        g.setColor(this.color);
        g.fillOval((int) (x - sz / 2), (int) (y - sz / 2), sz, sz);
        g.drawString(this.name, (int) x, (int) (y + sz));
        g.setColor(Color.black);
        g.fillPolygon(px, py, 4);
    }
}

class Bullet {
    public Player player;
    public OnMapStatus status;

    public Bullet(Player player) {
        this.player = player;
        this.status = new OnMapStatus(player.status);
    }

    public boolean move() {
        this.status.update(Settings.bulletSpeed, 0.0);
        for (Player player : Players.players) {
            if (player.equals(this.player)) continue;
            if (this.status.dis(player.status) < Settings.collideDistance) {
                this.player.score += Settings.hitScore;
                player.status = new OnMapStatus();
                return true;
            }
        }
        return this.status.onBorder();
    }

    public void draw(Graphics g) {
        g.setColor(this.player.color);
        g.fillOval((int) this.status.x + Settings.gapSize - Settings.gapSize / 6,
                (int) this.status.y + Settings.gapSize - Settings.gapSize / 6,
                Settings.drawSize / 3, Settings.drawSize / 3);
    }
}

class OnMapStatus implements Cloneable {
    public double x;
    public double y;
    public double angle;

    public OnMapStatus() {
        this.x = Settings.getRandomPosition();
        this.y = Settings.getRandomPosition();
        this.angle = new Random().nextInt(360);
    }

    public OnMapStatus(OnMapStatus other) {
        this.x = other.x;
        this.y = other.y;
        this.angle = other.angle;
    }

    public boolean onBorder() {
        return this.x <= 0 || this.x >= Settings.mapSize
                || this.y <= 0 || this.y >= Settings.mapSize;
    }

    public double dis(OnMapStatus b) {
        return Math.sqrt(Math.pow(this.x - b.x, 2) + Math.pow(this.y - b.y, 2));
    }

    public void update(double speed, double dangle) {
        this.angle += dangle;
        if (this.angle < 0) this.angle += 360;
        if (this.angle > 360) this.angle -= 360;

        this.x += speed * Math.cos(Math.toRadians(angle));
        if (this.x < 0) this.x = 0;
        if (this.x > Settings.mapSize) this.x = Settings.mapSize;

        this.y += speed * Math.sin(Math.toRadians(angle));
        if (this.y < 0) this.y = 0;
        if (this.y > Settings.mapSize) this.y = Settings.mapSize;
    }
}