package com.endyary.function;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbImmutable(builder = Product.Builder.class)
public class Product {
  private final String id;
  private final String name;
  private final String category;
  private final float price;

  private Product(Builder b) {
    this.id = b.id;
    this.name = b.name;
    this.category = b.category;
    this.price = b.price;
  }
  public static Builder builder() { return new Builder(); }

  @DynamoDbPartitionKey
  public String id() { return this.id; }
  public String name() { return this.name; }
  public String category() { return this.category; }
  public float price() { return this.price; }

  public static final class Builder {
    private String id;
    private String name;
    private String category;
    private float price;

    private Builder() {}

    public Builder id(String id) {this.id = id; return this;}
    public Builder name(String name) {this.name = name; return this;}
    public Builder category(String category) {this.category = category; return this;}
    public Builder price(float price) {this.price = price; return this;}

    public Product build() { return new Product(this); }
  }
}
