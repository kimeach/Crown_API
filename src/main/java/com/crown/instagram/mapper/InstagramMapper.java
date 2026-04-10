package com.crown.instagram.mapper;

import com.crown.instagram.dto.InstagramAccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InstagramMapper {
    void insertOrUpdateAccount(InstagramAccountDto account);
    InstagramAccountDto selectAccountByMemberId(@Param("memberId") Long memberId);
    void deleteAccountByMemberId(@Param("memberId") Long memberId);
}