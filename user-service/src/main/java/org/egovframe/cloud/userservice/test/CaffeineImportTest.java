package org.egovframe.cloud.userservice.test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

/**
 * Caffeine import 테스트
 */
public class CaffeineImportTest {
    
    public void test() {
        Cache<String, String> cache = Caffeine.newBuilder().build();
        System.out.println("Caffeine import 성공!");
    }
}
