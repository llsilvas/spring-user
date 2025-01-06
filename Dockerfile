# Stage 1: Build stage
FROM maven:3.9.5-eclipse-temurin-21 as builder
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
COPY --from=builder /app/*.jar app.jar

# Copia as camadas extraídas
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# Define variáveis de ambiente
ENV SPRING_PROFILES_ACTIVE=""
ENV LOKI_URL="loki"
ENV KEYCLOAK_URL=${KEYCLOAK_URL}

# Exposição da porta da aplicação
EXPOSE 8091

# Inicia a aplicação com o launcher do Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
