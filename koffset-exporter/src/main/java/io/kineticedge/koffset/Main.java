package io.kineticedge.koffset;

import io.kineticedge.koffset.config.KoffsetConfig;
import io.kineticedge.koffset.util.KafkaEnv;
import org.apache.kafka.clients.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) {

        final KoffsetConfig koffsetConfig = new KoffsetConfig();

        KafkaEnv env = new KafkaEnv();

        Map<String, Object> config = env.to("KAFKA_");

        final Admin admin = Admin.create(config);
        final LagAnalyzer lagAnalyzer = new LagAnalyzer(koffsetConfig.getCollector(), admin);
        final AutoAdjuster autoAdjuster = new AutoAdjuster(koffsetConfig.getAutoAdjust(), lagAnalyzer);

        lagAnalyzer.start();

        final Server server = new Server(koffsetConfig.getServer(), lagAnalyzer, autoAdjuster);
        server.start();

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> 1111");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
            lagAnalyzer.stop();
            admin.close();
        }));

    }

}
