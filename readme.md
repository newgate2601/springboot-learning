# GATEWAY CONFIG

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
        <version>4.1.5</version>
    </dependency>

+ Nó bao gồm các library khi ta add vào như: spring-boot-starter-webflux + spring-cloud-starter + spring-cloud-gateway-server + spring-cloud-starter-loadbalancer

## _spring.cloud.gateway.discovery_
https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/the-discoveryclient-route-definition-locator.html
+ Sử dụng để config gateway dựa vào các services mà registered với Service Registry
+ Config thông qua class DiscoveryLocatorProperties


    @ConfigurationProperties("spring.cloud.gateway.discovery.locator")
    public class DiscoveryLocatorProperties {
        // cho phép gateway tích hợp với DiscoveryClient
        private boolean enabled = false;
        
        // 
    }


## _spring.cloud.gateway.routes_
### _ok_

## _spring.cloud.gateway.httpclient_
### _ok_

## _spring.cloud.gateway.enabled_
### _ok_