package jpabook.jpaApi.api;

import jpabook.jpaApi.domain.Address;
import jpabook.jpaApi.domain.Order;
import jpabook.jpaApi.domain.OrderItem;
import jpabook.jpaApi.domain.OrderStatus;
import jpabook.jpaApi.repository.OrderRepository;
import jpabook.jpaApi.repository.OrderSearch;
import jpabook.jpaApi.repository.order.query.OrderFlatDto;
import jpabook.jpaApi.repository.order.query.OrderItemQueryDto;
import jpabook.jpaApi.repository.order.query.OrderQueryDto;
import jpabook.jpaApi.repository.order.query.OrderQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }
    //dto로 반환
    @GetMapping("/api/v2/orders")
    public Result ordersV2(){
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream().map(o -> new OrderDto(o)).collect(Collectors.toList());
        return new Result(collect.size(), collect);
    }

    //fetch join으로 가져오기, 데이터 뻥튀기 됨, 페이징 시 하자 생김
    @GetMapping("/api/v3/orders")
    public Result ordersV3(){
        List<Order> orders = orderRepository.findAllWithItem();
        for (Order order : orders) {
            System.out.println("order = " + order.getId());
        }
        List<OrderDto> collect = orders.stream().map(o -> new OrderDto(o)).collect(Collectors.toList());
        return new Result(collect.size(), collect);
    }

    //페이징 할 수 있게 최적화 fetch join으로 xToOne 관계만 가져오고 OneToMany는 batch_fetch_size로 in절로 한번에
    //hibernate는 order을 가져온 걸 알고 있고 order관련된 쿼리(ex)orderItem)를 날릴 때 batchsize만큼의 orderId로
    //in절 날림, 처음으로 lazy loading 초기화 할 때 in절 쿼리 나감
    @GetMapping("/api/v3.1/orders")
    public Result ordersV3_page(@RequestParam(value = "offset",defaultValue = "0") int offset,
                                @RequestParam(value = "limit",defaultValue = "100") int limit){
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset,limit);
        for (Order order : orders) {
            System.out.println("order = " + order.getId());
        }
        List<OrderDto> collect = orders.stream().map(o -> new OrderDto(o)).collect(Collectors.toList());
        return new Result(collect.size(), collect);
    }

    //dto로 직접 조회, 1+n문제 생김(orderItems 관련 쿼리 두번)
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
       return orderQueryRepository.findOrderQueryDtos();
    }

    //dto로 직접 조회 최적화, 1+n해소, 그러나 코드가 매우 길어짐
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> orderV5(){
        return orderQueryRepository.findAllByDto_optimization();
    }

    //쿼리가 1개 나가지만 데이터 뻥튀기 + order로 페이징 시 하자 생김
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> orderV6(){
        //return orderQueryRepository.findAllByDto_flat();

        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        log.info("flats={}",flats);
        Collector<OrderFlatDto, ?, List<OrderItemQueryDto>> mapping = Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                o.getItemName(), o.getOrderPrice(), o.getCount()), Collectors.toList());
        log.info("mapping={}",mapping);
        Map<OrderQueryDto, List<OrderItemQueryDto>> collect = flats.stream()
                .collect(Collectors.groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping
                ));
        log.info("collect={}",collect);
        List<OrderQueryDto> collect1 = collect.entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(Collectors.toList());
        return collect1;
     }


    @Getter
    static class OrderDto{

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;
        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); //batch size 사용하면 in쿼리로 모조리 가져옴
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            log.info("nonoon");
            orderItems = order.getOrderItems().stream().map(OrderItemDto::new)//메소드 레퍼런스 최적화
                    .collect(Collectors.toList());
            log.info("hihi");
        }
    }

    @Getter
    static class OrderItemDto{

        private String itemName;
        private int orderPrice;
        private int count;
        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

    @Data
    @AllArgsConstructor
    static class Result<T>{
        private int count;
        private T data;
    }
}
