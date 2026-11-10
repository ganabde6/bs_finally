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
    private final SysClassMapper classMapper;
    private final SysSubjectMapper subjectMapper;
    private final SysTeacherClassMapper teacherClassMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysTaskMapper taskMapper;
    private final SysTaskRecordMapper taskRecordMapper;
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
        fillTeacherClassName(result.getRecords());
        return result;
    }

    /**
     * 教师的班级信息存于 sys_teacher_class 任课关系(sys_user.class_id 仅学生使用),
     * 列表展示时把任课班级名填入 className 字段
     */
    private void fillTeacherClassName(List<SysUser> records) {
        List<Long> teacherIds = records.stream()
                .filter(u -> u.getRoleId() != null && u.getRoleId() == 3L)
                .map(SysUser::getId)
                .collect(Collectors.toList());
        if (teacherIds.isEmpty()) return;
        for (SysUser u : records) {
            if (u.getRoleId() == null || u.getRoleId() != 3L) continue;
            List<SysTeacherClass> tcs = teacherClassMapper.selectByTeacherId(u.getId());
            String names = tcs.stream()
                    .map(SysTeacherClass::getClassId)
                    .distinct()
                    .map(cid -> classMapper.selectById(cid))
                    .filter(java.util.Objects::nonNull)
                    .map(SysClass::getClassName)
                    .collect(Collectors.joining("、"));
            u.setClassName(names);
        }
    }

    public SysUser getUserById(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u != null) u.setPassword(null);
        return u;
    }

    public void addUser(SysUser user) {
        // 账号未填写时由系统自动生成:角色前缀 + 6位随机数,冲突时重试
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(generateUsername(user.getRoleId()));
        }
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(
                user.getPassword() == null || user.getPassword().isBlank()
                        ? "123456" : user.getPassword()));
        if (user.getStatus() == null) user.setStatus(1);
        userMapper.insert(user);
    }

    /** 按角色生成账号:角色编码小写做前缀 + 6位随机数字(如 s382614/t102938),冲突时最多重试 10 次 */
    private String generateUsername(Long roleId) {
        String prefix = "u";
        if (roleId != null) {
            SysRole role = roleMapper.selectById(roleId);
            if (role != null && role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
                prefix = role.getRoleCode().toLowerCase().replaceAll("[^a-z]", "");
                if (prefix.isEmpty()) prefix = "u";
            }
        }
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 10; i++) {
            String candidate = prefix + (100000 + random.nextInt(900000));
            if (userMapper.selectByUsername(candidate) == null) {
                return candidate;
            }
        }
        // 兜底:时间戳保证唯一
        return prefix + System.currentTimeMillis() % 1000000000L;
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
        // 每位教师只能任教科目一致(同一学科可教多个班级)
        for (SysTeacherClass exist : teacherClassMapper.selectByTeacherId(teacherId)) {
            if (exist.getClassId().equals(classId) && exist.getSubjectId().equals(subjectId)) {
                throw new BizException("该教师已在该班级任此学科,请勿重复分配");
            }
            if (!exist.getSubjectId().equals(subjectId)) {
                SysSubject s = subjectMapper.selectById(exist.getSubjectId());
                String name = s != null ? s.getSubjectName() : "未知学科";
                throw new BizException("每位教师只能任教科目一致,该教师已任教【" + name + "】,不能分配其他学科");
            }
        }
        SysTeacherClass tc = new SysTeacherClass();
        tc.setTeacherId(teacherId);
        tc.setClassId(classId);
        tc.setSubjectId(subjectId);
        teacherClassMapper.insert(tc);
    }

    public void updateTeacherCourse(Long id, Long classId, Long subjectId) {
        SysTeacherClass row = teacherClassMapper.selectById(id);
        if (row == null) {
            throw new BizException("任课不存在");
        }
        boolean subjectChanged = !subjectId.equals(row.getSubjectId());
        List<SysTeacherClass> all = teacherClassMapper.selectByTeacherId(row.getTeacherId());
        for (SysTeacherClass other : all) {
            if (other.getId().equals(id)) continue;
            // 改学科后其他行会同步为新学科,按新学科判断重复
            Long otherSubject = subjectChanged ? subjectId : other.getSubjectId();
            if (other.getClassId().equals(classId) && otherSubject.equals(subjectId)) {
                throw new BizException("该教师已在该班级任此学科,请勿重复分配");
            }
        }
        row.setClassId(classId);
        row.setSubjectId(subjectId);
        teacherClassMapper.updateById(row);
        if (subjectChanged) {
            // 一师一科:同步该教师全部任课为新学科
            for (SysTeacherClass other : all) {
                if (other.getId().equals(id)) continue;
                other.setSubjectId(subjectId);
                teacherClassMapper.updateById(other);
            }
        }
    }

    public void removeTeacherCourse(Long id) {
        teacherClassMapper.deleteById(id);
    }

    // ============== 公告管理 ==============

    public Page<SysNotice> pageNotices(Long current, Long size) {
        return noticeMapper.selectPage(new Page<>(current == null ? 1 : current, size == null ? 10 : size),
                new LambdaQueryWrapper<SysNotice>().orderByDesc(SysNotice::getCreateTime));
    }

    /** 查询当前用户可见公告(已发布 且 目标为 全体/ALL 或 指定本角色) */
    public List<SysNotice> listVisibleNotices(String roleCode) {
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .and(w -> w.isNull(SysNotice::getTargetRole)
                        .or().eq(SysNotice::getTargetRole, "ALL")
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

    // ============== 任务派发 ==============

    /** 管理端任务列表(管理员派发的教师任务,含目标教师姓名) */
    public List<Map<String, Object>> listTasks() {
        List<SysTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<SysTask>()
                .eq(SysTask::getTaskType, 1)
                .orderByAsc(SysTask::getStatus)
                .orderByDesc(SysTask::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysTask t : tasks) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("content", t.getContent());
            m.put("targetTeacherId", t.getTargetTeacherId());
            m.put("targetTeacherName", t.getTargetTeacherId() == null ? "全体教师" : userName(t.getTargetTeacherId()));
            m.put("deadline", t.getDeadline());
            m.put("status", t.getStatus());
            m.put("completeTime", t.getCompleteTime());
            m.put("createTime", t.getCreateTime());
            result.add(m);
        }
        return result;
    }

    public void addTask(SysTask task) {
        task.setTaskType(1);
        if (task.getStatus() == null) task.setStatus(0);
        task.setPublisherId(SecurityUtils.getCurrentUserId());
        if (task.getStatus() != 1) task.setCompleteTime(null);
        taskMapper.insert(task);
    }

    public void updateTask(SysTask task) {
        SysTask exist = taskMapper.selectById(task.getId());
        if (exist == null) throw new BizException("任务不存在");
        if (task.getStatus() != null && task.getStatus() == 1
                && exist.getStatus() != null && exist.getStatus() == 0) {
            task.setCompleteTime(LocalDateTime.now());
        }
        if (task.getCompleteTime() == null) task.setCompleteTime(exist.getCompleteTime());
        task.setPublisherId(exist.getPublisherId());
        taskMapper.updateById(task);
    }

    public void deleteTask(Long id) {
        taskMapper.deleteById(id);
    }

    /** 当前教师的待完成任务(面向全体教师 + 指定本人的) */
    public List<SysTask> listMyPendingTasks(Long teacherId) {
        return taskMapper.selectList(new LambdaQueryWrapper<SysTask>()
                .eq(SysTask::getStatus, 0)
                .and(w -> w.isNull(SysTask::getTargetTeacherId)
                        .or().eq(SysTask::getTargetTeacherId, teacherId))
                .orderByDesc(SysTask::getCreateTime));
    }

    public void completeTask(Long teacherId, Long taskId) {
        SysTask t = taskMapper.selectById(taskId);
        if (t == null) throw new BizException("任务不存在");
        if (t.getTargetTeacherId() != null && !t.getTargetTeacherId().equals(teacherId)) {
            throw new BizException("该任务未派发给您");
        }
        t.setStatus(1);
        t.setCompleteTime(LocalDateTime.now());
        taskMapper.updateById(t);
    }

    // ============== 教师派发的学生任务 ==============

    /** 教师向所教班级派发任务(为班级每个学生生成一条待办记录) */
    public void dispatchStudentTask(Long teacherId, SysTask task) {
        Long count = teacherClassMapper.selectCount(new LambdaQueryWrapper<SysTeacherClass>()
                .eq(SysTeacherClass::getTeacherId, teacherId)
                .eq(SysTeacherClass::getClassId, task.getTargetClassId()));
        if (count == null || count == 0) {
            throw new BizException("您未任教该班级,不能向其派发任务");
        }
        task.setTaskType(2);
        task.setTargetTeacherId(null);
        if (task.getStatus() == null) task.setStatus(0);
        task.setPublisherId(teacherId);
        taskMapper.insert(task);
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, 4L)
                .eq(SysUser::getClassId, task.getTargetClassId()));
        for (SysUser s : students) {
            SysTaskRecord r = new SysTaskRecord();
            r.setTaskId(task.getId());
            r.setStudentId(s.getId());
            r.setStatus(0);
            taskRecordMapper.insert(r);
        }
    }

    /** 教师派发的学生任务列表(含完成进度) */
    public List<Map<String, Object>> listMyStudentTasks(Long teacherId) {
        List<SysTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<SysTask>()
                .eq(SysTask::getTaskType, 2)
                .eq(SysTask::getPublisherId, teacherId)
                .orderByDesc(SysTask::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysTask t : tasks) {
            List<SysTaskRecord> records = taskRecordMapper.selectList(new LambdaQueryWrapper<SysTaskRecord>()
                    .eq(SysTaskRecord::getTaskId, t.getId()));
            SysClass c = t.getTargetClassId() == null ? null : classMapper.selectById(t.getTargetClassId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("content", t.getContent());
            m.put("targetClassId", t.getTargetClassId());
            m.put("targetClassName", c == null ? "未知班级" : c.getClassName());
            m.put("deadline", t.getDeadline());
            m.put("total", records.size());
            m.put("completed", records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count());
            m.put("createTime", t.getCreateTime());
            result.add(m);
        }
        return result;
    }

    /** 教师删除自己派发的学生任务(连同学生记录) */
    public void deleteStudentTask(Long teacherId, Long taskId) {
        SysTask t = taskMapper.selectById(taskId);
        if (t == null || t.getTaskType() == null || t.getTaskType() != 2) {
            throw new BizException("任务不存在");
        }
        if (t.getPublisherId() == null || !t.getPublisherId().equals(teacherId)) {
            throw new BizException("只能删除自己派发的任务");
        }
        taskRecordMapper.delete(new LambdaQueryWrapper<SysTaskRecord>().eq(SysTaskRecord::getTaskId, taskId));
        taskMapper.deleteById(taskId);
    }

    /** 当前学生的待完成任务(未完成) */
    public List<Map<String, Object>> listMyPendingStudentTasks(Long studentId) {
        List<SysTaskRecord> records = taskRecordMapper.selectList(new LambdaQueryWrapper<SysTaskRecord>()
                .eq(SysTaskRecord::getStudentId, studentId)
                .eq(SysTaskRecord::getStatus, 0)
                .orderByDesc(SysTaskRecord::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysTaskRecord r : records) {
            SysTask t = taskMapper.selectById(r.getTaskId());
            if (t == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("recordId", r.getId());
            m.put("taskId", t.getId());
            m.put("title", t.getTitle());
            m.put("content", t.getContent());
            m.put("deadline", t.getDeadline());
            m.put("createTime", t.getCreateTime());
            result.add(m);
        }
        return result;
    }

    /** 学生完成自己的任务待办 */
    public void completeStudentTask(Long studentId, Long recordId) {
        SysTaskRecord r = taskRecordMapper.selectById(recordId);
        if (r == null || !r.getStudentId().equals(studentId)) {
            throw new BizException("任务待办不存在");
        }
        if (r.getStatus() != null && r.getStatus() == 1) {
            throw new BizException("该任务已完成");
        }
        r.setStatus(1);
        r.setCompleteTime(LocalDateTime.now());
        taskRecordMapper.updateById(r);
    }

    private String userName(Long userId) {
        SysUser u = userId == null ? null : userMapper.selectById(userId);
        if (u == null) return "未知";
        return u.getRealName() != null && !u.getRealName().isEmpty() ? u.getRealName() : u.getUsername();
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
     * 获取教师任教的学科列表(去重)
     */
    public List<SysSubject> listTeacherSubjects(Long teacherId) {
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        if (teacherClasses.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> subjectIds = teacherClasses.stream()
                .map(SysTeacherClass::getSubjectId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (subjectIds.isEmpty()) {
            return new ArrayList<>();
        }
        return subjectMapper.selectBatchIds(subjectIds);
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
