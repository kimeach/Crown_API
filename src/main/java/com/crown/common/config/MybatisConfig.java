package com.crown.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.crown.*.mapper")
public class MybatisConfig {
}
