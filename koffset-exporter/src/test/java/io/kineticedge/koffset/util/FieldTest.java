package io.kineticedge.koffset.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FieldTest {

    static class NoArg {
        public NoArg(int value) {
        }
    }

    static class Data {

        private String foo;
        private String nonPublicGetter;
        private NoArg noArgConstructor;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        private String getNonPublicGetter() {
            return nonPublicGetter;
        }

        public void setNonPublicGetter(String nonPublicGetter) {
            this.nonPublicGetter = nonPublicGetter;
        }

        public void setBadSetter(String value) {
            throw new RuntimeException("set of BadSetter is not supported");
        }

        public String getBadSetter() {
            return null;
        }

        public void setBadGetter(String value) {
        }

        public String getBadGetter() {
            throw new RuntimeException("set of BadSetter is not supported");
        }

        public void setNoArgConstructor(NoArg noArgConstructor) {
            this.noArgConstructor = noArgConstructor;
        }

        public NoArg getNoArgConstructor() {
            return noArgConstructor;
        }
    }

    @Test
    void normalExecution() throws Exception {

        ConfigLoader.Field field = new ConfigLoader.Field("foo", String.class, Data.class.getDeclaredMethod("getFoo"), Data.class.getDeclaredMethod("setFoo", String.class), "x", "x.key");

        Data data = new Data();

        field.set(data, "foo");
        String value = field.get(data);

        Assertions.assertEquals("foo", value);
    }

    @Test
    void testNonPublicGetterThrowsException() throws Exception {
        ConfigLoader.Field field = new ConfigLoader.Field("nonPublicGetter", String.class, Data.class.getDeclaredMethod("getNonPublicGetter"), Data.class.getDeclaredMethod("setNonPublicGetter", String.class), "x", "x.key");

        Data data = new Data();

        field.set(data, null);
        Assertions.assertThrows(RuntimeException.class, () -> field.get(data));
    }


    @Test
    void testSetterThrowsException() throws Exception {
        ConfigLoader.Field field = new ConfigLoader.Field("badSetter", String.class, Data.class.getDeclaredMethod("getBadSetter"), Data.class.getDeclaredMethod("setBadSetter", String.class), "x", "x.key");

        Data data = new Data();

        Assertions.assertThrows(RuntimeException.class, () -> field.set(data, "FOO"));
    }

    @Test
    void testGetterThrowsException() throws Exception {
        ConfigLoader.Field field = new ConfigLoader.Field("badGetter", String.class, Data.class.getDeclaredMethod("getBadGetter"), Data.class.getDeclaredMethod("setBadGetter", String.class), "x", "x.key");

        Data data = new Data();

        Assertions.assertThrows(RuntimeException.class, () -> field.get(data));
        Assertions.assertThrows(RuntimeException.class, () -> field.getOrCreate(data));
    }

    @Test
    void testNoArgConstructorException() throws Exception {
        ConfigLoader.Field field = new ConfigLoader.Field("noArgConstructor", NoArg.class, Data.class.getDeclaredMethod("getNoArgConstructor"), Data.class.getDeclaredMethod("setNoArgConstructor", NoArg.class), "x", "x.key");

        Data data = new Data();

        Assertions.assertThrows(RuntimeException.class, () -> field.getOrCreate(data));
    }

    // private Setter

    // setter throws exception

    // getter throws exception

    // no public no-arg constructor
}
