package com.orderops.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountMapper {
    @Select("SELECT id, username, password_hash, role, enabled FROM accounts WHERE username = #{username}")
    Account findByUsername(@Param("username") String username);

    @Insert("INSERT INTO accounts(username, password_hash, role) VALUES (#{username}, #{passwordHash}, #{role}) ON CONFLICT (username) DO NOTHING")
    int insertIfAbsent(@Param("username") String username, @Param("passwordHash") String passwordHash, @Param("role") String role);
}
