FROM openjdk:8-jdk

MAINTAINER khiempm1 (khiem.pm170084@sis.hust.edu.vn)

COPY MasterProject-1.0-SNAPSHOT.jar-1.0-SNAPSHOT.jar /usr/local/service/
COPY conf /usr/local/service/
COPY data /usr/local/service/

COPY lib /usr/local/service/



WORKDIR /usr/local/service/

CMD ["java", "-cp", "lib/*:MasterProject-1.0-SNAPSHOT.jar-1.0-SNAPSHOT.jar", "com.viettel.vtcc.master.data.report.bigdata.process.ProcessDataPublisher"]