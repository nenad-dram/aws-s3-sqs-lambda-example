package com.endyary.function;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import java.io.InputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import java.util.UUID;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Handler for requests to Lambda function.
 */
public class ReadFileHandler implements RequestHandler<S3Event, Void> {

  private final S3Client s3Client;
  private final SqsClient sqsClient;
  private final JsonNodeParser jsonParser;

  public ReadFileHandler() {
    s3Client = ClientProvider.getS3Client();
    sqsClient = ClientProvider.getSQSClient();
    jsonParser = JsonNodeParser.builder().build();
  }

  public Void handleRequest(final S3Event s3Event, final Context context) {
    LambdaLogger logger = context.getLogger();

    S3EventNotificationRecord eventRecord = s3Event.getRecords().get(0);
    String bucket = eventRecord.getS3().getBucket().getName();
    String key = eventRecord.getS3().getObject().getUrlDecodedKey();
    logger.log(String.format("Event from bucket %s and key %s", bucket, key));

    List<JsonNode> jsonProducts = getJsonProducts(bucket, key);
    logger.log("Number of products: " + jsonProducts.size());

    SendMessageBatchResponse queueResponse = sendToQueue(jsonProducts);
    String resultMessage = queueResponse.hasFailed() ? "Failed" : "Successful";
    logger.log("Send to queue result: " + resultMessage);

    if (queueResponse.hasFailed()) {
      logFailedMessages(queueResponse.failed(), logger);
    }

    return null;
  }

  private List<JsonNode> getJsonProducts(final String bucket, final String key) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    InputStream inputStream = s3Client.getObject(getObjectRequest);
    JsonNode jsonContent = jsonParser.parse(inputStream);
    s3Client.close();

    return jsonContent.asArray();
  }

  private SendMessageBatchResponse sendToQueue(final List<JsonNode> jsonProducts) {
    String queueUrl = ClientProvider.getQueueUrl();

    List<SendMessageBatchRequestEntry> batchRequestEntries = jsonProducts.stream()
        .map(product -> SendMessageBatchRequestEntry.builder()
            .id(UUID.randomUUID().toString())
            .messageBody(product.toString())
            .build())
        .toList();

    SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest.builder()
        .queueUrl(queueUrl)
        .entries(batchRequestEntries)
        .build();
    SendMessageBatchResponse response = sqsClient.sendMessageBatch(sendMessageBatchRequest);
    sqsClient.close();

    return response;
  }

  private void logFailedMessages(final List<BatchResultErrorEntry> errorEntries,
      final LambdaLogger logger) {
    errorEntries.forEach(
        entry -> logger.log(entry.id() + " failed with error: " + entry.message()));
  }
}
