package com.crown.shorts.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UsageMapper {

    @Select("SELECT COUNT(*) FROM sm_usage_log " +
            "WHERE member_id = #{memberId} AND action = #{action} " +
            "AND DATE(created_at) BETWEEN #{from} AND #{to}")
    int countThisMonth(@Param("memberId") Long memberId,
                       @Param("action") String action,
                       @Param("from") String from,
                       @Param("to") String to);

    @Insert("INSERT INTO sm_usage_log (member_id, action) VALUES (#{memberId}, #{action})")
    void insert(@Param("memberId") Long memberId, @Param("action") String action);
}
