package com.endyary.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.HttpStatusCode;

public class GetProductsHandler implements RequestHandler<APIGatewayProxyRequestEvent,
    APIGatewayProxyResponseEvent> {

  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<Product> productTableSchema;

  public GetProductsHandler() {
    dbClient = DynamoDbProvider.getEnhancedClient();
    tableName = DynamoDbProvider.getTableName();
    productTableSchema = TableSchema.fromImmutableClass(Product.class);
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

    DynamoDbTable<Product> productTable = dbClient.table(tableName, productTableSchema);
    Gson gson = new Gson();
    List<Product> responseList = productTable.scan().items().stream().toList();

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(HttpStatusCode.OK)
        .withHeaders(Collections.emptyMap())
        .withBody(gson.toJson(responseList));
  }
}