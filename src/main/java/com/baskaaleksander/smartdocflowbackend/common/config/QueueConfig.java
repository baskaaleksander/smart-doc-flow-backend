package com.baskaaleksander.smartdocflowbackend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    public static final String EXCHANGE = "app.exchange";
    public static final String OCR_QUEUE = "tasks.ocr";
    public static final String OCR_ROUTING_KEY = "tasks.ocr";
    public static final String EMBED_QUEUE = "tasks.embed";
    public static final String EMBED_ROUTING_KEY = "tasks.embed";

    public static final String OCR_DLQ = "tasks.ocr.dlq";
    public static final String EMBED_DLQ = "tasks.embed.dlq";
    public static final String DLX = "app.exchange.dlx";
    public static final String OCR_DLK = "tasks.ocr.dlq";
    public static final String EMBED_DLK = "tasks.embed.dlq";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue ocrQueue() {
        return QueueBuilder.durable(OCR_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", OCR_DLK)
                .build();
    }

    @Bean
    public Queue embedQueue() {
        return QueueBuilder.durable(EMBED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", EMBED_DLK)
                .build();
    }

    @Bean
    public Queue ocrDlq() {
        return QueueBuilder.durable(OCR_DLQ).build();
    }

    @Bean
    public Queue embedDlq() {
        return QueueBuilder.durable(EMBED_DLQ).build();
    }

    @Bean
    public Binding ocrBinding() {
        return BindingBuilder.bind(ocrQueue()).to(exchange()).with(OCR_ROUTING_KEY);
    }

    @Bean
    public Binding embedBinding() {
        return BindingBuilder.bind(embedQueue()).to(exchange()).with(EMBED_ROUTING_KEY);
    }

    @Bean
    public Binding ocrDlqBinding() {
        return BindingBuilder.bind(ocrDlq()).to(deadLetterExchange()).with(OCR_DLK);
    }

    @Bean
    public Binding embedDlqBinding() {
        return BindingBuilder.bind(embedDlq()).to(deadLetterExchange()).with(EMBED_DLK);
    }

    @Bean
    public MessageConverter jackson2MessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter conv) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(conv);
        return tpl;
    }
}
