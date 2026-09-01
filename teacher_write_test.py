# -*- coding: utf-8 -*-
"""教师端写操作自测：内部登录，token 仅存内存。测试结束后清理创建的测试数据。"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:9000"


def call(method, path, body=None, token=None, timeout=60):
    url = BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            try:
                return resp.status, json.loads(raw)
            except Exception:
                return resp.status, {"raw": raw[:200]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw[:200]}
    except Exception as e:
        return -1, {"error": str(e)}


def brief(j):
    if isinstance(j, dict):
        code = j.get("code")
        msg = j.get("message", "")
        data = j.get("data")
        if data is None:
            return f"code={code} msg={msg}"
        if isinstance(data, dict):
            return f"code={code} msg={msg} keys={list(data.keys())[:6]}"
        if isinstance(data, list):
            return f"code={code} msg={msg} len={len(data)}"
        return f"code={code} msg={msg} data={str(data)[:100]}"
    return str(j)[:150]


results = []


def test(name, method, path, body=None, token=None, timeout=60):
    st, j = call(method, path, body, token, timeout)
    results.append((name, st, j))
    print(f"[{st}] {name} -> {brief(j)}")
    return st, j


st, login = call("POST", "/api/auth/login", {"username": "teacher01", "password": "123456"})
if st != 200 or login.get("code") != 200:
    print("登录失败，终止。")
    raise SystemExit(1)
token = login["data"]["token"]
print("[200] 登录成功\n")

created_question_id = None
created_paper_id = None
created_student_id = None
created_homework_id = None

# ===== 题库 CRUD =====
q_body = {
    "subjectId": 1, "questionType": 1, "difficulty": 2,
    "knowledgePoint": "测试知识点", "content": "【接口自测】1+1=?",
    "options": '["2","3","4","5"]', "standardAnswer": "A",
    "fullScore": 5
}
st, j = test("题目-新增", "POST", "/api/teacher/question", q_body, token=token)
if j.get("code") == 200:
    st2, j2 = call("GET", "/api/teacher/questions?current=1&size=10&keyword=%E6%8E%A5%E5%8F%A3%E8%87%AA%E6%B5%8B", token=token)
    records = (j2.get("data") or {}).get("records") or []
    for r in records:
        if r.get("content") == "【接口自测】1+1=?":
            created_question_id = r["id"]
            break
    if created_question_id:
        test("题目-修改", "PUT", "/api/teacher/question",
             {"id": created_question_id, "subjectId": 1, "questionType": 1, "difficulty": 3,
              "knowledgePoint": "测试知识点", "content": "【接口自测】1+1=?（已改）",
              "options": '["2","3","4","5"]', "standardAnswer": "A", "fullScore": 5}, token=token)
        test("题目-删除", "DELETE", f"/api/teacher/question/{created_question_id}", token=token)

# ===== 试卷 CRUD =====
paper_body = {
    "paper": {"paperName": "接口自测试卷", "paperType": 1, "subjectId": 1, "duration": 30,
              "description": "自动测试创建"},
    "questions": [{"questionId": 1, "score": 5}, {"questionId": 2, "score": 5}]
}
st, j = test("试卷-新增", "POST", "/api/teacher/paper", paper_body, token=token)
if isinstance(j.get("data"), (int, float)):
    created_paper_id = int(j["data"])
    test("试卷-发布", "PUT", f"/api/teacher/paper/{created_paper_id}/publish", token=token)
    st2, j2 = call("GET", f"/api/teacher/paper/{created_paper_id}", token=token)
    status = (j2.get("data") or {}).get("paper", {}).get("status")
    print(f"[{st2}] 试卷-发布后状态 -> status={status}")
    test("试卷-结束", "PUT", f"/api/teacher/paper/{created_paper_id}/finish", token=token)
    test("试卷-删除", "DELETE", f"/api/teacher/paper/{created_paper_id}", token=token)

# ===== 批改详情 + 手动微调 =====
st, j = test("批改-作答列表", "GET", "/api/teacher/correct/1/answers", token=token)
answers = j.get("data")
if isinstance(answers, list) and answers:
    ans = answers[0]
    aid = ans.get("id")
    if aid:
        test("批改-详情", "GET", f"/api/teacher/correct/{aid}", token=token)
        records = (ans.get("correctRecords") or [])
        if records:
            cid = records[0].get("id")
            test("批改-手动微调", "PUT", f"/api/teacher/correct/{cid}/adjust",
                 {"score": 4.5, "remark": "接口自测微调"}, token=token)
        else:
            print("[--] 批改-微调: 无批改记录，跳过")
else:
    print("[--] 批改详情: paper 1 无作答，跳过")

# ===== 学员管理 CRUD =====
stu_body = {
    "username": "test_student_tmp", "password": "123456", "realName": "接口自测学生",
    "classId": 1, "gradeLevel": 2
}
st, j = test("学员-新增", "POST", "/api/teacher/student", stu_body, token=token)
if j.get("code") == 200:
    st2, j2 = call("GET", "/api/teacher/students?current=1&size=10&keyword=test_student_tmp", token=token)
    records = (j2.get("data") or {}).get("records") or []
    for r in records:
        if r.get("username") == "test_student_tmp":
            created_student_id = r["id"]
            break
    if created_student_id:
        test("学员-修改", "PUT", "/api/teacher/student",
             {"id": created_student_id, "realName": "接口自测学生(改)", "classId": 1}, token=token)
        test("学员-重置密码", "PUT", f"/api/teacher/student/{created_student_id}/reset-password",
             {"password": "654321"}, token=token)
        test("学员-删除", "DELETE", f"/api/teacher/student/{created_student_id}", token=token)

# ===== 听说作业: 创建草稿 -> 保存题目 -> 详情 -> 发布 -> 列表 =====
hw_body = {"title": "接口自测听说作业", "classId": 1, "subjectId": 1,
           "type": 1, "description": "自动测试创建"}
st, j = test("听说作业-创建", "POST", "/api/teacher/listening-speaking-homework/create", hw_body, token=token)
hw_id = j.get("data")
if isinstance(hw_id, (int, float)):
    created_homework_id = int(hw_id)
    st2, j2 = call("GET", f"/api/teacher/listening-speaking-homework/{created_homework_id}", token=token)
    print(f"[{st2}] 听说作业-详情 -> {brief(j2)}")
    qs = (j2.get("data") or {}).get("questions") or []
    if qs:
        test("听说作业-保存题目", "POST",
             f"/api/teacher/listening-speaking-homework/{created_homework_id}/questions", qs[:1], token=token)
        test("听说作业-发布", "POST",
             f"/api/teacher/listening-speaking-homework/{created_homework_id}/publish", token=token)
        test("听说作业-列表", "GET", "/api/teacher/listening-speaking-homework/list", token=token)
        test("听说作业-复制", "POST",
             f"/api/teacher/listening-speaking-homework/{created_homework_id}/copy?regenerate=false", token=token)
        st3, j3 = call("GET", "/api/teacher/listening-speaking-homework/list", token=token)
        lst = j3.get("data") or []
        if isinstance(lst, list) and lst:
            copy = [x for x in lst if x.get("title", "").startswith("接口自测听说作业") and x.get("id") != created_homework_id]
            if copy:
                print(f"[{st3}] 听说作业-复制产物 -> id={copy[0].get('id')}")
        test("听说作业-班级报告", "GET",
             f"/api/teacher/listening-speaking-homework/{created_homework_id}/report", token=token)
    else:
        print("[--] 听说作业: 详情无题目(需要AI生成)，跳过保存/发布")
else:
    print("[--] 听说作业-创建失败，跳过后续")

print("\n===== 汇总 =====")
fails = [r for r in results if r[1] < 0 or (isinstance(r[2], dict) and r[2].get("code") not in (200, None))]
print(f"共 {len(results)} 个请求, 异常/失败 {len(fails)} 个")
for name, st, j in fails:
    print(f"  FAIL: {name} -> {brief(j)}")
