package br.com.webber.techchallenge.apigateway.function;

import br.com.webber.techchallenge.apigateway.model.RatingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Gateway {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final RestTemplate restTemplate = new RestTemplate();

    @FunctionName("ApiGatewayCourseRatingsFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                    route = "apigateway",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Java HTTP trigger processed a request.");
        try {
            String body = validateBody(request);
            RatingRequest ratingDto = parseRequestBody(body);


            String urlFuncaoB = System.getenv("URL_FUNC_PRIVATE"); // Ex: https://.../api/ratings
            String secretToken = System.getenv("INTERNAL_SECRET_TOKEN");

            // 4. Preparar a chamada HTTP para a Função B
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-internal-secret", secretToken); // Header de segurança manual

            HttpEntity<RatingRequest> entity = new HttpEntity<>(ratingDto, headers);
            context.getLogger().info("entity " + entity);

            ResponseEntity<String> responseB = restTemplate.exchange(
                    urlFuncaoB,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    String.class
            );

            // 6. Retornar o resultado da Função B para o cliente
            return request.createResponseBuilder(HttpStatus.valueOf(responseB.getStatusCodeValue()))
                    .header("Content-Type", "application/json")
                    .body(responseB.getBody())
                    .build();

        } catch (IllegalArgumentException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(e.getMessage()).build();
        } catch (Exception e) {
            context.getLogger().severe("Erro no Gateway: " + e.getMessage());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao processar integração interna.")
                .build();
        }
    }

    private String validateBody(HttpRequestMessage<Optional<String>> request) {
        String body = request.getBody().orElse(null);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Body required. Send JSON with rating and description");
        }
        return body;
    }

    private RatingRequest parseRequestBody(String body) {
        try {
            return mapper.readValue(body, RatingRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON");
        }
    }
}
