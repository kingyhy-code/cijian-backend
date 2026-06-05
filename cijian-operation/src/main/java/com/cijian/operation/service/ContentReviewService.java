package com.cijian.operation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.page.PageResult;
import com.cijian.operation.mapper.WorkMapper;
import com.cijian.operation.entity.Work;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentReviewService {

    private final WorkMapper workMapper;

    public PageResult<Work> listPending(int pageNum, int pageSize) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getStatus, 0).orderByDesc(Work::getCreateTime);
        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize);
    }

    public PageResult<Work> listAll(Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Work::getStatus, status);
        wrapper.orderByDesc(Work::getCreateTime);
        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize);
    }

    public Work approve(Long workId) {
        Work w = workMapper.selectById(workId);
        if (w != null) { w.setStatus(1); workMapper.updateById(w); }
        return w;
    }

    public Work reject(Long workId) {
        Work w = workMapper.selectById(workId);
        if (w != null) { w.setStatus(2); workMapper.updateById(w); }
        return w;
    }

    // ====== 名家作品管理 ======
    public Long createMasterpiece(Long adminId, String title, String content, String summary, String originalAuthor, String coverUrl) {
        Work w = new Work();
        w.setAuthorId(adminId);
        w.setTitle(title); w.setContent(content); w.setSummary(summary);
        w.setOriginalAuthor(originalAuthor); w.setCoverUrl(coverUrl);
        w.setIsMasterpiece(1); w.setIsInspiration(0); w.setContentType(1); w.setStatus(1);
        w.setViewCount(0L); w.setLikeCount(0L); w.setCommentCount(0L); w.setCollectCount(0L); w.setInspirationRefCount(0L);
        workMapper.insert(w);
        return w.getId();
    }

    public void updateMasterpiece(Long id, Map<String, String> body) {
        Work w = workMapper.selectById(id);
        if (w == null) return;
        if (body.containsKey("title")) w.setTitle(body.get("title"));
        if (body.containsKey("content")) w.setContent(body.get("content"));
        if (body.containsKey("summary")) w.setSummary(body.get("summary"));
        if (body.containsKey("originalAuthor")) w.setOriginalAuthor(body.get("originalAuthor"));
        if (body.containsKey("coverUrl")) w.setCoverUrl(body.get("coverUrl"));
        workMapper.updateById(w);
    }

    public void deleteWork(Long id) { workMapper.deleteById(id); }

    public void forceDeleteWork(Long id) { workMapper.forceDeleteById(id); }

    public void deleteMasterpiece(Long id) { workMapper.deleteById(id); }

    public PageResult<Work> listMasterpieces(int pageNum, int pageSize) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getIsMasterpiece, 1).eq(Work::getStatus, 1).orderByDesc(Work::getCreateTime);
        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize);
    }

    public Map<String, Long> stats() {
        long pending = workMapper.selectCount(new LambdaQueryWrapper<Work>().eq(Work::getStatus, 0));
        long published = workMapper.selectCount(new LambdaQueryWrapper<Work>().eq(Work::getStatus, 1));
        long rejected = workMapper.selectCount(new LambdaQueryWrapper<Work>().eq(Work::getStatus, 2));
        long total = workMapper.selectCount(null);
        return Map.of("pending", pending, "published", published, "rejected", rejected, "total", total);
    }
}
