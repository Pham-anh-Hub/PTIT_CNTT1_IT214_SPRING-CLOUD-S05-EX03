package microservice.ordersservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Value("${server.port:8082}")
    private String port;

    @GetMapping
    public String getOrders() {
        System.out.println(">>> [ORDERS-SERVICE] Yêu cầu tới cổng: " + port);
        return "Phản hồi Order Service từ cổng: " + port;
    }

    @GetMapping("/{id}")
    public String getOrdersById(@PathVariable String id) {
        System.out.println(">>> [ORDERS-SERVICE] Yêu cầu lấy orders theo id tới cổng: " + port);
        return "Phản hồi Order Service (lấy orders theo id) từ cổng: " + port;
    }
}
