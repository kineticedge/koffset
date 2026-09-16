package io.kineticedge.koffset;

import io.kineticedge.koffset.config.KoffsetConfig;
import io.kineticedge.koffset.util.ConfigLoader;
import io.kineticedge.koffset.util.EnvConfigLoader;
import io.kineticedge.koffset.util.VersionInfo;
import org.apache.kafka.clients.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) {

        final ConfigLoader loader = new EnvConfigLoader();


        KoffsetConfig koffsetConfig = new KoffsetConfig();

        loader.populate(koffsetConfig, "KOFFSET");

        VersionInfo.banner(koffsetConfig);
        VersionInfo.log();

        final Admin admin = Admin.create(koffsetConfig.getKafka());
        final LagAnalyzer lagAnalyzer = new LagAnalyzer(koffsetConfig.getCollector(), admin);
        final AutoAdjuster autoAdjuster = new AutoAdjuster(koffsetConfig.getAutoAdjust(), lagAnalyzer);

        lagAnalyzer.start();

        final Server server = new Server(koffsetConfig.getServer(), lagAnalyzer, autoAdjuster);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
            lagAnalyzer.stop();
            admin.close();
        }));

    }

}
