package com.crown;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AutoDevSmokeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context, "Application context should load successfully");
    }

    @Test
    void allMappersLoaded() {
        // MyBatis 매퍼 빈이 로드되었는지 확인
        String[] beanNames = context.getBeanDefinitionNames();
        boolean hasMapper = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("mapper") || name.toLowerCase().contains("dao")) {
                hasMapper = true;
                break;
            }
        }
        assertTrue(hasMapper, "At least one Mapper/Dao bean should exist");
    }
}
