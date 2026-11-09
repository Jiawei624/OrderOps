package com.orderops;

import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "ORDEROPS_RUN_INTEGRATION", matches = "true")
@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(RemoteDependenciesIntegrationTest.MapperConfig.class)
class RemoteDependenciesIntegrationTest {
    @TestConfiguration
    @MapperScan(basePackageClasses = ProbeMapper.class, annotationClass = Mapper.class)
    static class MapperConfig { }

    @Mapper
    interface ProbeMapper {
        @Select("SELECT description FROM orderops_schema_marker WHERE marker_id = 1")
        String marker();
        @Insert("INSERT INTO orderops_schema_marker (marker_id, description) VALUES (2, 'transaction probe')")
        void insertProbe();
        @Select("SELECT count(*) FROM orderops_schema_marker WHERE marker_id = 2")
        int countProbe();
    }

    @Autowired ProbeMapper mapper;
    @Autowired StringRedisTemplate redis;
    @Value("${orderops.redis-key-prefix}") String prefix;

    @Test
    @Transactional
    void flywayAndMyBatisReadAndWriteInDedicatedDatabase() {
        assertThat(mapper.marker()).isEqualTo("OrderOps initial schema");
        mapper.insertProbe();
        assertThat(mapper.countProbe()).isEqualTo(1);
    }

    @Test
    void redisUsesDedicatedTestNamespace() {
        String key = prefix + "probe:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "ok", java.time.Duration.ofMinutes(1));
            assertThat(redis.opsForValue().get(key)).isEqualTo("ok");
        } finally {
            redis.delete(key);
        }
    }
}
