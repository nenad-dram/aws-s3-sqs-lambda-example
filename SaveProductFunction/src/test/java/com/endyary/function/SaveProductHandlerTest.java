package com.endyary.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
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
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class SaveProductHandlerTest {
  @Mock
  private Context context;
  @Mock
  private LambdaLogger logger;
  @Mock
  private SQSEvent sqsEvent;
  private static DynamoDbEnhancedClient dbClient;
  private static DynamoDbTable<Product> dynamoDbTable;
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
    dynamoDbTable = dbClient.table(TABLE_NAME, TableSchema.fromBean(Product.class));
    dynamoDbTable.createTable();
  }


  @Test
  void shouldSaveToDb() {
    List<SQSMessage> queueMessages = getTestQueueMessages();
    when(context.getLogger()).thenReturn(logger);
    when(sqsEvent.getRecords()).thenReturn(queueMessages);

    try (MockedStatic<DynamoDbProvider> dynamoDbProviderMocked = mockStatic(DynamoDbProvider.class)) {
      dynamoDbProviderMocked.when(DynamoDbProvider::getEnhancedClient).thenReturn(dbClient);
      dynamoDbProviderMocked.when(DynamoDbProvider::getTableName).thenReturn(TABLE_NAME);

      SaveProductHandler saveProductHandler = new SaveProductHandler();
      Void result = saveProductHandler.handleRequest(sqsEvent, context);
      assertNull(result);

       List<Product> dbProducts = dynamoDbTable.scan().items().stream().toList();
       assertEquals(queueMessages.size(), dbProducts.size());
    }
  }

  private List<SQSMessage> getTestQueueMessages() {
    try (InputStream inputStream = SaveProductHandlerTest.class.getClassLoader()
        .getResourceAsStream("data.json")) {
      assert inputStream != null;
      return JsonNodeParser.builder().build()
          .parse(inputStream).asArray().stream()
          .map(jsonNode -> {
            SQSMessage sqsMessage = new SQSMessage();
            sqsMessage.setMessageId(UUID.randomUUID().toString());
            sqsMessage.setBody(jsonNode.toString());
            return sqsMessage;
          })
          .toList();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
