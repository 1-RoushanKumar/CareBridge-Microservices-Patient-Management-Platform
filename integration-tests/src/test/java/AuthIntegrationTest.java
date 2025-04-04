import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {

    //here we set the base URI for the tests
//we are testing api-gateway endpoint first because it was the one who run first when login.
    @BeforeAll
    static void setUp() {
        // Setting the base URI for RestAssured to point to the API gateway at localhost:4004.
        // This ensures all requests in the tests use this as the base URL.
        RestAssured.baseURI = "http://localhost:4004";
    }

    //this test is to check if the login endpoint is working correctly
    @Test
    public void shouldReturnOkWithValidToken() {
        //how to structure test
        //1.Arrange
        //2.Act
        //3.Assert

        //Arrange
        // Creating a JSON payload containing valid login credentials.
        // This simulates a real user trying to log in with the correct email and password.
        String loginPayload = """
                  {
                      "email": "testuser@test.com",
                      "password":"password123"
                  }
                """;

        //Act and asserting.
        // Sending a POST request to the /auth/login endpoint with the loginPayload.
        // Setting the Content-Type header to application/json.
        // Expecting a 200 status code response indicating success.
        // Verifying that the response body contains a non-null "token" field,
        // which confirms successful authentication.
        Response response = given()
                .contentType("application/json")  // Setting content type to JSON
                .body(loginPayload)  // Attaching the login payload to the request body
                .when()
                .post("/auth/login")  // Making a POST request to the login endpoint
                .then()
                .statusCode(200)  // Asserting that the response status is 200 (OK)
                .body("token", notNullValue())  // Asserting that the response contains a token
                .extract()
                .response();  // Extracting the response object for further use

        // Printing the generated token to verify its presence in the response.
        System.out.println("Generated Token: " + response.jsonPath().getString("token"));
    }

    //this test is to check if the login endpoint is not working correctly
    @Test
    public void shouldReturnUnauthorizedOnInvalidLogin() {
        //how to structure test
        //1.Arrange
        //2.Act
        //3.Assert

        //Arrange
        // Creating a JSON payload with invalid login credentials.
        // This simulates an unauthorized user attempting to log in with incorrect details.
        String loginPayload = """
                  {
                      "email": "invalid_user@test.com",
                      "password":"wrongpassword"
                  }
                """;

        //Act and asserting.
        // Sending a POST request with incorrect login credentials.
        // Expecting a 401 status code response, which indicates "Unauthorized."
        // The response body is not checked because when authentication fails,
        // we do not expect a token in the response.
        given()
                .contentType("application/json")  // Setting content type to JSON
                .body(loginPayload)  // Attaching the invalid login payload to the request
                .when()
                .post("/auth/login")  // Making a POST request to the login endpoint
                .then()
                .statusCode(401);  // Asserting that the response status is 401 (Unauthorized)

        //here i remove body because if we get error we don't need to check body for token.
    }

}
