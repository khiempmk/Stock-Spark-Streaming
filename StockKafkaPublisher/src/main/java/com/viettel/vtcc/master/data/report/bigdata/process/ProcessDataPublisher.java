package com.viettel.vtcc.master.data.report.bigdata.process;

import com.google.gson.JsonObject;
import com.viettel.vtcc.master.data.ohlc.StockUtils;
import com.viettel.vtcc.master.data.report.bigdata.es.EsUtils;
import com.viettel.vtcc.master.data.report.bigdata.kafka.KafkaPublisher;
import com.viettel.vtcc.master.data.utils.ConfigurationLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.management.timer.Timer;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
public class ProcessDataPublisher {

    private static final KafkaPublisher kafkaPublisher = new KafkaPublisher();
    private static final String FOLDER_FORUM = ConfigurationLoader.getInstance().getAsString("folder.forum", "C:\\Users\\KGM\\Downloads\\data_forum\\data_forum\\raw_data\\");
    private static final String FOLDER_TELEGRAM = ConfigurationLoader.getInstance().getAsString("folder.telegram", "C:\\Users\\KGM\\Downloads\\stock_telegram\\order_data_by_month\\");
    private static final String FILE_VNINDEX_30 = "vnindex30.txt";
    private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");
    public static void main(String[] args) {
//         push forum msg to kafka topic forum_data
        Random random = new Random();
        Thread thread = new Thread(() -> {
            while (true){
                for (File file : new File(FOLDER_FORUM).listFiles()) {
                    try {
                        try {
                            List<String> listLines = FileUtils.readLines(file, "UTF-8");
                            for (String line : listLines){
                                kafkaPublisher.push_data(line, "forum_data");
                                Thread.sleep(random.nextInt(40)+80);
                            }
                            listLines.forEach(line -> kafkaPublisher.push_data(line, "forum_data"));
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        });
        thread.start();
        // push telegram msg to kafka topic telegram_data
        new Thread(() -> {
            while (true){
                for (File file : new File(FOLDER_TELEGRAM).listFiles()) {
                    try {
                        try {
                            List<String> listLines = FileUtils.readLines(file, "UTF-8");
                            for (String line : listLines){
                                kafkaPublisher.push_data(line, "telegram_data");
                                Thread.sleep(random.nextInt(30)+120);
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }).start();


        new Thread(()->{
            while (true){
                try {
                    EsUtils.initClient();
                    String index_30 = FileUtils.readFileToString(new File(FILE_VNINDEX_30), "UTF-8");
                    for (String index : index_30.split(",")) {
                        try {
                            String watch_day = date_format.format(new Date());
                            float change = StockUtils.getChangeRealTime(index, watch_day);
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("stock_id", index);
                            jsonObject.addProperty("value", change);
                            jsonObject.addProperty("updated_time", watch_day);
                            String data = jsonObject.toString();
                            log.info("write data to es {} {}", index, change);
                            EsUtils.indexDocument(data, "index_30");
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    Thread.sleep(Timer.ONE_DAY);
                } catch (Exception e){
                    log.error(e.getMessage(),e);
                }
                EsUtils.close();
            }
        }).start();
    }
}
