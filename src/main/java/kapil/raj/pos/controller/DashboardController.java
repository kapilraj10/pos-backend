package kapil.raj.pos.controller;


import kapil.raj.pos.io.DashboardResponse;
import kapil.raj.pos.io.OrderResponse;
import kapil.raj.pos.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private  final OrderService orderService;

    @GetMapping
    public DashboardResponse getDashboardDate() {
        LocalDate today = LocalDate.now();
        Double todaySale = orderService.sumSalesByDate(today);
        Long todayOrderCount = orderService.countByOrderDate(today);
        List<OrderResponse> recentOrders = orderService.findRecentOrders();
        return new DashboardResponse(
                todaySale != null ? todaySale: 0.0,
                todayOrderCount != null ? todayOrderCount :0,
                recentOrders
        );
    }
}
