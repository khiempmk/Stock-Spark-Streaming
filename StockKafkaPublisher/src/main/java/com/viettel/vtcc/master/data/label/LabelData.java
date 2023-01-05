package com.viettel.vtcc.master.data.label;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.viettel.vtcc.master.data.label.model.LabelModel;
import com.viettel.vtcc.master.data.ohlc.StockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class LabelData {

    private static final String FOLDER_TELEGRAM_INPUT = "C:\\Users\\KGM\\Downloads\\stock_telegram\\msg_contain_stock_order";
    private static final String FOLDER_LABEL = "C:\\Users\\KGM\\Downloads\\data_forum\\data_forum\\label_data";
    private static final String FOLDER_FORUM_INPUT = "C:\\Users\\KGM\\Downloads\\data_forum\\data_forum\\forum_data_contain_stock";

    public static void main(String[] args) {
        process_telegram();
        process_forum();
    }

    private static void process_telegram() {
        for (File file : new File(FOLDER_TELEGRAM_INPUT).listFiles()) {
            try {
                FileOutputStream fop = new FileOutputStream(FOLDER_LABEL + "\\" + file.getName(), true);
                List<String> listLines = FileUtils.readLines(file, "UTF-8");
                log.info("process file {}", file.getName());
                listLines.forEach(line -> {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
                        JsonArray list_stock = jsonObject.getAsJsonArray("list_stock");
                        if (list_stock.size() > 0) {
                            list_stock.forEach(jsonElement -> {
                                try {
                                    LabelModel labelModel = new LabelModel();
                                    String stock_id = jsonElement.getAsString();
                                    labelModel.setStock(stock_id);
                                    String text = jsonObject.get("text").getAsString().replaceAll("\n", " ").replaceAll("\t", " ");
                                    labelModel.setText(text);
                                    String date = jsonObject.get("date").getAsString();
                                    labelModel.setDate(date);
                                    labelModel.setType("telegram");
                                    String date_short = date.split(" ")[0];
                                    Triple<List<Float>, Float, List<Float>> triple = StockUtils.getPriceInDay(stock_id, date_short);
                                    labelModel.setChange(triple.getMiddle());
                                    labelModel.setNext_change(triple.getRight().get(0));
                                    labelModel.setThree_day_previous_change(triple.getLeft());
                                    labelModel.setThree_day_following_change(triple.getRight());
                                    fop.write((labelModel + "\n").getBytes(StandardCharsets.UTF_8));
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
                try {
                    fop.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private static void process_forum() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (File file : new File(FOLDER_FORUM_INPUT).listFiles()) {
            try {
                FileOutputStream fop = new FileOutputStream(FOLDER_LABEL + "\\" + file.getName(), true);
                List<String> listLines = FileUtils.readLines(file, "UTF-8");
                log.info("process file {}", file.getName());
                listLines.forEach(line -> {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
                        JsonArray list_stock = jsonObject.getAsJsonArray("list_stock");
                        if (list_stock.size() > 0) {
                            list_stock.forEach(jsonElement -> {
                                try {
                                    LabelModel labelModel = new LabelModel();
                                    String stock_id = jsonElement.getAsString();
                                    labelModel.setStock(stock_id);
                                    String text = jsonObject.get("text").getAsString().replaceAll("\n", " ").replaceAll("\t", " ");
                                    labelModel.setText(text);
                                    String date = jsonObject.get("date").getAsString();
                                    Date date_input = simpleDateFormat.parse(date);
                                    String date_format = outputDateFormat.format(date_input);
                                    labelModel.setDate(date_format);
                                    labelModel.setType("forum");
                                    String date_short = date_format.split(" ")[0];
                                    Triple<List<Float>, Float, List<Float>> triple = StockUtils.getPriceInDay(stock_id, date_short);
                                    labelModel.setChange(triple.getMiddle());
                                    labelModel.setNext_change(triple.getRight().get(0));
                                    labelModel.setThree_day_previous_change(triple.getLeft());
                                    labelModel.setThree_day_following_change(triple.getRight());
                                    fop.write((labelModel + "\n").getBytes(StandardCharsets.UTF_8));
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
                try {
                    fop.close();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
