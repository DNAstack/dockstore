package com.dnastack.dockstore;

import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class MetadataE2eTest extends BaseE2eTest {
    @Test
    public void appNameAndVersionShouldBeExposed() {
        given()
            .log().method()
            .log().uri()
        .when()
            .get("/metadata/config.json")
        .then()
            .log().ifValidationFails()
            .statusCode(200);
    }
}