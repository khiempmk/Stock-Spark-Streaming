package com.viettel.vtcc.master.data.report.bigdata.es;

import com.viettel.vtcc.master.data.utils.ConfigurationLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;

@Slf4j
public class EsUtils {
    private static RestHighLevelClient highLevelClient;
    private static String ES_INDEX = ConfigurationLoader.getInstance().getAsString("es.index", "elasticsearch");
    private static Integer ES_PORT = ConfigurationLoader.getInstance().getAsInteger("es.port", 9200);
    public static void initClient() {
        try {
            highLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(ES_INDEX, ES_PORT, "http")));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void close() {
        try {
            highLevelClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void indexDocument(String data, String index) {
        try {
            IndexRequest request = new IndexRequest(index);
            request.source(data, XContentType.JSON);
            IndexResponse indexResponse = highLevelClient.index(request, RequestOptions.DEFAULT);
            log.info("result index {}", indexResponse.getResult().getLowercase());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


}
