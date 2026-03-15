package com.zing.compass.mapper;

import com.zing.compass.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    int insertOrder(OrderInfo orderInfo);
}
