package com.endyary.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class GetProductsHandlerTest {
  private static DynamoDbEnhancedClient dbClient;
  private static DynamoDbTable<Product> dynamoDbTable;
  @Mock
  private Context context;
  @Mock
  private APIGatewayProxyRequestEvent request;
  private static final String TABLE_NAME = "Products";

  @Container
  static LocalStackContainer localStack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:latest")

  );

  @BeforeAll
  static void init() {
    dbClient = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(
            DynamoDbClient.builder().endpointOverride(localStack.getEndpoint())
                .build())
        .build();
    dynamoDbTable = dbClient.table(TABLE_NAME, TableSchema.fromImmutableClass(Product.class));
    dynamoDbTable.createTable();
  }

  @Test
  void shouldReturnProducts() {
    insertProducts();

    try (MockedStatic<DynamoDbProvider> dynamoDbProviderMocked = mockStatic(
        DynamoDbProvider.class)) {
      dynamoDbProviderMocked.when(DynamoDbProvider::getEnhancedClient).thenReturn(dbClient);
      dynamoDbProviderMocked.when(DynamoDbProvider::getTableName).thenReturn(TABLE_NAME);
      GetProductsHandler getProductsHandler = new GetProductsHandler();
      APIGatewayProxyResponseEvent response = getProductsHandler.handleRequest(request, context);

      assertEquals(HttpStatusCode.OK, response.getStatusCode());
      Product[] products = new Gson().fromJson(response.getBody(), Product[].class);
      assertEquals(3, products.length);
    }
  }

  private void insertProducts() {
    Product testProduct1 = Product.builder()
        .id(UUID.randomUUID().toString())
        .name("Product 1")
        .category("Category 1")
        .price(10)
        .build();
    Product testProduct2 = Product.builder()
        .id(UUID.randomUUID().toString())
        .name("Product 2")
        .category("Category 2")
        .price(20)
        .build();
    Product testProduct3 = Product.builder()
        .id(UUID.randomUUID().toString())
        .name("Product 3")
        .category("Category 3")
        .price(30)
        .build();

    BatchWriteItemEnhancedRequest writeItemRequest = BatchWriteItemEnhancedRequest.builder()
        .writeBatches(WriteBatch.builder(Product.class)
            .mappedTableResource(dynamoDbTable)
            .addPutItem(builder -> builder.item(testProduct1))
            .addPutItem(builder -> builder.item(testProduct2))
            .addPutItem(builder -> builder.item(testProduct3))
            .build())
        .build();

    dbClient.batchWriteItem(writeItemRequest);
  }
}
