package uk.co.howes.sean.ledger.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import uk.co.howes.sean.ledger.exceptions.OverDrawnException;

/**
 * Verifies ValidationFailureHandler without static mocking: for each failure type, the
 * interactions the handler causes on the RoutingContext and HttpServerResponse must match
 * those caused by calling the expected ErrorResponse method directly.
 */
class ValidationFailureHandlerTest {

  private final ValidationFailureHandler handler = new ValidationFailureHandler();

  @Test
  @DisplayName("OverDrawnException produces the overdrawn error response")
  void handle_overDrawnException_producesOverdrawnResponse() {
    OverDrawnException failure = mock(OverDrawnException.class);

    List<String> actual = run(failure, handler::handle);
    List<String> expected = run(failure, ErrorResponse::sendOverdrawnError);

    assertFalse(actual.isEmpty(), "handler should have written a response");
    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("Any other failure produces the validation error response for that failure")
  void handle_otherFailure_producesValidationResponse() {
    Throwable failure = new IllegalArgumentException("amount must be positive");

    List<String> actual = run(failure, handler::handle);
    List<String> expected = run(failure, ctx -> ErrorResponse.sendValidationError(ctx, failure));

    assertFalse(actual.isEmpty(), "handler should have written a response");
    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("A null failure produces the validation error response")
  void handle_nullFailure_producesValidationResponse() {
    List<String> actual = run(null, handler::handle);
    List<String> expected = run(null, ctx -> ErrorResponse.sendValidationError(ctx, null));

    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("Overdrawn and validation failures produce different responses")
  void handle_overDrawnAndValidation_areDistinguished() {
    List<String> overdrawn = run(mock(OverDrawnException.class), handler::handle);
    List<String> validation = run(new IllegalArgumentException("bad"), handler::handle);

    assertNotEquals(overdrawn, validation);
  }

  /** Runs the action against a fresh mocked context and returns every interaction recorded. */
  private static List<String> run(Throwable failure, Consumer<RoutingContext> action) {
    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class, Mockito.RETURNS_SELF);
    when(ctx.failure()).thenReturn(failure);
    when(ctx.response()).thenReturn(response);

    action.accept(ctx);

    return Stream.concat(
        Mockito.mockingDetails(ctx).getInvocations().stream()
          .filter(i -> !i.getMethod().getName().equals("failure"))
          .filter(i -> !i.getMethod().getName().equals("response")),
        Mockito.mockingDetails(response).getInvocations().stream())
      .map(ValidationFailureHandlerTest::describe)
      .collect(Collectors.toList());
  }

  private static String describe(Invocation invocation) {
    return invocation.getMethod().getDeclaringClass().getSimpleName()
      + "."
      + invocation.getMethod().getName()
      + List.of(invocation.getArguments());
  }
}
