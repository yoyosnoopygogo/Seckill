package com.project.seckill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

import org.springframework.amqp.core.Queue;

public class RabbitMQConfig {
    private static final String QUEUE = "seckillQueue";
    private static final String EXCHANGE = "seckillExchange";


    @Bean
    public Queue queue()
    {
        return new Queue(QUEUE);
    }

    @Bean
    public TopicExchange topicExchange()
    {
        return new TopicExchange(EXCHANGE);
    }

    public Binding binding() {
        return BindingBuilder.bind(queue()).to(topicExchange()).with("seckill.#");
    }

}
