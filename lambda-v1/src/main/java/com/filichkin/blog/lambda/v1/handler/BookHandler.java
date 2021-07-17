package com.filichkin.blog.lambda.v1.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filichkin.blog.lambda.model.Book;
import com.filichkin.blog.lambda.service.RequestDispatcher;
import com.filichkin.blog.lambda.storage.EnhancedClientBookStorage;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;


public class BookHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "books";
    private final RequestDispatcher requestDispatcher;

    public BookHandler() {
        DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDbEnhancedClient.create();
        DynamoDbTable<Book> dynamoDbTable = dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(Book.class));
        requestDispatcher = new RequestDispatcher(new EnhancedClientBookStorage(dynamoDbTable), new ObjectMapper());
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        return requestDispatcher.dispatch(apiGatewayProxyRequestEvent);
    }
}
