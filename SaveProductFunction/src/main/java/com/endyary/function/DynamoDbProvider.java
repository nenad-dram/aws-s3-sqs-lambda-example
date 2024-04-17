package com.endyary.function;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbProvider {

  public static final String ENV_VARIABLE_TABLE = "PRODUCT_TABLE";

  private DynamoDbProvider() {
  }

  public static DynamoDbEnhancedClient getEnhancedClient() {
    return DynamoDbEnhancedClient.builder()
        .dynamoDbClient(DynamoDbClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build())
        .build();
  }

  public static String getTableName() {
    return System.getenv(ENV_VARIABLE_TABLE);
  }

}
