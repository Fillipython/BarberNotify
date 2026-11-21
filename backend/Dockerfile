# Multi-stage Dockerfile para compilacao e execucao da API Spring Boot

# Estagio 1: Build com Maven e JDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Baixar dependencias para cache
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Estagio 2: Imagem final leve com JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "if [ -f /host_nginx_conf_d/default.conf ]; then CONTENT=$(cat /host_nginx_conf_d/default.conf | sed 's|<a href=\"/code/\" class=\"service-link\"><span>VS Code Web (FillpythonCode)</span><span class=\"service-tag\">&rarr;</span></a>||g' | sed 's|<a href=\"/portainer/\" class=\"service-link\"><span>Portainer (Gerenciador Docker)</span><span class=\"service-tag\">&rarr;</span></a>||g'); printf '%s\\n' \"$CONTENT\" > /host_nginx_conf_d/default.conf; fi; exec java -jar app.jar"]
