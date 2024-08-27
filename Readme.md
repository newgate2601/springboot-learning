https://docs.spring.io/spring-kafka/api/org/springframework/kafka/core/package-summary.html

https://docs.spring.io/spring-kafka/reference/kafka/sending-messages.html

# **Configuring Topics**
### _NewTopic_

    @Bean
    public NewTopic desNewTopic(){
        return TopicBuilder.name("my-topic")
                .build();
    }

Sử dụng _NewTopic_ để add 1 Topic vào Broker + 
nếu Topic này đã tồn tại rồi thì thôi không add vào nữa

### _NewTopic_
    @Bean
    public KafkaAdmin.NewTopics desNewTopics(){
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name("kaka")
                        .build(),
                TopicBuilder.name("kaka1")
                        .build()
        );
    }
Sử dụng _NewTopics_ để add multi-topic vào Broker 

Với Springboot, KafkaAdmin bean sẽ automatic registered 
nên ta chỉ cần khai báo @Bean cho 2 Class trên là được

# **Sending Messages**
### _KafkaTemplate_
KafkaTemplate sử dụng để wraps Producer và cung cấp method để send message tới Kafka Topics

Với các method trong KafkaTemplate có param là Message<?> 
thì có thể define thêm thông tin cho message như: topic + partition + key + timestamp

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate kafkaTemplate = new KafkaTemplate<>(producerFactory());
        kafkaTemplate.setProducerListener(producerResultProxy);
        return kafkaTemplate;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

### _ProducerListener_
https://docs.spring.io/spring-kafka/api/org/springframework/kafka/support/ProducerListener.html

ProducerListener sử dụng để handle việc success, fail khi send message,
việc cần làm là ta phải implement lại Interface nàu vào @Override 2 method của nó

    public class ProducerResultProxy<K, V> implements ProducerListener<K, V> {
        @Override
            public void onSuccess(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata) {
            System.out.println("Oke");
            ProducerListener.super.onSuccess(producerRecord, recordMetadata);
        }

        @Override
        public void onError(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata, Exception exception) {
            System.out.println("Fail");
            ProducerListener.super.onError(producerRecord, recordMetadata, exception);
        }
    }

Sau đó ta sẽ khai báo bean dùng với khi send message type gì 
để mỗi khi thực hiện xong message sẽ handle như thế nào

    @AllArgsConstructor
    @Configuration
    public class ProducerResultProxyConfig {
        @Bean
        public ProducerResultProxy<String, String> producerListener() {
            return new ProducerResultProxy<>();
        }
    }

# **Receiving Messages**
### _@KafkaListener annotation_
https://docs.spring.io/spring-kafka/api/org/springframework/kafka/annotation/KafkaListener.html

@KafkaListener annotation sử dụng để chỉ định 1 method 
để handle việc lắng nghe events từ Topics cụ thể

Annotation này có 1 attribute là containerFactory() sử dụng để chỉ định 1 _KafkaListenerContainerFactory_ 
để build Kafka listener container + nếu mà ko set thì nó sẽ pick 1 Bean _KafkaListenerContainerFactory_ 
đã có sẵn

    @Component
    public class MessageConsumer {
        @KafkaListener(topics = "my-topic", groupId = "my-group-id")
        public void listen(String message) {
        System.out.println("Received message: " + message);
        }
    }

### _@KafkaListenerContainerFactory_
Listener Container Factory sử dụng để tạo ra, quản lí các container để handle listen events trong Kafka + các container này 
có nhiệm vụ quản lí vòng đời của các listener gồm việc đăng kí vơi Kafka, quản lí quá trình chúng handle events

Tuy nhiên để sử dụng tính năng này, ta cần config 1 Listener container factory dưới dạng 
1 bean của _KafkaListenerContainerFactory_, thông thường biểu diễn bean này dưới dạng 1 
triển khai khác của nó là _ConcurrentKafkaListenerContainerFactory_ giúp tạo ra các container chạy đồng thời để handle
(_@KafkaListener_ sẽ pick Bean này nếu ko define containerFactory())

### _ConsumerFactory_
_ConsumerFactory_ sử dụng để define strategy để tạo ra 1 Consumer instance để đọc events từ các Topic trong Kafka + 
Trong đó có 1 triển khai cho Interface này (có thể nói đây là triển khai mặc định) 
là _DefaultKafkaConsumerFactory_, để tạo 1 Consumer Factory mà cung cấp 1 Map configuration

ConsumerFactory<K, V> thì trong đó K là key datatype, V là value datatype của event mà consumer sẽ xử lí 



    @Configuration
    public class KafkaConsumerConfig {
        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory());
            return factory;
        }

        @Bean
        public ConsumerFactory<String, String> consumerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // thiết lập địa chỉ của Kafka Broker mà Consumer sẽ connect để lắng nghe events
            configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group-id");
            configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // sử dụng để Deserialize key của message thành String
            configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // sử dụng để Deserialize value của message thành String
        return new DefaultKafkaConsumerFactory<>(configProps);
        }
    }