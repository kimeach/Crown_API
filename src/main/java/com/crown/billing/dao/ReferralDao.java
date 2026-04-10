package com.crown.billing.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReferralDao {

    void insertReferral(Map<String, Object> params);

    Map<String, Object> getReferralByCode(@Param("referralCode") String referralCode);

    Map<String, Object> getByInviterAndInvitee(@Param("inviterId") Long inviterId,
                                                @Param("inviteeId") Long inviteeId);

    void updateStatus(@Param("id") Long id,
                      @Param("status") String status);

    void updateSignupBonusGiven(@Param("id") Long id);

    void updateSubBonusGiven(@Param("id") Long id);

    Map<String, Object> getRewardConfig(@Param("eventType") String eventType,
                                         @Param("planName") String planName);

    Map<String, Object> getStatsByInviter(@Param("inviterId") Long inviterId);

    List<Map<String, Object>> getHistoryByInviter(@Param("inviterId") Long inviterId,
                                                   @Param("limit") int limit,
                                                   @Param("offset") int offset);

    int getHistoryCountByInviter(@Param("inviterId") Long inviterId);

    Map<String, Object> getReferralByInviteeId(@Param("inviteeId") Long inviteeId);
}
