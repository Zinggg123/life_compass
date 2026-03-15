package com.zing.compass.mapper;

import com.zing.compass.entity.Business;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BizMapper {
    List<Business> selectBusinessesByIds(@Param("list") List<String> bizIds);

    Business selectBusinessById(String bizId);
}
