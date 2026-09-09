# TỔNG HỢP BÀI TẬP VỀ API GATEWAY & LOAD BALANCING (DỰ ÁN ORDERS-SERVICE)

---

# BÀI TẬP 1: CẤU HÌNH API GATEWAY & LOAD BALANCING

## 1. Phân tích lỗi
- **Dòng cấu hình sai**: `uri: http://orders-service`
- **Nguyên nhân**: Tiền tố `http://` khiến Gateway hiểu `orders-service` là một tên miền tĩnh ngoài Internet (DNS Lookup) nên ném ra lỗi `UnknownHostException`.
- **Cách khắc phục**: Đổi thành `uri: lb://orders-service`. Tiền tố `lb://` kích hoạt bộ lọc LoadBalancer tra cứu danh bạ Eureka để lấy IP/Port thực tế.

## 2. File cấu hình chuẩn (`api-gateway/src/main/resources/application.yml`)
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      routes:
        - id: orders-service-route
          uri: lb://orders-service
          predicates:
            - Path=/api/orders/**

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

# BÀI TẬP 2: LỖI 503 SERVICE UNAVAILABLE (KHÔNG KHỚP SERVICE ID)

## 1. Nguyên nhân & Thuật ngữ kỹ thuật
- `spring.application.name`: Xác định Service ID phía service con gửi đăng ký tới Eureka.
- `uri: lb://<Service-ID>`: Gateway sử dụng `ReactiveLoadBalancerClientFilter` gọi Spring Cloud LoadBalancer truy vấn Eureka danh sách `ServiceInstance` khả dụng.
- **Lỗi 503**: Khi tên đăng ký (`orders-service`) và tên tra cứu không trùng khớp 100% từng ký tự, LoadBalancer nhận về danh sách instance rỗng $\rightarrow$ Ném ngoại lệ `NotFoundException` và phản hồi mã lỗi **`503 Service Unavailable`**.

## 2. Sửa lỗi triệt để
- Phía `orders-service`: `spring.application.name: orders-service`
- Phía `api-gateway`: `uri: lb://orders-service`

---

# BÀI TẬP 3: CẤU HÌNH RANDOM LOAD BALANCER TÙY BIẾN CHO ORDERS-SERVICE

## 1. Khai báo Class Cấu hình Ngẫu nhiên (`RandomLoadBalancerConfig.java`)
Tạo file [RandomLoadBalancerConfig.java](file:///d:/Microsevices%20In%20Action/VietMart-Microservice-SS05/api-gateway/src/main/java/microservice/apigateway/config/RandomLoadBalancerConfig.java):

```java
package microservice.apigateway.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class RandomLoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                name
        );
    }
}
```

## 2. Kích hoạt Cấu hình Ngẫu nhiên riêng cho `orders-service`
Cập nhật [ApiGatewayApplication.java](file:///d:/Microsevices%20In%20Action/VietMart-Microservice-SS05/api-gateway/src/main/java/microservice/apigateway/ApiGatewayApplication.java):

```java
package microservice.apigateway;

import microservice.apigateway.config.RandomLoadBalancerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;

@SpringBootApplication
@EnableDiscoveryClient
@LoadBalancerClient(name = "orders-service", configuration = RandomLoadBalancerConfig.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
```

## 3. Giải thích Kỹ thuật
- Nhờ annotation `@LoadBalancerClient(name = "orders-service", configuration = RandomLoadBalancerConfig.class)`, chiến lược ngẫu nhiên **`RandomLoadBalancer`** sẽ **chỉ áp dụng duy nhất cho `orders-service`**.
- Các service khác trong hệ thống nếu có phát triển về sau sẽ vẫn tiếp tục dùng thuật toán mặc định **`RoundRobinLoadBalancer`**.