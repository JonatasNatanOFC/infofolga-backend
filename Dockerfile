FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependências em camada própria: só são baixadas de novo quando o pom.xml muda
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Datas de negócio ("hoje", "últimos 30 dias") são calculadas no fuso do servidor
ENV TZ=America/Sao_Paulo

RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /app/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
