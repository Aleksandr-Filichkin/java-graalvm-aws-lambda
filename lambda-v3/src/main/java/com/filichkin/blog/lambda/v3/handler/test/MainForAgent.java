package com.filichkin.blog.lambda.v3.handler.test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filichkin.blog.lambda.model.Book;
import com.filichkin.blog.lambda.service.RequestDispatcher;
import com.filichkin.blog.lambda.storage.EnhancedClientBookStorage;
import com.filichkin.blog.lambda.v3.handler.test.model.InvocationResponse;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

@Slf4j
public class MainForAgent {

    private static final String REQUEST_ID_HEADER = "lambda-runtime-aws-request-id";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final RequestDispatcher REQUEST_DISPATCHER = initDispatcher();
    private static final String TABLE_NAME = "books";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();


    private static RequestDispatcher initDispatcher() {
        DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("XXXXX", "XXXX")))
                        .region(Region.US_EAST_1)
                        .httpClientBuilder(UrlConnectionHttpClient.builder()).build())
                .build();
        StaticTableSchema<Book> schema = buildDynamodbSchema();
        DynamoDbTable<Book> dynamoDbTable = dynamoDbEnhancedClient.table(TABLE_NAME, schema);
        return new RequestDispatcher(new EnhancedClientBookStorage(dynamoDbTable), OBJECT_MAPPER);
    }

    /**
     * cannot use https://github.com/aws/aws-sdk-java-v2/issues/2445
     */
    private static StaticTableSchema<Book> buildDynamodbSchema() {
        return StaticTableSchema.builder(Book.class)
                .newItemSupplier(Book::new)
                .addAttribute(String.class, a -> a.name("id")
                        .getter(Book::getId)
                        .setter(Book::setId)
                        .tags(primaryPartitionKey()))
                .addAttribute(String.class, a -> a.name("name")
                        .getter(Book::getName)
                        .setter(Book::setName))
                .addAttribute(String.class, a -> a.name("author")
                        .getter(Book::getAuthor)
                        .setter(Book::setAuthor))
                .build();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
//        while (true) {
        String endpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");
        InvocationResponse invocation = getInvocation(endpoint);

        try {
            APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = OBJECT_MAPPER.readValue(invocation.getEvent(), APIGatewayProxyRequestEvent.class);
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = REQUEST_DISPATCHER.dispatch(apiGatewayProxyRequestEvent);

            log.info(OBJECT_MAPPER.writeValueAsString(apiGatewayProxyResponseEvent));
//                 Post to Lambda success endpoint
//                HttpRequest request = HttpRequest.newBuilder()
//                        .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(apiGatewayProxyResponseEvent)))
//                        .uri(URI.create(String.format("http://%s/2018-06-01/runtime/invocation/%s/response", endpoint, invocation.getRequestId())))
//                        .build();
//                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            handleException(endpoint, invocation, e);
        }
//        }
    }

    private static void handleException(String endpoint, InvocationResponse invocation, Exception exception) throws IOException, InterruptedException {


        String errorBody = OBJECT_MAPPER.writeValueAsString(Map.of("error", exception.getMessage()));

        APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
        errorResponse.setStatusCode(500);
        errorResponse.setBody(errorBody);

        // Post to Lambda error endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(errorResponse)))
                .uri(URI.create(String.format("http://%s/2018-06-01/runtime/invocation/%s/error", endpoint, invocation.getRequestId())))
                .build();
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static InvocationResponse getInvocation(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(String.format("http://%s/2018-06-01/runtime/invocation/next", endpoint)))
                .build();

//        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
//        String requestId = response.headers().firstValue(REQUEST_ID_HEADER).orElseThrow();
        return new InvocationResponse("requestId", "{     \"resource\": \"/v3/book\",     \"path\": \"/v3/book\",     \"httpMethod\": \"POST\",     \"headers\": {         \"Accept\": \"application/json\",         \"Accept-Encoding\": \"gzip,deflate\",         \"CloudFront-Forwarded-Proto\": \"https\",         \"CloudFront-Is-Desktop-Viewer\": \"true\",         \"CloudFront-Is-Mobile-Viewer\": \"false\",         \"CloudFront-Is-SmartTV-Viewer\": \"false\",         \"CloudFront-Is-Tablet-Viewer\": \"false\",         \"CloudFront-Viewer-Country\": \"ES\",         \"content-type\": \"application/json\",         \"Host\": \"3olqg31gjc.execute-api.us-east-1.amazonaws.com\",         \"User-Agent\": \"Apache-HttpClient/4.5.13 (Java/11.0.11)\",         \"Via\": \"1.1 2acf3d0afaac4c2afcd872669e134733.cloudfront.net (CloudFront)\",         \"X-Amz-Cf-Id\": \"O68UigvQMzekLvhbMye1T7UpD4HREjKSO5nUaqi8KdzOgDw1ZSTVCQ==\",         \"X-Amzn-Trace-Id\": \"Root=1-60f2ced7-4381aabc6f02aef01217693b\",         \"X-Forwarded-For\": \"213.94.3.38, 70.132.45.82\",         \"X-Forwarded-Port\": \"443\",         \"X-Forwarded-Proto\": \"https\"     },     \"multiValueHeaders\": {         \"Accept\": [             \"application/json\"         ],         \"Accept-Encoding\": [             \"gzip,deflate\"         ],         \"CloudFront-Forwarded-Proto\": [             \"https\"         ],         \"CloudFront-Is-Desktop-Viewer\": [             \"true\"         ],         \"CloudFront-Is-Mobile-Viewer\": [             \"false\"         ],         \"CloudFront-Is-SmartTV-Viewer\": [             \"false\"         ],         \"CloudFront-Is-Tablet-Viewer\": [             \"false\"         ],         \"CloudFront-Viewer-Country\": [             \"ES\"         ],         \"content-type\": [             \"application/json\"         ],         \"Host\": [             \"3olqg31gjc.execute-api.us-east-1.amazonaws.com\"         ],         \"User-Agent\": [             \"Apache-HttpClient/4.5.13 (Java/11.0.11)\"         ],         \"Via\": [             \"1.1 2acf3d0afaac4c2afcd872669e134733.cloudfront.net (CloudFront)\"         ],         \"X-Amz-Cf-Id\": [             \"O68UigvQMzekLvhbMye1T7UpD4HREjKSO5nUaqi8KdzOgDw1ZSTVCQ==\"         ],         \"X-Amzn-Trace-Id\": [             \"Root=1-60f2ced7-4381aabc6f02aef01217693b\"         ],         \"X-Forwarded-For\": [             \"213.94.3.38, 70.132.45.82\"         ],         \"X-Forwarded-Port\": [             \"443\"         ],         \"X-Forwarded-Proto\": [             \"https\"         ]     },     \"queryStringParameters\": null,     \"multiValueQueryStringParameters\": null,     \"pathParameters\": null,     \"stageVariables\": null,     \"requestContext\": {         \"resourceId\": \"7tq9am\",         \"resourcePath\": \"/v3/book\",         \"httpMethod\": \"POST\",         \"extendedRequestId\": \"CnVBtHpiIAMF9JQ=\",         \"requestTime\": \"17/Jul/2021:12:36:39 +0000\",         \"path\": \"/Prod/v3/book\",         \"accountId\": \"853325250913\",         \"protocol\": \"HTTP/1.1\",         \"stage\": \"Prod\",         \"domainPrefix\": \"3olqg31gjc\",         \"requestTimeEpoch\": 1626525399577,         \"requestId\": \"f8d5c3cc-9fbc-483e-92f5-2ce8b4b58e0a\",         \"identity\": {             \"cognitoIdentityPoolId\": null,             \"accountId\": null,             \"cognitoIdentityId\": null,             \"caller\": null,             \"sourceIp\": \"213.94.3.38\",             \"principalOrgId\": null,             \"accessKey\": null,             \"cognitoAuthenticationType\": null,             \"cognitoAuthenticationProvider\": null,             \"userArn\": null,             \"userAgent\": \"Apache-HttpClient/4.5.13 (Java/11.0.11)\",             \"user\": null         },         \"domainName\": \"3olqg31gjc.execute-api.us-east-1.amazonaws.com\",         \"apiId\": \"3olqg31gjc\"     },     \"body\": \"{\\n  \\\"name\\\": \\\"Sotnikov\\\",\\n  \\\"author\\\": \\\"Vasil Baykoav\\\"\\n}\",     \"isBase64Encoded\": false }");
    }
}

