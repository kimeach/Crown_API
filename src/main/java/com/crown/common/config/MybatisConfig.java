package com.crown.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"com.crown.*.mapper", "com.crown.*.dao"})
public class MybatisConfig {
}
