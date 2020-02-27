package com.dnastack.wallet;

import io.restassured.RestAssured;
import org.junit.Before;

import java.util.function.Supplier;

import static org.junit.Assert.fail;

public class BaseE2eTest {

    @Before
    public void setUp() throws Exception {
        RestAssured.baseURI = requiredEnv("E2E_BASE_URI");
    }

    static String requiredEnv(String name) {
        String val = System.getenv(name);
        if (val == null) {
            fail("Environnment variable `" + name + "` is required");
        }
        return val;
    }

    static String requiredEnv(String name, Supplier<String> errorMsg) {
        String val = System.getenv(name);
        if (val == null) {
            fail(errorMsg.get());
        }
        return val;
    }

    static String optionalEnv(String name, String defaultValue) {
        String val = System.getenv(name);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }
}
