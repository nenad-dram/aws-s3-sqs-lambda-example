package com.endyary.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

public class SaveProductHandler implements RequestHandler<SQSEvent, Void> {

  private final Gson gson;
  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<Product> userTableSchema;

  public SaveProductHandler() {
    gson = new Gson();
    dbClient = DynamoDbProvider.getEnhancedClient();
    tableName = DynamoDbProvider.getTableName();
    userTableSchema = TableSchema.fromBean(Product.class);
  }
  @Override
  public Void handleRequest(SQSEvent sqsEvent, Context context) {
    LambdaLogger logger = context.getLogger();
    logger.log("Received event, number of records: " + sqsEvent.getRecords().size());

    List<Product> products = sqsEvent.getRecords().stream()
        .map(sqsMessage -> {
          Product product = gson.fromJson(sqsMessage.getBody(), Product.class);
          product.setId(sqsMessage.getMessageId());
          return product;
        }).toList();

    DynamoDbTable<Product> productTable = dbClient.table(tableName, userTableSchema);
    WriteBatch.Builder<Product> writeBatchBuilder = WriteBatch.builder(Product.class)
        .mappedTableResource(productTable);
    products.forEach(writeBatchBuilder::addPutItem);

    dbClient.batchWriteItem(builder -> builder.writeBatches(writeBatchBuilder.build()));

    return null;
  }
}