package com.zing.compass;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication
@MapperScan("com.zing.compass.mapper")
public class CompassApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompassApplication.class, args);
    }

}
