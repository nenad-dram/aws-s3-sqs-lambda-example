package com.endyary.function;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

public class ClientProvider {

  public static final String ENV_VARIABLE_QUEUE= "PRODUCT_QUEUE";

  private ClientProvider(){}

  public static S3Client getS3Client() {
    return S3Client.builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
        .build();
  }

  public static SqsClient getSQSClient() {
    return SqsClient.builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
        .build();
  }

  public static String getQueueUrl() {
    return System.getenv(ENV_VARIABLE_QUEUE);
  }
}
