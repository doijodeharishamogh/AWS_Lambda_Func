# csye6225-fa19-lambda

## Team Information

| Name | NEU ID | Email Address |
| --- | --- | --- |
| Veena Vasudevan Iyer | 001447061 | iyer.v@husky.neu.edu |
| Amogh Doijode Harish| 001449026 | doijodeharish.a@husky.neu.edu |
| Ravi Kiran | 001491808 | lnu.ra@husky.neu.edu |
| | | |

## Technology Stack
The application uses Lambda function to trigger email when SNS topic is created
The lambda function is created in AWS console.
Dynamodb is used to store token which indicates time to live

## Build Instructions
To trigger lambda function to send email, clone git repository
git@github.com:VeenaIyer-17/csye6225-fa19-lambda.git

## Deploy Instructions
We can trigger the Code Deployment using curl command to call the circleci API

## CICD
For CircleCi to read the config.yml we need to set inputs in CircleCI environment variables
Setup your circleci user credentials in circle ci environment which is created in AWS console
Setup code deploy bucket name which is the bucket created in AWS console for code deploy to upload the s3 artifact
Setup the region in circle ci environment variables where the code deloy should take place
Specify the branch name in circle ci for which build needs to be triggered
Command to trigger CICD
curl -u {circlecitoken} -d build_parameters[CIRCLE_JOB]=build https://circleci.com/api/v1.1/project/github/VeenaIyer-17/csye6225-fa19-lambda/tree/master