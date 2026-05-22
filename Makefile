include .env

migrate:
	mvn flyway:migrate -Dflyway.user=$(DB_USERNAME) -Dflyway.password=$(DB_PASSWORD)

migrate-info:
	mvn flyway:info -Dflyway.user=$(DB_USERNAME) -Dflyway.password=$(DB_PASSWORD)

run:
	mvn spring-boot:run

test:
	mvn test

compile:
	mvn compile
