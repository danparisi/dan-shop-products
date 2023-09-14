package com.danshop.products;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
class KafkaSenderService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    void sendMessage(String message, String topicName) throws ExecutionException, InterruptedException {
        SendResult<String, String> resul = kafkaTemplate.send(topicName, message).get();
        log.info("Kafka result: " + resul);
    }

//    @Scheduled(fixedDelayString = "PT10S", initialDelay = 0)
//    public void test() throws ExecutionException, InterruptedException {
//        log.info("Sending message to kafka");
//        sendMessage(now() + ": kafka test", "dan-service-logs");
//    }

}
