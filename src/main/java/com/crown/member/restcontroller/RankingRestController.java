package com.crown.member.restcontroller;

import com.crown.common.dto.ApiResponse;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingRestController {

    private final MemberService memberService;

    @GetMapping
    public ApiResponse<List<MemberDto>> getRanking(@RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(memberService.findRanking(limit));
    }
}
