package com.doowzs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Server implements Runnable {
    private Logger logger = Logger.getLogger(Server.class.getName());
    protected ServerSocket server;

    public Server() {
        try {
            this.server = new ServerSocket(Settings.port);
            this.logger.info("Server started at port " + Settings.port + ".");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (true) {
                Socket socket = this.server.accept();
                this.logger.info("New client connected at " + socket.getInetAddress().getHostAddress() + ".");
                Client client = new Client(socket);
                if (client.player != null) {
                    new Thread(client).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Client implements Runnable {
    protected Player player;
    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;

    public Client(Socket socket) {
        try {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);

            String[] s = this.in.readLine().split("<");
            if (s.length == 2 && s[0].equals("CON")) {
                this.player = Players.addPlayer(s[1]);
                respond(this.player.uuid.toString());
            } else {
                this.player = null;
                respond("Invalid operation.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            boolean alive = true;
            long timeMillis = 0;
            while (alive) {
                String[] s = this.in.readLine().split("<");
                if (System.currentTimeMillis() < timeMillis + 20) continue;
                timeMillis = System.currentTimeMillis();

                switch (s[0]) {
                    case "ACT":
                        if (s.length == 4) {
                            if ("SHT".equals(s[1])) {
                                this.player.shoot();
                                this.player.move(s[1], s[2], s[3]);
                                respond("SHT");
                            } else {
                                this.player.move(s[1], s[2], s[3]);
                                respond("MOV");
                            }
                        } else {
                            respond("INV");
                        }
                        break;
                    case "BYE":
                        respond("BYE");
                        alive = false;
                        break;
                    default:
                        respond("INV");
                }
            }
        } catch (Exception e) {
            if (!"Connection reset".equals(e.getMessage())) {
                e.printStackTrace();
            }
        }

        for (Bullet bullet : Players.bullets) {
            if (bullet.player.equals(this.player)) {
                Players.bullets.remove(bullet);
            }
        }
        Players.removePlayer(this.player);
    }

    public void respond(String s) {
        StringBuilder sb = new StringBuilder("RES");
        sb.append("<").append(s);
        sb.append("<").append(((int) this.player.status.x));
        sb.append("<").append(((int) this.player.status.y));
        this.out.println(sb.toString());
    }
}