package com.cijian.profile.service;

import com.cijian.profile.dto.*;

import java.util.List;

public interface ProfileService {

    UserStatsVO getUserStats(Long userId);

    List<TagStatVO> getUserTagStats(Long userId);

    LianciReportVO getLianciReport(Long userId);

    CollectionExportVO exportCollections(Long userId);
}
