package com.zhixue.ai.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.module.system.entity.*;
import com.zhixue.ai.module.system.mapper.*;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统管理服务(用户/角色/权限/班级/学科/公告/日志/教师任课)
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysClassMapper classMapper;
    private final SysSubjectMapper subjectMapper;
    private final SysTeacherClassMapper teacherClassMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysLogMapper logMapper;
    private final PasswordEncoder passwordEncoder;

    // ============== 用户管理 ==============

    public Page<SysUser> pageUsers(Long current, Long size, String keyword, Long roleId, Long classId) {
        Page<SysUser> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(keyword), SysUser::getUsername, keyword)
                .or().like(StringUtils.hasText(keyword), SysUser::getRealName, keyword)
                .eq(roleId != null, SysUser::getRoleId, roleId)
                .eq(classId != null, SysUser::getClassId, classId)
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        // 脱敏
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    public SysUser getUserById(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u != null) u.setPassword(null);
        return u;
    }

    public void addUser(SysUser user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(
                user.getPassword() == null ? "123456" : user.getPassword()));
        if (user.getStatus() == null) user.setStatus(1);
        userMapper.insert(user);
    }

    public void updateUser(SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }
        userMapper.updateById(user);
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    public void resetPassword(Long id, String newPwd) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(u);
    }

    /** 修改个人资料 */
    public void updateProfile(SysUser user) {
        SysUser u = new SysUser();
        u.setId(SecurityUtils.getCurrentUserId());
        u.setAvatar(user.getAvatar());
        u.setPhone(user.getPhone());
        u.setEmail(user.getEmail());
        u.setRealName(user.getRealName());
        userMapper.updateById(u);
    }

    /** 修改密码 */
    public void changePassword(String oldPwd, String newPwd) {
        Long uid = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(uid);
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new BizException("原密码错误");
        }
        SysUser u = new SysUser();
        u.setId(uid);
        u.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(u);
    }

    // ============== 角色管理 ==============

    public List<SysRole> listRoles() {
        return roleMapper.selectList(null);
    }

    public void addRole(SysRole role) {
        roleMapper.insert(role);
    }

    public void updateRole(SysRole role) {
        roleMapper.updateById(role);
    }

    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
    }

    /** 分配角色权限 */
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        for (Long pid : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
    }

    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
    }

    // ============== 权限管理 ==============

    public List<SysPermission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSort));
    }

    public List<SysPermission> treePermissions() {
        List<SysPermission> all = listPermissions();
        // 构建树
        Map<Long, List<SysPermission>> childrenMap = new HashMap<>();
        List<SysPermission> roots = new ArrayList<>();
        for (SysPermission p : all) {
            childrenMap.computeIfAbsent(p.getParentId(), k -> new ArrayList<>()).add(p);
        }
        for (SysPermission p : all) {
            if (p.getParentId() == null || p.getParentId() == 0L) {
                roots.add(p);
            }
        }
        return roots;
    }

    public void addPermission(SysPermission perm) {
        permissionMapper.insert(perm);
    }

    public void updatePermission(SysPermission perm) {
        permissionMapper.updateById(perm);
    }

    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
    }

    // ============== 班级管理 ==============

    public List<SysClass> listClasses() {
        return classMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .orderByDesc(SysClass::getCreateTime));
    }

    /** 按学段过滤班级列表 */
    public List<SysClass> listClassesByGradeLevel(Integer gradeLevel) {
        if (gradeLevel == null || gradeLevel == 0) {
            return listClasses();
        }
        return classMapper.selectList(new LambdaQueryWrapper<SysClass>()
                .eq(SysClass::getGradeLevel, gradeLevel)
                .orderByDesc(SysClass::getCreateTime));
    }

    public void addClass(SysClass cls) {
        if (cls.getStatus() == null) cls.setStatus(1);
        classMapper.insert(cls);
    }

    public void updateClass(SysClass cls) {
        classMapper.updateById(cls);
    }

    public void deleteClass(Long id) {
        classMapper.deleteById(id);
    }

    /** 查询班级学生列表 */
    public List<SysUser> listClassStudents(Long classId) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, classId)
                .eq(SysUser::getRoleId, 4L)
                .orderByAsc(SysUser::getUsername));
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    // ============== 学科管理 ==============

    public List<SysSubject> listSubjects() {
        return subjectMapper.selectList(new LambdaQueryWrapper<SysSubject>()
                .orderByAsc(SysSubject::getSort));
    }

    /** 按学段过滤学科列表（gradeLevel=0 表示通用学科） */
    public List<SysSubject> listSubjectsByGradeLevel(Integer gradeLevel) {
        if (gradeLevel == null || gradeLevel == 0) {
            return listSubjects();
        }
        return subjectMapper.selectList(new LambdaQueryWrapper<SysSubject>()
                .and(w -> w.eq(SysSubject::getGradeLevel, 0).or().eq(SysSubject::getGradeLevel, gradeLevel))
                .orderByAsc(SysSubject::getSort));
    }

    public void addSubject(SysSubject subject) {
        subjectMapper.insert(subject);
    }

    public void updateSubject(SysSubject subject) {
        subjectMapper.updateById(subject);
    }

    public void deleteSubject(Long id) {
        subjectMapper.deleteById(id);
    }

    // ============== 教师任课管理 ==============

    public List<SysTeacherClass> listTeacherCourses(Long teacherId) {
        return teacherClassMapper.selectByTeacherId(teacherId);
    }

    public void assignTeacherCourse(Long teacherId, Long classId, Long subjectId) {
        SysTeacherClass tc = new SysTeacherClass();
        tc.setTeacherId(teacherId);
        tc.setClassId(classId);
        tc.setSubjectId(subjectId);
        teacherClassMapper.insert(tc);
    }

    public void removeTeacherCourse(Long id) {
        teacherClassMapper.deleteById(id);
    }

    // ============== 公告管理 ==============

    public Page<SysNotice> pageNotices(Long current, Long size) {
        return noticeMapper.selectPage(new Page<>(current == null ? 1 : current, size == null ? 10 : size),
                new LambdaQueryWrapper<SysNotice>().orderByDesc(SysNotice::getCreateTime));
    }

    /** 查询当前用户可见公告 */
    public List<SysNotice> listVisibleNotices(String roleCode) {
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .and(w -> w.isNull(SysNotice::getTargetRole)
                        .or().eq(SysNotice::getTargetRole, roleCode))
                .orderByDesc(SysNotice::getPublishTime));
    }

    public void addNotice(SysNotice notice) {
        if (notice.getStatus() == null) notice.setStatus(0);
        notice.setPublisherId(SecurityUtils.getCurrentUserId());
        if (notice.getStatus() == 1) notice.setPublishTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    public void updateNotice(SysNotice notice) {
        if (notice.getStatus() != null && notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.updateById(notice);
    }

    public void deleteNotice(Long id) {
        noticeMapper.deleteById(id);
    }

    // ============== 日志管理 ==============

    public Page<SysLog> pageLogs(Long current, Long size, String module) {
        return logMapper.selectPage(new Page<>(current == null ? 1 : current, size == null ? 10 : size),
                new LambdaQueryWrapper<SysLog>()
                        .eq(StringUtils.hasText(module), SysLog::getModule, module)
                        .orderByDesc(SysLog::getCreateTime));
    }

    public void addLog(SysLog log) {
        logMapper.insert(log);
    }

    // ============== 教师学员管理 ==============

    /**
     * 获取教师所教班级列表
     */
    public List<SysClass> listTeacherClasses(Long teacherId) {
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        if (teacherClasses.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> classIds = teacherClasses.stream()
                .map(SysTeacherClass::getClassId)
                .collect(Collectors.toList());
        return classMapper.selectBatchIds(classIds);
    }

    /**
     * 分页查询教师所教班级的学生
     */
    public Page<SysUser> pageTeacherStudents(Long teacherId, Long current, Long size, String keyword, Long classId) {
        // 获取教师所教班级ID列表
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        if (teacherClasses.isEmpty()) {
            return new Page<>(current, size, 0);
        }
        List<Long> classIds = teacherClasses.stream()
                .map(SysTeacherClass::getClassId)
                .collect(Collectors.toList());

        Page<SysUser> page = new Page<>(current == null ? 1 : current, size == null ? 10 : size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, 4L) // 只查学生
                .in(SysUser::getClassId, classIds)
                .eq(classId != null, SysUser::getClassId, classId)
                .and(StringUtils.hasText(keyword), w -> 
                    w.like(SysUser::getUsername, keyword)
                     .or().like(SysUser::getRealName, keyword)
                )
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    /**
     * 教师添加学生
     */
    public void addStudentByTeacher(Long teacherId, SysUser student) {
        // 验证班级是否属于该教师
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        boolean validClass = teacherClasses.stream()
                .anyMatch(tc -> tc.getClassId().equals(student.getClassId()));
        if (!validClass) {
            throw new BizException("该班级不属于您，无法添加学生");
        }
        if (userMapper.selectByUsername(student.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        student.setPassword(passwordEncoder.encode(
                student.getPassword() == null ? "123456" : student.getPassword()));
        student.setRoleId(4L); // 强制学生角色
        if (student.getStatus() == null) student.setStatus(1);
        userMapper.insert(student);
    }

    /**
     * 教师更新学生信息
     */
    public void updateStudentByTeacher(Long teacherId, SysUser student) {
        // 验证学生是否属于教师所教班级
        SysUser existingStudent = userMapper.selectById(student.getId());
        if (existingStudent == null || !existingStudent.getRoleId().equals(4L)) {
            throw new BizException("学生不存在");
        }
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        boolean validClass = teacherClasses.stream()
                .anyMatch(tc -> tc.getClassId().equals(existingStudent.getClassId()));
        if (!validClass) {
            throw new BizException("该学生不属于您管理的班级");
        }
        // 不允许修改角色
        student.setRoleId(null);
        if (StringUtils.hasText(student.getPassword())) {
            student.setPassword(passwordEncoder.encode(student.getPassword()));
        } else {
            student.setPassword(null);
        }
        userMapper.updateById(student);
    }

    /**
     * 教师删除学生
     */
    public void deleteStudentByTeacher(Long teacherId, Long studentId) {
        SysUser student = userMapper.selectById(studentId);
        if (student == null || !student.getRoleId().equals(4L)) {
            throw new BizException("学生不存在");
        }
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        boolean validClass = teacherClasses.stream()
                .anyMatch(tc -> tc.getClassId().equals(student.getClassId()));
        if (!validClass) {
            throw new BizException("该学生不属于您管理的班级");
        }
        userMapper.deleteById(studentId);
    }

    /**
     * 教师重置学生密码
     */
    public void resetStudentPasswordByTeacher(Long teacherId, Long studentId, String newPassword) {
        SysUser student = userMapper.selectById(studentId);
        if (student == null || !student.getRoleId().equals(4L)) {
            throw new BizException("学生不存在");
        }
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        boolean validClass = teacherClasses.stream()
                .anyMatch(tc -> tc.getClassId().equals(student.getClassId()));
        if (!validClass) {
            throw new BizException("该学生不属于您管理的班级");
        }
        SysUser u = new SysUser();
        u.setId(studentId);
        u.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(u);
    }
}
