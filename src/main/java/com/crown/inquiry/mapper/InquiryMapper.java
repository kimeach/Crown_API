package com.crown.inquiry.mapper;

import com.crown.inquiry.dto.InquiryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InquiryMapper {
    void insert(@Param("memberId") Long memberId,
                @Param("title") String title,
                @Param("content") String content);
    InquiryDto findById(@Param("inquiryId") Long inquiryId);
}
