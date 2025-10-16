package com.baskaaleksander.smartdocflowbackend.common.config;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.JobStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.util.UUID;

@Configuration
public class QueueConfig {

    public static final String EXCHANGE = "app.exchange";
    public static final String OCR_QUEUE = "tasks.ocr";
    public static final String OCR_ROUTING_KEY = "tasks.ocr";
    public static final String EMBED_QUEUE = "tasks.embed";
    public static final String EMBED_ROUTING_KEY = "tasks.embed";
    public static final String CREDENTIALS_EMAIL_QUEUE = "tasks.credentials-email";
    public static final String CREDENTIALS_EMAIL_ROUTING_KEY = "tasks.credentials-email";
    public static final String PASSWORD_RESET_EMAIL_QUEUE = "tasks.password-reset";
    public static final String PASSWORD_RESET_EMAIL_ROUTING_KEY = "tasks.password-reset";


    public static final String OCR_DLQ = "tasks.ocr.dlq";
    public static final String EMBED_DLQ = "tasks.embed.dlq";
    public static final String CREDENTIALS_EMAIL_DLQ = "tasks.credentials-email.dlq";
    public static final String PASSWORD_RESET_EMAIL_DLQ = "tasks.password-reset.dlq";
    public static final String DLX = "app.exchange.dlx";
    public static final String OCR_DLK = "tasks.ocr.dlq";
    public static final String EMBED_DLK = "tasks.embed.dlq";
    public static final String CREDENTIALS_EMAIL_DLK = "tasks.credentials-email.dlq";
    public static final String PASSWORD_RESET_EMAIL_DLK = "tasks.password-reset.dlq";

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
    public Queue credentialsEmailQueue() {
        return QueueBuilder
                .durable(CREDENTIALS_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", CREDENTIALS_EMAIL_DLK)
                .build();
    }

    @Bean
    public Queue passwordResetEmailQueue() {
        return QueueBuilder
                .durable(PASSWORD_RESET_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", PASSWORD_RESET_EMAIL_DLK)
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
    public Queue credentialsEmailDlq() {
        return QueueBuilder.durable(CREDENTIALS_EMAIL_DLQ).build();
    }

    @Bean
    public Queue passwordResetEmailDlq() {
        return QueueBuilder.durable(PASSWORD_RESET_EMAIL_DLQ).build();
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
    public Binding credentialsEmailBinding() {
        return BindingBuilder.bind(credentialsEmailQueue()).to(exchange()).with(CREDENTIALS_EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding passwordResetEmailBinding() {
        return BindingBuilder.bind(passwordResetEmailQueue()).to(exchange()).with(PASSWORD_RESET_EMAIL_ROUTING_KEY);
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
    public Binding credentialsEmailDlqBinding() {
        return BindingBuilder.bind(credentialsEmailDlq()).to(deadLetterExchange()).with(CREDENTIALS_EMAIL_DLK);
    }

    @Bean
    public Binding passwordResetEmailDlqBinding() {
        return BindingBuilder.bind(passwordResetEmailDlq()).to(deadLetterExchange()).with(PASSWORD_RESET_EMAIL_DLK);
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter mc, RabbitTemplate template, JobStatusService js
    ) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(mc);
        factory.setDefaultRequeueRejected(false);

        var retry = new RetryTemplateBuilder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2.0, 5000)
                .build();

        MessageRecoverer recoverer = (message, cause) -> {
            var props = message.getMessageProperties();
            var rk = props.getReceivedRoutingKey();
            var headers = props.getHeaders();
            var docId = (String) headers.getOrDefault("documentId", null);

            if (docId != null) {
                if ("tasks.ocr".equals(rk)) {
                    js.markFailedOcr(UUID.fromString(docId));
                } else if ("tasks.embed".equals(rk)) {
                    js.markFailedEmbed(UUID.fromString(docId));
                }
            }

            new RepublishMessageRecoverer(template, QueueConfig.DLX, null).recover(message, cause);
        };

        var advice = RetryInterceptorBuilder.stateless()
                .retryOperations(retry)
                .recoverer(recoverer)
                .build();

        factory.setAdviceChain(advice);

        return factory;
    }
}
