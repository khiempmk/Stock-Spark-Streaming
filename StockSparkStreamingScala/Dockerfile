FROM bde2020/spark-submit:2.4.5-hadoop2.7

MAINTAINER khiempm1 (khiem.pm170084@sis.hust.edu.vn)

ENV SPARK_APPLICATION_JAR_NAME MasterScala-1.0-SNAPSHOT-jar-with-dependencies
ENV SPARK_APPLICATION_JAR_LOCATION MasterScala-1.0-SNAPSHOT-jar-with-dependencies.jar
ENV SPARK_APPLICATION_MAIN_CLASS com.viettel.vtcc.spark.process.StreamTelegram

ENV SPARK_MASTER_NAME spark-master
ENV SPARK_MASTER_PORT 7077

COPY template.sh /

RUN apk add --no-cache openjdk8 maven\
      && chmod +x /template.sh \
      && mkdir -p /app \
      && mkdir -p /usr/src/app

COPY MasterScala-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/src/app/
COPY conf /usr/src/app/conf
COPY industry_stock_id_broad_band.txt /usr/src/app/


CMD ["/bin/bash", "/template.sh"]