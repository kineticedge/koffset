package io.kineticedge.koffset.util;

import io.kineticedge.koffset.config.KoffsetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {

    private static final Logger log = LoggerFactory.getLogger(VersionInfo.class);

    public static void banner(KoffsetConfig config) {
        try (InputStream is = VersionInfo.class.getResourceAsStream("/banner.txt")) {
            if (is != null) {

                String banner = new String(is.readAllBytes());

                banner = banner.replace("{{PORT}}", String.valueOf(config.getServer().getPort()));

                log.info("\n{}\n", banner);
            }
        } catch (Exception e) {
            //ignore
        }
    }
    public static void log() {
        try (InputStream is = VersionInfo.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                log.info("koffset version: ref={}, sha={}, build={}]\n",
                        props.getProperty("build.ref"),
                        props.getProperty("build.sha"),
                        props.getProperty("build.time")
                );
            } else {
                log.warn("Version metadata not found.");
            }
        } catch (Exception e) {
            log.debug("Could not load version info", e);
        }
    }
}