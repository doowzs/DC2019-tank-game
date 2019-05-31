package com.doowzs;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.logging.Logger;

public class Graphics {
    private Logger logger = Logger.getLogger(Graphics.class.getName());
    protected JFrame frame;
    protected MapPanel mapPanel;
    protected InfoPanel infoPanel;

    public Graphics() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Tank Game Server");
        frame.setSize(Settings.mapSize + 2 * Settings.gapSize + 200,
                Settings.mapSize + 6 * Settings.gapSize);

        mapPanel = new MapPanel();
        infoPanel = new InfoPanel();
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                false, mapPanel, infoPanel);
        jSplitPane.setDividerLocation(Settings.mapSize + 2 * Settings.gapSize);
        frame.setContentPane(jSplitPane);

        frame.setVisible(true);
        logger.info("Graphics initialized.");

        Thread mapThread = new Thread(mapPanel);
        Thread infoThread = new Thread(infoPanel);
        mapThread.start();
        infoThread.start();
    }
}

class MapPanel extends JPanel implements Runnable {
    public MapPanel() {
        this.setPreferredSize(new Dimension(Settings.mapSize + 2 * Settings.gapSize,
                Settings.mapSize + 2 * Settings.gapSize));
        this.setBackground(Color.white);
    }

    public void run() {
        java.awt.Graphics g = this.getGraphics();
        Image buffer = createImage(this.getWidth(), this.getHeight());
        java.awt.Graphics g2 = buffer.getGraphics();
        while (true) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            update(g2);
            g2.setColor(Color.BLACK);
            g2.drawRect(Settings.gapSize, Settings.gapSize, Settings.mapSize, Settings.mapSize);
            drawBullets(g2);
            drawPlayers(g2);
            g.drawImage(buffer, 0, 0, null);
        }
    }

    private synchronized void drawPlayers(java.awt.Graphics g) {
        for (Player player : Players.players) {
            player.draw(g);
        }
    }

    private synchronized void drawBullets(java.awt.Graphics g) {
        for (Bullet bullet : Players.bullets) {
            if (bullet.move()) {
                Players.bullets.remove(bullet);
            } else {
                bullet.draw(g);
            }
        }
    }
}

class InfoPanel extends JPanel implements Runnable {
    public JList<String> jList;

    public InfoPanel() {
        this.setBackground(Color.white);
        this.setLayout(new BorderLayout());
        this.jList = new JList<String>();
        this.add(new JLabel("Players"), "North");
        this.add(jList, "Center");
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(25);
                this.jList.setListData(Players.getPlayersList());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
