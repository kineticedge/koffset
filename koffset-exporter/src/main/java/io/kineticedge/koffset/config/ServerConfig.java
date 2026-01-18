package io.kineticedge.koffset.config;

import io.kineticedge.koffset.Server;

public class ServerConfig {

    //TODO 8080 ?
    private int port = 8080;

    //

    /* config loader */
    public ServerConfig() {
    }

    /* property loader */
    public ServerConfig(int port) {
        this.port = port;
    }

    //

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                '}';
    }
}