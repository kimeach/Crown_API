package com.crown.instagram.dao;

import com.crown.instagram.dto.InstagramAccountDto;
import com.crown.instagram.mapper.InstagramMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InstagramDao {
    private final InstagramMapper instagramMapper;

    public void saveAccount(InstagramAccountDto account) {
        instagramMapper.insertOrUpdateAccount(account);
    }

    public InstagramAccountDto getAccountByMemberId(Long memberId) {
        return instagramMapper.selectAccountByMemberId(memberId);
    }

    public void removeAccount(Long memberId) {
        instagramMapper.deleteAccountByMemberId(memberId);
    }
}