package jpabook.jpaApi.service;

import jakarta.persistence.EntityManager;
import jpabook.jpaApi.domain.Address;
import jpabook.jpaApi.domain.Member;
import jpabook.jpaApi.domain.Order;
import jpabook.jpaApi.domain.OrderStatus;
import jpabook.jpaApi.domain.item.Book;
import jpabook.jpaApi.domain.item.Item;
import jpabook.jpaApi.exception.NotEnoughStockException;
import jpabook.jpaApi.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    EntityManager em;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception{
        Member member = createMember();

        Book book = createBook("jpa", 10000, 10);

        int orderCount = 2;
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        Order getOrder = orderRepository.findOne(orderId);

        assertEquals(OrderStatus.ORDER,getOrder.getStatus(),"상품 주문시 상태는 order");
        assertEquals(1,getOrder.getOrderItems().size(),"주문한 상품 종류 수가 정확해야 한다");
        assertEquals(10000*orderCount,getOrder.getTotalPrice(),"주문 가격은 가격*수량이다");
        assertEquals(8, book.getStockQuantity(),"주문 수량만큼 재고가 줄어야 한다.");
    }

    @Test
    public void 주문취소() throws Exception{
        Member member = createMember();
        Book item = createBook("jpa", 10000, 10);
        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);
        System.out.println(" item.stockcount " + item.getStockQuantity());


        System.out.println(" item.stockcount " + item.getStockQuantity());

        orderService.cancelOrder(orderId);
        em.flush();
        em.clear();
        Order getOrder = orderRepository.findOne(orderId);
        System.out.println("getOrder = " + getOrder);
        System.out.println("getOrder = " + getOrder.getStatus());

        System.out.println("getOrder = " + getOrder.getStatus());

        assertEquals(OrderStatus.CANCEL,getOrder.getStatus(),"주문 취소시 상태는 cancel이다");
        assertEquals(10, item.getStockQuantity(),"주문이 취소된 상품은 그만큼 재고가 증가해야 함");
    }

    @Test
    public void 상품주문_재고수량초과() throws Exception{
        Member member = createMember();
        Item item = createBook("jpa", 10000, 10);

        int orderCount = 11;

        assertThrows(NotEnoughStockException.class,
                () -> orderService.order(member.getId(), item.getId(), orderCount));
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울","강가","123123"));
        em.persist(member);
        return member;
    }
}