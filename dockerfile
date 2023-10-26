FROM openjdk:17
COPY . /app
WORKDIR /app/target
CMD ["java","-jar", "ShortUrl-0.0.1-SNAPSHOT.jar"]