package io.kineticedge.koffset.util;

import io.kineticedge.koffset.config.KoffsetConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionInfoTest {


    // just sanity check to make sure these do not throw errors on a typical uses
    @Test
    void pointless() {
        Assertions.assertDoesNotThrow(VersionInfo::log);
        Assertions.assertDoesNotThrow(() -> VersionInfo.banner(new KoffsetConfig()));
    }
}