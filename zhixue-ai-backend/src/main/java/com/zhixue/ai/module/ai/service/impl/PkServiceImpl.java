package com.zhixue.ai.module.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.module.ai.entity.*;
import com.zhixue.ai.module.ai.mapper.*;
import com.zhixue.ai.module.ai.service.PkService;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.mapper.ExamQuestionMapper;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PkServiceImpl implements PkService {

    private final PkRoomMapper pkRoomMapper;
    private final PkRoomMemberMapper pkRoomMemberMapper;
    private final PkAnswerRecordMapper pkAnswerRecordMapper;
    private final ExamQuestionMapper questionMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional
    public Map<String, Object> createRoom(Long userId, Long subjectId, Integer questionCount, Integer timeLimitSeconds) {
        // 从题库中随机抽题
        List<ExamQuestion> pool = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getSubjectId, subjectId)
                        .in(ExamQuestion::getQuestionType, Arrays.asList(1, 3))
                        .eq(ExamQuestion::getDeleted, 0));
        if (pool.isEmpty()) {
            throw new BizException("该学科暂无可用题目");
        }
        Collections.shuffle(pool);
        int count = Math.min(questionCount != null ? questionCount : 10, pool.size());
        List<ExamQuestion> selected = pool.subList(0, count);
        List<Long> qIds = selected.stream().map(ExamQuestion::getId).collect(Collectors.toList());

        // 生成6位房间号
        String roomCode = generateRoomCode();

        PkRoom room = new PkRoom();
        room.setRoomCode(roomCode);
        room.setCreatorId(userId);
        room.setSubjectId(subjectId);
        room.setQuestionCount(count);
        room.setTimeLimitSeconds(timeLimitSeconds != null ? timeLimitSeconds : 600);
        room.setQuestionIds(JSON.toJSONString(qIds));
        room.setStatus(0);
        pkRoomMapper.insert(room);

        // 创建者自动加入房间
        SysUser user = userMapper.selectById(userId);
        PkRoomMember member = new PkRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setRealName(user != null ? user.getRealName() : null);
        member.setAnsweredCount(0);
        member.setCorrectCount(0);
        member.setAccuracy(BigDecimal.ZERO);
        pkRoomMemberMapper.insert(member);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", room.getId());
        result.put("roomCode", roomCode);
        result.put("questionCount", count);
        result.put("timeLimitSeconds", room.getTimeLimitSeconds());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> joinRoom(Long userId, String roomCode) {
        PkRoom room = pkRoomMapper.selectOne(
                new LambdaQueryWrapper<PkRoom>().eq(PkRoom::getRoomCode, roomCode));
        if (room == null) {
            throw new BizException("房间不存在");
        }
        if (room.getStatus() == 2) {
            throw new BizException("该房间已结束");
        }

        // 检查是否已在房间中
        PkRoomMember existing = pkRoomMemberMapper.selectOne(
                new LambdaQueryWrapper<PkRoomMember>()
                        .eq(PkRoomMember::getRoomId, room.getId())
                        .eq(PkRoomMember::getUserId, userId));
        if (existing != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("roomId", room.getId());
            result.put("roomCode", roomCode);
            result.put("questionCount", room.getQuestionCount());
            result.put("timeLimitSeconds", room.getTimeLimitSeconds());
            result.put("status", room.getStatus());
            return result;
        }

        // 限制最多4人
        long memberCount = pkRoomMemberMapper.selectCount(
                new LambdaQueryWrapper<PkRoomMember>().eq(PkRoomMember::getRoomId, room.getId()));
        if (memberCount >= 4) {
            throw new BizException("房间已满(最多4人)");
        }

        SysUser user = userMapper.selectById(userId);
        PkRoomMember member = new PkRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setRealName(user != null ? user.getRealName() : null);
        member.setAnsweredCount(0);
        member.setCorrectCount(0);
        member.setAccuracy(BigDecimal.ZERO);
        pkRoomMemberMapper.insert(member);

        // 如果人数>=2，自动开始
        long newCount = pkRoomMemberMapper.selectCount(
                new LambdaQueryWrapper<PkRoomMember>().eq(PkRoomMember::getRoomId, room.getId()));
        if (newCount >= 2 && room.getStatus() == 0) {
            room.setStatus(1);
            room.setStartTime(LocalDateTime.now());
            pkRoomMapper.updateById(room);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", room.getId());
        result.put("roomCode", roomCode);
        result.put("questionCount", room.getQuestionCount());
        result.put("timeLimitSeconds", room.getTimeLimitSeconds());
        result.put("status", room.getStatus());
        return result;
    }

    @Override
    public List<Map<String, Object>> getRoomQuestions(Long userId, String roomCode) {
        PkRoom room = pkRoomMapper.selectOne(
                new LambdaQueryWrapper<PkRoom>().eq(PkRoom::getRoomCode, roomCode));
        if (room == null) {
            throw new BizException("房间不存在");
        }
        if (room.getStatus() < 1) {
            throw new BizException("房间尚未开始，等待更多同学加入...");
        }

        List<Long> qIds = JSON.parseArray(room.getQuestionIds(), Long.class);
        List<ExamQuestion> questions = questionMapper.selectBatchIds(qIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamQuestion q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("stem", q.getContent());
            item.put("type", q.getQuestionType());
            item.put("difficulty", q.getDifficulty());
            List<String> opts = parseOptions(q.getOptions());
            item.put("options", opts);
            item.put("correctAnswer", q.getStandardAnswer());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitAnswer(Long userId, String roomCode, Long questionId, String userAnswer) {
        PkRoom room = pkRoomMapper.selectOne(
                new LambdaQueryWrapper<PkRoom>().eq(PkRoom::getRoomCode, roomCode));
        if (room == null) {
            throw new BizException("房间不存在");
        }

        // 查找题目标准答案
        ExamQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        boolean isCorrect = question.getStandardAnswer() != null
                && question.getStandardAnswer().trim().equalsIgnoreCase(userAnswer != null ? userAnswer.trim() : "");

        // 保存答题记录
        PkAnswerRecord record = pkAnswerRecordMapper.selectOne(
                new LambdaQueryWrapper<PkAnswerRecord>()
                        .eq(PkAnswerRecord::getRoomId, room.getId())
                        .eq(PkAnswerRecord::getUserId, userId)
                        .eq(PkAnswerRecord::getQuestionId, questionId));
        if (record == null) {
            record = new PkAnswerRecord();
            record.setRoomId(room.getId());
            record.setUserId(userId);
            record.setQuestionId(questionId);
        }
        record.setUserAnswer(userAnswer);
        record.setIsCorrect(isCorrect ? 1 : 0);
        if (record.getId() == null) {
            pkAnswerRecordMapper.insert(record);
        } else {
            pkAnswerRecordMapper.updateById(record);
        }

        // 更新成员统计
        PkRoomMember member = pkRoomMemberMapper.selectOne(
                new LambdaQueryWrapper<PkRoomMember>()
                        .eq(PkRoomMember::getRoomId, room.getId())
                        .eq(PkRoomMember::getUserId, userId));
        if (member != null) {
            // 重新计算统计
            long totalAnswered = pkAnswerRecordMapper.selectCount(
                    new LambdaQueryWrapper<PkAnswerRecord>()
                            .eq(PkAnswerRecord::getRoomId, room.getId())
                            .eq(PkAnswerRecord::getUserId, userId));
            long totalCorrect = pkAnswerRecordMapper.selectCount(
                    new LambdaQueryWrapper<PkAnswerRecord>()
                            .eq(PkAnswerRecord::getRoomId, room.getId())
                            .eq(PkAnswerRecord::getUserId, userId)
                            .eq(PkAnswerRecord::getIsCorrect, 1));
            member.setAnsweredCount((int) totalAnswered);
            member.setCorrectCount((int) totalCorrect);
            if (totalAnswered > 0) {
                member.setAccuracy(BigDecimal.valueOf(totalCorrect * 100.0 / totalAnswered).setScale(2, RoundingMode.HALF_UP));
            }

            // 检查是否全部答完
            if (totalAnswered >= room.getQuestionCount()) {
                member.setFinishTime(LocalDateTime.now());
            }
            pkRoomMemberMapper.updateById(member);
        }

        // 检查是否所有人都答完
        long finishedCount = pkRoomMemberMapper.selectCount(
                new LambdaQueryWrapper<PkRoomMember>()
                        .eq(PkRoomMember::getRoomId, room.getId())
                        .isNotNull(PkRoomMember::getFinishTime));
        long totalMembers = pkRoomMemberMapper.selectCount(
                new LambdaQueryWrapper<PkRoomMember>().eq(PkRoomMember::getRoomId, room.getId()));
        if (finishedCount >= totalMembers && room.getStatus() == 1) {
            room.setStatus(2);
            room.setEndTime(LocalDateTime.now());
            pkRoomMapper.updateById(room);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questionId", questionId);
        result.put("correct", isCorrect);
        result.put("correctAnswer", question.getStandardAnswer());
        result.put("userAnswer", userAnswer);
        result.put("answeredCount", member != null ? member.getAnsweredCount() : 0);
        result.put("totalCount", room.getQuestionCount());
        return result;
    }

    @Override
    public List<Map<String, Object>> getRoomRanking(String roomCode) {
        PkRoom room = pkRoomMapper.selectOne(
                new LambdaQueryWrapper<PkRoom>().eq(PkRoom::getRoomCode, roomCode));
        if (room == null) {
            throw new BizException("房间不存在");
        }

        List<PkRoomMember> members = pkRoomMemberMapper.selectList(
                new LambdaQueryWrapper<PkRoomMember>()
                        .eq(PkRoomMember::getRoomId, room.getId())
                        .orderByDesc(PkRoomMember::getAccuracy)
                        .orderByAsc(PkRoomMember::getFinishTime));

        List<Map<String, Object>> ranking = new ArrayList<>();
        int rank = 1;
        for (PkRoomMember m : members) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("userId", m.getUserId());
            item.put("realName", m.getRealName());
            item.put("answeredCount", m.getAnsweredCount());
            item.put("correctCount", m.getCorrectCount());
            item.put("accuracy", m.getAccuracy());
            item.put("finishTime", m.getFinishTime());
            item.put("isCreator", m.getUserId().equals(room.getCreatorId()));
            ranking.add(item);
        }
        return ranking;
    }

    @Override
    public Map<String, Object> getRoomStatus(String roomCode) {
        PkRoom room = pkRoomMapper.selectOne(
                new LambdaQueryWrapper<PkRoom>().eq(PkRoom::getRoomCode, roomCode));
        if (room == null) {
            throw new BizException("房间不存在");
        }

        List<PkRoomMember> members = pkRoomMemberMapper.selectList(
                new LambdaQueryWrapper<PkRoomMember>()
                        .eq(PkRoomMember::getRoomId, room.getId()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", room.getId());
        result.put("roomCode", roomCode);
        result.put("status", room.getStatus());
        result.put("questionCount", room.getQuestionCount());
        result.put("timeLimitSeconds", room.getTimeLimitSeconds());
        result.put("memberCount", members.size());
        result.put("startTime", room.getStartTime());
        result.put("endTime", room.getEndTime());

        List<Map<String, Object>> memberList = new ArrayList<>();
        for (PkRoomMember m : members) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", m.getUserId());
            item.put("realName", m.getRealName());
            item.put("answeredCount", m.getAnsweredCount());
            item.put("isCreator", m.getUserId().equals(room.getCreatorId()));
            item.put("finishTime", m.getFinishTime());
            memberList.add(item);
        }
        result.put("members", memberList);
        return result;
    }

    private String generateRoomCode() {
        Random random = new Random();
        // 生成6位数字房间号
        int code = 100000 + random.nextInt(900000);
        String codeStr = String.valueOf(code);
        // 检查是否已存在
        long count = pkRoomMapper.selectCount(
                new LambdaQueryWrapper<PkRoom>().eq(PkRoom::getRoomCode, codeStr));
        if (count > 0) {
            return generateRoomCode();
        }
        return codeStr;
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(optionsJson, String.class);
        } catch (Exception e) {
            return Collections.singletonList(optionsJson);
        }
    }
}
