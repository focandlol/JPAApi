package jpabook.jpaApi.domain.item;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderDto {
    private Long memberId;
    private Long itemId;

    private List<OrderDto> orderDtoList;
}
