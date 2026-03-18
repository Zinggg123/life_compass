package com.zing.compass.mapper;

import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {
    Integer insertOrder(OrderInfo orderInfo);

    List<UserBehavior> selectRecentOrderBiz(String userId, Integer limit);
}
