package org.egovframe.cloud.apigateway.utils;

import org.egovframe.cloud.apigateway.dto.ServicePathResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathExtractorTest {

    @Test
    void extractServiceAndPath_정상적인_경로() {
        // Given
        String fullPath = "/user-service/api/users/profile";
        
        // When
        ServicePathResult result = PathExtractor.extractServiceAndPath(fullPath);
        
        // Then
        assertEquals("user-service", result.getServiceName());
        assertEquals("/api/users/profile", result.getRequestPath());
    }

    @Test
    void extractServiceAndPath_루트_경로만_있는_경우() {
        // Given
        String fullPath = "/user-service";
        
        // When
        ServicePathResult result = PathExtractor.extractServiceAndPath(fullPath);
        
        // Then
        assertEquals("user-service", result.getServiceName());
        assertEquals("/", result.getRequestPath());
    }

    @Test
    void extractServiceAndPath_알려지지_않은_서비스() {
        // Given
        String fullPath = "/unknown-path/api/test";
        
        // When
        ServicePathResult result = PathExtractor.extractServiceAndPath(fullPath);
        
        // Then
        assertEquals("unknown-service", result.getServiceName());
        assertEquals("/unknown-path/api/test", result.getRequestPath());
    }

    @Test
    void extractServiceAndPath_빈_경로() {
        // Given
        String fullPath = "";
        
        // When
        ServicePathResult result = PathExtractor.extractServiceAndPath(fullPath);
        
        // Then
        assertEquals("unknown-service", result.getServiceName());
        assertEquals("/", result.getRequestPath());
    }

    @Test
    void extractServiceAndPath_null_경로() {
        // Given
        String fullPath = null;
        
        // When
        ServicePathResult result = PathExtractor.extractServiceAndPath(fullPath);
        
        // Then
        assertEquals("unknown-service", result.getServiceName());
        assertEquals("/", result.getRequestPath());
    }
}
