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
