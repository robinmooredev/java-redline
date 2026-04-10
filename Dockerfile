FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x ./mvnw
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "if [ -n \"$ASPOSE_LICENSE_B64\" ]; then echo \"$ASPOSE_LICENSE_B64\" | base64 -d > /app/Aspose.WordsforJava.lic; fi && java -jar app.jar"]
