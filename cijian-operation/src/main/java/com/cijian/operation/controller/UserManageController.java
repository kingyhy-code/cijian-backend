package com.cijian.operation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.operation.entity.User;
import com.cijian.operation.entity.Work;
import com.cijian.operation.mapper.UserMapper;
import com.cijian.operation.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserMapper userMapper;
    private final WorkMapper workMapper;

    @GetMapping
    public R<PageResult<User>> list(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        return R.success(PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize));
    }

    @PutMapping("/{id}/disable")
    public R<?> disable(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u != null) { u.setStatus(0); userMapper.updateById(u); }
        return R.success("已禁用");
    }

    @PutMapping("/{id}/enable")
    public R<?> enable(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u != null) { u.setStatus(1); userMapper.updateById(u); }
        return R.success("已启用");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public R<?> delete(@PathVariable Long id) {
        // 软删除：先删该用户的所有作品，再删用户
        workMapper.delete(new LambdaQueryWrapper<Work>().eq(Work::getAuthorId, id));
        userMapper.deleteById(id);
        return R.success("已删除");
    }

    @DeleteMapping("/{id}/force")
    @Transactional
    public R<?> forceDelete(@PathVariable Long id) {
        // 物理删除：先清除 RESTRICT FK 约束的表，再删用户
        userMapper.deleteAnnotationsByUserId(id);
        userMapper.deleteCommentsByUserId(id);
        userMapper.deleteWorksByAuthorId(id);  // works 级联删除 comment/like/collection 等
        userMapper.forceDeleteById(id);        // 用户级联删除 follow/like/lianci_log 等
        return R.success("已彻底删除");
    }
}
