FROM eclipse-temurin:21 as builder
# First stage : Extract the layers
WORKDIR /@project.name@

#COPY mvnw .
#COPY .mvn .mvn
#COPY pom.xml .
#COPY src src
#RUN chmod +x mvnw
#RUN ./mvnw clean package -DskipTests

WORKDIR /@project.name@

ADD ./ /@project.name@

ARG JAR_FILE=*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-jammy as final
# Cria o usuário e grupo spring
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Instala tzdata para gerenciar timezones
RUN apt-get update && apt-get install -y tzdata

# Define o timezone desejado enquanto ainda é root
RUN ln -snf /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && echo "America/Sao_Paulo" > /etc/timezone

# Cria o diretório de trabalho e atribui permissões ao usuário criado
WORKDIR /@project.name@
RUN chown -R spring:spring /@project.name@

# Altera o usuário para o usuário não root
USER spring:spring

## Second stage : Copy the extracted layers
COPY --from=builder @project.name@/dependencies/ ./
COPY --from=builder @project.name@/spring-boot-loader/ ./
COPY --from=builder @project.name@/snapshot-dependencies/ ./
COPY --from=builder @project.name@/application/ ./
COPY --from=builder @project.name@/target/*.jar app.jar

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=""
ENV LOKI_URL="loki"

EXPOSE 8091

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]