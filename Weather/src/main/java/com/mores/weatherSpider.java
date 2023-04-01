package com.mores;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import snw.jkook.command.JKookCommand;
import snw.jkook.message.component.card.CardBuilder;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.HeaderModule;
import snw.jkook.message.component.card.module.SectionModule;
import snw.jkook.plugin.BasePlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class weatherSpider extends BasePlugin {

    @Override
    public void onEnable() {
        getLogger().info("天气插件已成功加载");

        new JKookCommand("weather")
                .addOptionalArgument(String.class,"None")
                .executesUser((sender,arguments,message)->{
                    String senderName=sender.getName();
                    if (arguments.length>=1&arguments[0]=="None"){
                        message.reply(senderName+"你想要问我哪个城市的天气呢？");
                    }else {
                        String address= (String) arguments[0];
                        String cityCode=findCityCode(address);
                        if (cityCode==null){
                            message.reply("没有获取到"+arguments[0]+"的天气信息，请检查该城市名是否正确以及是否在大陆内");
                            return;
                        }
                        String url = "http://t.weather.sojson.com/api/weather/city/" + cityCode;
                        String jsonResponse;
                        try {
                            jsonResponse =sendGetRequest(url);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        JsonNode root;
                        try {
                            root = parseJsonResponse(jsonResponse);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        JsonNode forecastNode=root.path("data").path("forecast").get(0);
                            String date = forecastNode.path("ymd").asText();//日期
                            String week = forecastNode.path("week").asText();//星期几
                            String highTemperature = forecastNode.path("high").asText();//最高温度
                            String lowTemperature = forecastNode.path("low").asText();//最低温度
                            String weatherType = forecastNode.path("type").asText();//天气
                            String fx = forecastNode.path("fx").asText();//风向
                            String fl = forecastNode.path("fl").asText();//风力
                            String notice = forecastNode.path("notice").asText();//提醒

                            MultipleCardComponent cardComponent = new CardBuilder()
                                    .setTheme(Theme.PRIMARY)
                                    .setSize(Size.LG)
                                    .addModule(new HeaderModule(new PlainTextElement((String) arguments[0], false)))
                                    .addModule(new SectionModule(new PlainTextElement(date), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(week), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(highTemperature), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(lowTemperature), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(weatherType), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(fx), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(fl), null, null))
                                    .addModule(new SectionModule(new PlainTextElement(notice), null, null))
                                    .build();
                            message.reply(cardComponent);
                        }
                }).register(this);
    }

    public String findCityCode(String cityName){
        String CityCode = null;
        try {
            //读取json文件
            InputStream inputStream = getClass().getResourceAsStream("/cityCode.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonContent = builder.toString();
            //解析json
            JSONObject jsonObject= JSON.parseObject(jsonContent);
            JSONArray cityArray=jsonObject.getJSONArray("cities");
            for(int i=0;i<cityArray.size();i++){
                JSONObject city=cityArray.getJSONObject(i);
                if (city.getString("name").equals(cityName)){
                   CityCode=city.getString("code");
                   break;
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return CityCode;
    }

    private static String sendGetRequest(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try (InputStream inputStream = connection.getInputStream();
             Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static JsonNode parseJsonResponse(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonResponse);
    }
}
