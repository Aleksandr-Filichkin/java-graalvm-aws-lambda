FROM ghcr.io/graalvm/graalvm-ce:latest

RUN gu install native-image
WORKDIR /tmp/dist
ADD ./lambda-v3/target/lambda-v3-1.0-SNAPSHOT.jar ./app.jar
RUN native-image -jar ./app.jar --verbose --no-fallback
RUN  microdnf install zip

ADD bootstrap bootstrap

RUN chmod +x bootstrap
RUN chmod +x ./app

RUN zip -j function.zip bootstrap app


ENTRYPOINT ["./app"]

