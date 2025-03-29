package com.example.learning.responsehandle.message;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

    // support managing get message from message.properties
    // pick message from specific message file based on Locale
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // search messages.properties
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");

        // default false: return key if not have message of this key.
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
