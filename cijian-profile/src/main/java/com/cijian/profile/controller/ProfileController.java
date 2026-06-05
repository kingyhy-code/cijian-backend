package com.cijian.profile.controller;

import com.cijian.common.result.R;
import com.cijian.profile.dto.*;
import com.cijian.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/stats/{userId}")
    public R<UserStatsVO> getUserStats(@PathVariable("userId") Long userId) {
        return R.success(profileService.getUserStats(userId));
    }

    @GetMapping("/tags/{userId}")
    public R<List<TagStatVO>> getUserTagStats(@PathVariable("userId") Long userId) {
        return R.success(profileService.getUserTagStats(userId));
    }

    @GetMapping("/lianci/{userId}")
    public R<LianciReportVO> getLianciReport(@PathVariable("userId") Long userId) {
        return R.success(profileService.getLianciReport(userId));
    }

    @GetMapping("/collections/export/{userId}")
    public R<CollectionExportVO> exportCollections(@PathVariable("userId") Long userId) {
        return R.success(profileService.exportCollections(userId));
    }
}
