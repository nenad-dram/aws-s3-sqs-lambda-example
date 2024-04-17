package com.endyary.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class ReadFileHandlerTest {
  @Mock
  private Context context;
  @Mock
  private LambdaLogger logger;
  @Mock
  private S3Event s3Event;
  private static S3Client s3Client;
  private static SqsClient sqsClient;
  private static String sqsQueueUrl;
  private static final String BUCKET_NAME = "test-bucket";
  private static final String OBJECT_KEY = "test-key";
  private static final String QUEUE_NAME = "test-queue";

  @Container
  static LocalStackContainer localStack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:latest")
  );

  @BeforeAll
  static void init() {

    s3Client = S3Client.builder()
        .forcePathStyle(true)
        .endpointOverride(localStack.getEndpoint())
        .build();
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

    putTestDataInBucket();

    sqsClient = SqsClient.builder()
        .endpointOverride(localStack.getEndpoint())
        .build();

    sqsClient.createQueue(CreateQueueRequest.builder()
        .queueName(QUEUE_NAME).build());

    sqsQueueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build()).queueUrl();
  }

  @Test
  void shouldSendToQueue() {
    when(context.getLogger()).thenReturn(logger);
    when(s3Event.getRecords()).thenReturn(getTestEventRecordList());

    try (MockedStatic<ClientProvider> clientProviderMocked = mockStatic(ClientProvider.class)) {
      clientProviderMocked.when(ClientProvider::getS3Client).thenReturn(s3Client);
      clientProviderMocked.when(ClientProvider::getSQSClient).thenReturn(sqsClient);
      clientProviderMocked.when(ClientProvider::getQueueUrl).thenReturn(sqsQueueUrl);

      ReadFileHandler readFileHandler = new ReadFileHandler();
      Void result = readFileHandler.handleRequest(s3Event, context);

      assertNull(result);
      assertQueueSize();
    }
  }

  void assertQueueSize() {
    sqsClient = SqsClient.builder()
        .endpointOverride(localStack.getEndpoint())
        .build();
    GetQueueUrlResponse queueUrlResp =
        sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build());

    List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
        .queueUrl(queueUrlResp.queueUrl())
        .maxNumberOfMessages(10)
        .build()).messages();

    assertEquals(10, messages.size());
  }

  private static byte[] getTestFileContent() {
    try (InputStream inputStream = ReadFileHandlerTest.class.getClassLoader()
        .getResourceAsStream("data.json")) {
      assert inputStream != null;
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void putTestDataInBucket() {
    RequestBody body = RequestBody.fromBytes(getTestFileContent());
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(OBJECT_KEY).build(), body);
  }

  private List<S3EventNotificationRecord> getTestEventRecordList() {
    S3ObjectEntity s3Object = new S3ObjectEntity(OBJECT_KEY, null, null, null, null);
    S3BucketEntity s3Bucket = new S3BucketEntity(BUCKET_NAME, null, null);
    S3Entity s3Entity = new S3Entity(null, s3Bucket, s3Object, null);
    S3EventNotificationRecord s3Record = new S3EventNotificationRecord(null, null, null, null, null,
        null, null, s3Entity, null);
    return List.of(s3Record);
  }
}
