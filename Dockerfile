FROM alpine:latest

RUN ["apk", "update", "-q"]
RUN ["apk", "upgrade", "-q"]
RUN ["apk", "add", "openjdk8", "-q"]
RUN ["apk", "add", "maven", "-q"]

WORKDIR /app
COPY . /app/

RUN ["mvn", "clean", "package", "-DskipTests=true", "-q"]

ENTRYPOINT ["java", "-jar", "target/p2p-sudoku-1.0.jar"]
CMD ["-ma", "127.0.0.1", "-mp", "4001", "-lp", "4001"]