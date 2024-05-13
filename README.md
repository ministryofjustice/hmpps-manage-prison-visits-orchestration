# HMPPS Manage Prison Visits Orchestration API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-prison-visits-orchestration/tree/main.svg?style=shield)](https://app.circleci.com/pipelines/github/ministryofjustice/visit-scheduler)

This is a Spring Boot application, written in Kotlin, used as an orchestration layer between the Visits front end and external API calls like visit-scheduler, prison-api, prisoner-search. Used by [Visits UI](https://github.com/ministryofjustice/book-a-prison-visit-staff-ui).

## Building

To build the project (without tests):
```
./gradlew clean build -x test
```

## Testing

Run:
```
./gradlew test 
```

## Running
You have 2 options to run the visit-orchestration service. Either connect to the dev environment for all services (except local stack) 
or connect to dev environment and run a local version of the visit-scheduler.

First, create a .env file at the project root and add 2 secrets to it
```
SYSTEM_CLIENT_ID='get from kubernetes secrets for dev namespace'
SYSTEM_CLIENT_SECRET='get from kubernetes secrets for dev namespace'
```

Then create a Spring Boot run configuration with active profile of 'dev' and set an environments file to the
`.env` file we just created. Run the service in your chosen IDE.

### Using dev environment (recommended)
```
docker-compose up -d
```

### Using dev environment and local visit-scheduler
To use the visit-orchestration service with a local visit-scheduler, do not run docker compose. Instead, change the `visit-scheduler.api.url` 
field in the `application-dev.yml` file to `http://localhost:8081`, and run the visit-orchestration service 
in your IDE.

Now clone the [Visit scheduler](https://github.com/ministryofjustice/visit-scheduler) repo and follow the set up guide for that service.

## Ports

| Service             | Port |  
|---------------------|------|
| visit-orchestration | 8080 |
Call info endpoint:
```
$ curl 'http://localhost:8080/info' -i -X GET
```

## Swagger v3
Manage Prison Visits Orchestration
```
http://localhost:8080/swagger-ui/index.html
```

Export Spec
```
http://localhost:8080/v3/api-docs?group=full-api
```

## Common gradle tasks

To list project dependencies, run:

```
./gradlew dependencies
``` 

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```

To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```

#### Ktlint Gradle Tasks

To run Ktlint check:
```
./gradlew ktlintCheck
```

To run Ktlint format:
```
./gradlew ktlintFormat
```

To apply ktlint styles to intellij
```
./gradlew ktlintApplyToIdea
```

To register pre-commit check to run Ktlint format:
```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook 
```

...or to register pre-commit check to only run Ktlint check:
```
./gradlew ktlintApplyToIdea addKtlintCheckGitPreCommitHook
```


#### Send notifications locally
To help test notification events locally we can send events to localstack to replicate what NOMIS would do -
##### Step 1 - Start Orchestration service on local

##### Step 2 - Install awscli (if not already installed)
```
brew install awscli
```

##### Step 3 - configure aws with dummy values (if not already configured)
```
aws configure
```
Put any dummy value for AWS_ACCESS_KEY and AWS_SECRET_KEY and eu-west-2 as default region.
The queueName is the value of hmpps.sqs.queues.prisonvisitsevents.queueName on the application-<env>.yml file.
So the queue URL would usually be - http://localhost:4566/000000000000/{queueName}

##### Step 4 - Send a message to the application queue. The below is a non-assciation event for prisoners G0026GC and A8713DY. Replace the prisoner numbers with your test values.
```
aws sqs send-message \
  --endpoint-url=http://localhost:4566 \
  --queue-url=http://localhost:4566/000000000000/sqs_hmpps_prison_visits_event_queue \
  --message-body \
    '{"Type":"Notification", "Message": "{\"eventType\":\"non-associations.created\",\"additionalInformation\":{\"nsPrisonerNumber2\":\"G4206GT\",\"nsPrisonerNumber1\":\"G4216GE\"}}", "MessageId": "123"}'
```

If you are unsure about the queue name you can check the queue names using the following command and replace it in the above --queue-url value parameter
```
aws sqs list-queues --endpoint-url=http://localhost:4566
```

Sample send message requests by event type - 
Non-associations (non-associations.created)
```
aws sqs send-message \
  --endpoint-url=http://localhost:4566 \
  --queue-url=http://localhost:4566/000000000000/sqs_hmpps_prison_visits_event_queue \
  --message-body \
    '{"Type":"Notification", "Message": "{\"eventType\":\"non-associations.created\",\"additionalInformation\":{\"nsPrisonerNumber2\":\"G4206GT\",\"nsPrisonerNumber1\":\"G4216GE\"}}", "MessageId": "123"}'
```

Prisoner Release (prison-offender-events.prisoner.released)
```
aws sqs send-message \
  --endpoint-url=http://localhost:4566 \
  --queue-url=http://localhost:4566/000000000000/sqs_hmpps_prison_visits_event_queue \
  --message-body \
    '{"Type":"Notification", "Message": "{\"eventType\": \"prison-offender-events.prisoner.released\", \"additionalInformation\": {\"nomsNumber\": \"X8199EJ\",\"reason\": \"SENT_TO_COURT\",\"currentLocation\": \"OUTSIDE_PRISON\", \"prisonId\": \"BMI\", \"nomisMovementReasonCode\": \"CRT\", \"currentPrisonStatus\": \"UNDER_PRISON_CARE\"}}", "MessageId": "123"}'
```

Prisoner Restriction Changed (prison-offender-events.prisoner.restriction.changed)
```
aws sqs send-message \
  --endpoint-url=http://localhost:4566 \
  --queue-url=http://localhost:4566/000000000000/sqs_hmpps_prison_visits_event_queue \
  --message-body \
        '{"Type":"Notification", "Message": "{\"eventType\": \"prison-offender-events.prisoner.restriction.changed\", \"additionalInformation\": {\"nomsNumber\": \"X8199EJ\",\"bookingId\": \"2872889\", \"offenderRestrictionId\": \"557172\", \"restrictionType\": \"CHILD\", \"effectiveDate\": \"2023-09-20\", \"comment\": \"NTC anyone under the age of 18 - pot PPRC\", \"authorisedById\": \"37266\", \"enteredById\": \"1138054\"}}", "MessageId": "123"}'
```

