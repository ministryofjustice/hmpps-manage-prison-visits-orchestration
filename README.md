# HMPPS Manage Prison Visits Orchestration API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-prison-visits-orchestration/tree/main.svg?style=shield)](https://app.circleci.com/pipelines/github/ministryofjustice/visit-scheduler)

This is a Spring Boot application, written in Kotlin, used as an orchestration layer between the Visit Someone In Prison front end and external API calls like visit-scheduler, prison-api, prisoner-search. Used by [Visit Someone in Prison](https://github.com/ministryofjustice/book-a-prison-visit-staff-ui).

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

Create a Spring Boot run configuration with active profile of dev, to run against te development environment.


Alternatively the service can be run using docker-compose with client 'book-a-prison-visit-client' and the usual dev secret.
```
docker-compose up -d
```
or for particular compose files
```
docker-compose -f docker-compose-local.yml up -d
```
Ports

| Service            | Port |  
|--------------------|------|
| visit-scheduler    | 8081 |
| visit-scheduler-db | 5432 |
| hmpps-auth         | 8090 |

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

##### Step 4 - Send a message to the application queue. The below is a non-assciation event for prisoners G0026GC and A8713DY. Replace the prisoner numbers with your test values.
```
aws sqs --endpoint-url=http://localhost:4566 send-message --queue-url=http://localhost:4566/000000000000/sqs_hmpps_prison_visits_event_queue  --message-body '{"Type":"Notification", "Message": "{\"eventType\":\"non-associations.created\",\"additionalInformation\":{\"nsPrisonerNumber2\":\"G0026GC\",\"nsPrisonerNumber1\":\"A8713DY\"}}", "MessageId": "60945f6e-f146-418e-a8e9-1de858d41faa"}'
```

If you are unsure about the queue name you can check the queue names using the following command and replace it in the above --queue-url value parameter
```
aws sqs list-queues --endpoint-url=http://localhost:4566
```
