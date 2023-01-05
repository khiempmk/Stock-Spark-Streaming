package com.viettel.vtcc.spark.process

import java.text.SimpleDateFormat
import java.util.Date

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.viettel.vtcc.spark.model.{ElasticObject, ForumObject, StorageCount, StorageStock}
import javax.management.timer.Timer
import lombok.extern.slf4j.Slf4j
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.{SparkConf, SparkFiles}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import org.elasticsearch.spark.sparkRDDFunctions

import scala.io.Source

@Slf4j
object StreamForum extends Serializable {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop-common-2.2.0-bin-master\\")
    val bootstrapServer = "kafka-broker-1:29092,kafka-broker-2:29092"
    val groupId = "forum_data"
    val topic = "forum_data"
    val sparkAppName = "ForumStream"
    val esIndexAutoCreate = "true"
    val esNodes  = "elasticsearch:9200"

    // load kafka params
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> bootstrapServer,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> groupId,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    // create streaming context
    val conf = new SparkConf()
      .setAppName(sparkAppName)
      //      .setMaster(sparkMaster)
      .set("es.index.auto.create", esIndexAutoCreate)
      .set("es.nodes", esNodes)
      .set("es.nodes.wan.only", "true")

    val ssc = new StreamingContext(conf, Seconds(1))

    // load map stock and broad name
    var stock_map: Map[String, String] = Map()
    var stockFile = SparkFiles.get("industry_stock_id_broad_band.txt")
    // create map stock id and broad name
    val source = Source.fromFile(stockFile)
    for (line <- source.getLines()) {
      val first_value = line.split("\t")(0)
      val second_value = line.split("\t")(1)
      stock_map += (first_value -> second_value)
    }
    println("Load map stock id and broad name " + stock_map.size)
    source.close()


    val topics = Array(topic)
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    // print data to logs

    stream.map(record => (record.key, record.value)).map(record => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      mapper.readValue(record._2, classOf[ForumObject])
    }
    )
      .map(forum_object => convertObject(forum_object))
      .print()

    // write data to ES

    stream.map(record => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      mapper.readValue(record.value(), classOf[ForumObject])
    })
      .map(forum_object => convertObject(forum_object))
      .map(telegram => addCurrentTimeToObject(telegram)).foreachRDD(rdd => {
      rdd.saveToEs("raw_data/data")
    })


    // compute number process message contain stock_id

    stream.map(record => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      mapper.readValue(record.value, classOf[ForumObject])
    })
      .map(forum_object => convertObject(forum_object))
      .map(telegram_object => (telegram_object.id, telegram_object.text.split(" ")))
      .flatMap { case (key, values) => values.map((key, _)) }
      .filter(pair => stock_map.contains(pair._2))
      .reduceByKey((x, _) => x).count().map(count => addCurrentTime(count))
      .foreachRDD(rdd => rdd.saveToEs("msg_stock/data"))

    //compute each stock exist in process

    stream.map(record => {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      mapper.readValue(record.value(), classOf[ForumObject])
        .content
    })
      .flatMap(text => text.split(" "))
      .filter(text => stock_map.contains(text))
      .map(word => (word, 1)).reduceByKey(_ + _)
      .map(count => addCurrentTimeStock(count._1, count._2)).foreachRDD(rdd => {
      rdd.saveToEs("detail_stock/data")
    })

    ssc.start()
    ssc.awaitTermination()
  }

  def convertObject(x: ForumObject): ElasticObject = {
    val format_time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val current_date = format_time.format(new Date(System.currentTimeMillis() - 7 * Timer.ONE_HOUR))
    ElasticObject(id = x.id, date = current_date, text = x.content, from = x.title)
  }

  def addCurrentTimeToObject(x: ElasticObject): ElasticObject = {
    val format_time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val current_date = format_time.format(new Date(System.currentTimeMillis() - 7 * Timer.ONE_HOUR))
    ElasticObject(id = x.id, date = current_date, text = x.text, from = x.from)
  }

  def addCurrentTime(x: Long): StorageCount = {
    val format_time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val current_date = format_time.format(new Date(System.currentTimeMillis() - 7 * Timer.ONE_HOUR))
    StorageCount(x, current_date)
  }

  def addCurrentTimeStock(y: String, x: Long): StorageStock = {
    val format_time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val current_date = format_time.format(new Date(System.currentTimeMillis() - 7 * Timer.ONE_HOUR))
    StorageStock(stock = y, date = current_date, count = x)
  }

}
