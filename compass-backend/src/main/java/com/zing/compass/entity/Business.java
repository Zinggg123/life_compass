package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//商家实体
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Business {
    private String bizId;
    private String name;
    private String address;
    private String city;
    private String state;
    private String categories;
    private Double score;  //评分
    private Integer reviewCount; //评论数
}
//{"business_id":"Pns2l4eNsfO8kk83dixA6A","name":"Abby Rappoport, LAC, CMQ","address":"1616 Chapala St, Ste 2","city":"Santa Barbara","state":"CA","postal_code":"93101","latitude":34.4266787,"longitude":-119.7111968,"stars":5.0,"review_count":7,"is_open":0,"attributes":{"ByAppointmentOnly":"True"},"categories":"Doctors, Traditional Chinese Medicine, Naturopathic\/Holistic, Acupuncture, Health & Medical, Nutritionists","hours":null}