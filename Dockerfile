# Stage 1: Build stage
FROM llsilvas/java21-maven-otel:2.16.0 as builder
WORKDIR /app

## Copia o pom.xml e baixa as dependências
#COPY pom.xml .
#RUN mvn dependency:go-offline -B

# Copia o código-fonte
#COPY src ./src
## Compila o projeto e gera o JAR
#RUN mvn clean package -DskipTests
#RUN ls -la

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

#COPY /opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

RUN ls -la
# Extrai as camadas usando o novo modo tools
#RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher && \
#    mv /app/@project.name@/* /app/

RUN java -Djarmode=tools -jar app.jar extract --layers --launcher && \
    mv /app/app/* /app/

RUN ls -la /app

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-jammy as runtime
WORKDIR /app

#COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/app.jar /app/app.jar
COPY --from=builder /opt/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# Copia as camadas extraídas
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# Define variáveis de ambiente
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV LOKI_URL=${LOKI_URL}
ENV KEYCLOAK_AUTH_SERVER_URL=${KEYCLOAK_AUTH_SERVER_URL}
ENV KEYCLOAK_URL=${KEYCLOAK_URL}
ENV KEYCLOAK_JWK_SET_URI=${KEYCLOAK_JWK_SET_URI}
ENV KEYCLOAK_ISSUER_URI=${KEYCLOAK_ISSUER_URI}
ENV SPRING_CONFIG_SERVER=${SPRING_CONFIG_SERVER}
ENV EVENT_SERVICE_URL=${EVENT_SERVICE_URL}

# Exposição da porta da aplicação
EXPOSE 8091

# Inicia a aplicação com o launcher do Spring Boot
#CMD ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/myapp.jar"]
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar","-jar", "app.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
