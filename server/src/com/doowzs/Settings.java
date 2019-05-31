package com.doowzs;

import java.awt.*;
import java.util.Random;

public class Settings {
    final static int port = 10086;

    final static int shootScore = -1;
    final static int hitScore = +500;

    final static int mapSize = 600;
    final static int gapSize = 10;
    final static int drawSize = 20;

    final static double playerSpeed = 0.6;
    final static double bulletSpeed = 1.0;
    final static double collideDistance = 10.0;

    private static Random random = new Random();

    static Color getRandomColor() {
        final float hue = random.nextFloat();
        final float saturation = (random.nextInt(2000) + 1000) / 10000f;
        final float luminance = 0.9f;
        return Color.getHSBColor(hue, saturation, luminance);
    }

    static int getRandomPosition() {
        return random.nextInt(500) + 50;
    }
}
