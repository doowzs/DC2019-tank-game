package com.doowzs;

public class Main {
    protected static Graphics graphics;
    protected static Server server;

    public static void main(String[] args) {
        graphics = new Graphics();
        server = new Server();
        new Thread(server).run();
    }
}
