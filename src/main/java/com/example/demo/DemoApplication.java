package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        new Thread(DemoApplication::extracted).start();
    }

    private static void extracted() {
        // iphone
        String url = "https://www.apple.com.cn/shop/pickup-message-recommendations?mts.0=regular&mts.1=compact&searchNearby=true&store=R683&product=MU2Q3CH/A";
        // watch
//        String url = "https://www.apple.com.cn/shop/fulfillment-messages?searchNearby=true&parts.0=MQFG3CH/A&option.0=MQG03CH/A,MQEP3FE/A&store=R581";
        String webhooks = "https://open.feishu.cn/open-apis/bot/v2/hook/c9e3dd52-57af-4ae8-9f4c-da737894c212";

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] preBytes = new byte[0];
        while (true) {
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder().uri(new URI(url))
                        .GET().timeout(Duration.ofSeconds(5)).build();
            } catch (URISyntaxException e) {
                continue;
            }
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                continue;
            }
            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]" + response.body());
            Map<String, Object> recommend;
            try {
                recommend = objectMapper.readValue(response.body(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                continue;
            }


            List<Map<String, Object>> res = new ArrayList<>();

            ((List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) recommend.get("body")).get("PickupMessage")).get("stores"))
                    .stream()
                    .filter(x -> !CollectionUtils.isEmpty(x))
                    .map(x -> (Map<String, Object>) x.get("partsAvailability"))
                    .filter(x -> !CollectionUtils.isEmpty(x))
                    .map(Map::values)
                    .flatMap(x -> x.stream().map(z -> (Map<String, Object>) z).filter(z -> Objects.nonNull(z.get("partNumber")) && z.get("partNumber").toString().startsWith("MU")).map(y -> (Map<String, Object>) ((Map<String, Object>) y.get("messageTypes")).get("regular")))
                    .forEach(x -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("时间", x.get("storePickupQuote"));
                        map.put("型号", x.get("storePickupProductTitle"));
                        res.add(map);
                    });


            if (!CollectionUtils.isEmpty(res)) {
                System.out.println("======找到啦" + res + "找到啦======");
                Feishu content = new Feishu("text", new Content(res.toString()));
                byte[] bytes;
                try {
                    bytes = objectMapper.writeValueAsBytes(content);
                } catch (JsonProcessingException e) {
                    continue;
                }
                if (Arrays.equals(preBytes, bytes)) {
                    continue;
                }
                preBytes = bytes;
                try {
                    request = HttpRequest.newBuilder().uri(new URI(webhooks)).POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
                } catch (URISyntaxException e) {
                    continue;
                }
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (IOException | InterruptedException e) {
                    continue;
                }
                System.out.println("Feishu response" + response);
            }
            try {
                Thread.sleep(500 + new Random().nextInt(200));
            } catch (InterruptedException e) {
                continue;
            }
        }
    }

    static class Content {
        String text;

        public Content(String body) {
            this.text = body;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    static class Feishu {
        String msg_type;
        Content content;

        public Feishu(String msg_type, Content content) {
            this.msg_type = msg_type;
            this.content = content;
        }

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getMsg_type() {
            return msg_type;
        }

        public void setMsg_type(String msg_type) {
            this.msg_type = msg_type;
        }
    }
}
