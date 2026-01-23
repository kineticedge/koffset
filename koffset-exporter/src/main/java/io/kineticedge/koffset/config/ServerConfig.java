package io.kineticedge.koffset.config;

public class ServerConfig {

    private int port = 8080;

    //

    public ServerConfig() {
    }

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

    //

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                '}';
    }
}