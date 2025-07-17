package org.egovframe.cloud.userservice.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.egovframe.cloud.userservice.domain.Permission;

/**
 * 권한 정보 조회 MyBatis 매퍼
 * 
 * @version 1.0
 * @since 2025/07/18
 */
@Mapper
public interface PermissionMapper {

    /**
     * 특정 역할의 직접 권한 조회 (상속 제외)
     * 
     * @param roleName 역할명
     * @return 권한 목록
     */
    List<Permission> findDirectPermissionsByRole(@Param("roleName") String roleName);

    /**
     * 특정 역할의 모든 권한 조회 (상속 포함)
     * 
     * @param roleName 역할명  
     * @return 권한 목록
     */
    List<Permission> findAllPermissionsByRole(@Param("roleName") String roleName);

    /**
     * 모든 역할명 조회
     * 
     * @return 역할명 목록
     */
    List<String> findAllRoleNames();

//    /**
//     * 권한 데이터 해시값 조회 (캐시 무효화 판단용)
//     *
//     * @return 해시값
//     */
//    String getPermissionDataHash();
}
