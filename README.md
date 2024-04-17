# aws-s3-sqs-lambda-example

The purpose of this project is to demonstrate the usage of serverless AWS services - S3, Lambda, SQS, DynamoDB and API Gateway to create a simple application.  
AWS SAM framework is used to facilitate application deployment.

## Tech Stack
JDK 21, AWS SDK, JUnit 5, Mockito, Testcontainers, GSON, Maven, AWS SAM

## Description
As depicted on the diagram bellow the app flow starts when a JSON file is uploaded into the S3 bucket, which will create an event for `ReadFileFunction` lambda.
The lambda's role is to read JSON data from the bucket, and send them as messages into `product-queue`. The queue will send an event to `SaveProductFunction` lambda,
whose role is to read all messages and store them into `Products` (a DynamoDB table). Products stored in the table can be read through exposed API method (API Gateway + `GetProductsFunction` lambda).

All features are covered by integration tests (via Testcontainers and localstack image).  
AWS resources are defined in the `template.yaml` file.

## Architecture diagram

![Alt text](./resources/aws-diagram.png?raw=true)

## Build and deployment
To build and deploy run the following in a shell:

```bash
sam validate
sam build
sam deploy --guided --parameter-overrides BucketName=a-unique-bucket-name
```
The first command will validate the template file.  
The second command will build the source of your application.  
The third command will package and deploy the application to AWS, with a series of prompts.

## Cleanup
To delete the application (the created stack) from an AWS account, run the following:

```bash
sam delete --stack-name name-of-the-stack
```