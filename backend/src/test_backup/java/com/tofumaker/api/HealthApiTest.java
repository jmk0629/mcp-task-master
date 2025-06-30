package com.tofumaker.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class HealthApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void healthCheck_ShouldReturnSystemStatus() {
        given()
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", notNullValue())
                .body("timestamp", notNullValue())
                .body("application", equalTo("TofuMaker Backend"))
                .body("version", equalTo("1.0.0"))
                .body("components", notNullValue())
                .body("components.database", notNullValue())
                .body("components.redis", notNullValue());
    }

    @Test
    void readinessCheck_ShouldReturnReadinessStatus() {
        given()
                .when()
                .get("/api/health/ready")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", notNullValue())
                .body("timestamp", notNullValue())
                .body("checks", notNullValue())
                .body("checks.database", notNullValue())
                .body("checks.redis", notNullValue());
    }

    @Test
    void livenessCheck_ShouldReturnLivenessStatus() {
        given()
                .when()
                .get("/api/health/live")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("ALIVE"))
                .body("timestamp", notNullValue())
                .body("uptime", notNullValue());
    }

    @Test
    void healthEndpoints_ShouldHaveCorrectResponseStructure() {
        // Test main health endpoint structure
        given()
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("$", hasKey("status"))
                .body("$", hasKey("timestamp"))
                .body("$", hasKey("application"))
                .body("$", hasKey("version"))
                .body("$", hasKey("components"));

        // Test readiness endpoint structure
        given()
                .when()
                .get("/api/health/ready")
                .then()
                .statusCode(200)
                .body("$", hasKey("status"))
                .body("$", hasKey("timestamp"))
                .body("$", hasKey("checks"));

        // Test liveness endpoint structure
        given()
                .when()
                .get("/api/health/live")
                .then()
                .statusCode(200)
                .body("$", hasKey("status"))
                .body("$", hasKey("timestamp"))
                .body("$", hasKey("uptime"));
    }

    @Test
    void healthEndpoints_ShouldReturnValidTimestamps() {
        given()
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .body("timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));

        given()
                .when()
                .get("/api/health/ready")
                .then()
                .statusCode(200)
                .body("timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));

        given()
                .when()
                .get("/api/health/live")
                .then()
                .statusCode(200)
                .body("timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }

    @Test
    void healthEndpoints_ShouldReturnConsistentContentType() {
        given()
                .when()
                .get("/api/health")
                .then()
                .contentType(ContentType.JSON);

        given()
                .when()
                .get("/api/health/ready")
                .then()
                .contentType(ContentType.JSON);

        given()
                .when()
                .get("/api/health/live")
                .then()
                .contentType(ContentType.JSON);
    }

    @Test
    void healthEndpoints_ShouldRespondQuickly() {
        given()
                .when()
                .get("/api/health")
                .then()
                .statusCode(200)
                .time(lessThan(5000L)); // Should respond within 5 seconds

        given()
                .when()
                .get("/api/health/ready")
                .then()
                .statusCode(200)
                .time(lessThan(5000L));

        given()
                .when()
                .get("/api/health/live")
                .then()
                .statusCode(200)
                .time(lessThan(1000L)); // Liveness should be very fast
    }
} 