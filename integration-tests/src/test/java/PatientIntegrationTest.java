import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {

    @BeforeAll
    static void setUp() {
        // Setting the base URI for RestAssured to point to the API gateway at localhost:4004.
        // This ensures all requests in the tests use this as the base URL.
        RestAssured.baseURI = "http://localhost:4004";
        //this is same as login integration test because we setted all link to api-gateway
        //so we only need api-gateway endpoint.
    }

    @Test
    public void shouldReturnPatientsWithValidToken() {
        // Step 1: Authenticate and obtain a valid token

        // Creating a JSON payload with valid login credentials.
        // This simulates a user logging in with correct credentials.
        String loginPayload = """
                  {
                      "email": "testuser@test.com",
                      "password":"password123"
                  }
                """;

        // Sending a POST request to the /auth/login endpoint with valid credentials.
        // Expecting a 200 status code and a response body containing a valid token.
        String token = given()
                .contentType("application/json")  // Setting content type to JSON
                .body(loginPayload)  // Attaching the login payload to the request body
                .when()
                .post("/auth/login")  // Making a POST request to the login endpoint
                .then()
                .statusCode(200)  // Asserting that the response status is 200 (OK)
                .body("token", notNullValue())  // Asserting that the response contains a token
                .extract()
                .jsonPath()
                .get("token");  // Extracting the token from the response JSON

        // Step 2: Use the token to fetch patient data

        // Sending a GET request to the /api/patients endpoint with the obtained token.
        // The token is passed as an Authorization header using the Bearer scheme.
        given()
                .header("Authorization", "Bearer " + token)  // Attaching the token in the request header
                .when()
                .get("/api/patients")  // Making a GET request to retrieve patient data
                .then()
                .statusCode(200)  // Expecting a 200 status code indicating success
                .body("patients", notNullValue()); // Asserting that the response contains a non-null "patients" field
    }
}
