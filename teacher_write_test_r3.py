# -*- coding: utf-8 -*-
"""教师端写操作测试 Round3：试卷正常流程(带classId)、批改详情->微调(测后还原)、听说作业发布/复制/报告。"""
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
token = login["data"]["token"]
print("[200] 登录成功\n")

# ===== 1. 试卷正常流程(带 classId) =====
paper_body = {
    "paper": {"paperName": "接口自测R3试卷", "paperType": 1, "subjectId": 1,
              "classId": 1, "duration": 30, "description": "自动测试"},
    "questions": [{"questionId": 1, "score": 5}, {"questionId": 2, "score": 5}]
}
st, j = test("试卷-新增(带classId)", "POST", "/api/teacher/paper", paper_body, token=token)
pid = j.get("data")
if isinstance(pid, (int, float)):
    pid = int(pid)
    test("试卷-详情", "GET", f"/api/teacher/paper/{pid}", token=token)
    test("试卷-发布", "PUT", f"/api/teacher/paper/{pid}/publish", token=token)
    st2, j2 = call("GET", f"/api/teacher/paper/{pid}", token=token)
    print(f"[{st2}] 试卷-发布后状态 -> status={(j2.get('data') or {}).get('paper', {}).get('status')}")
    test("试卷-结束", "PUT", f"/api/teacher/paper/{pid}/finish", token=token)
    st3, j3 = call("GET", f"/api/teacher/paper/{pid}", token=token)
    print(f"[{st3}] 试卷-结束后状态 -> status={(j3.get('data') or {}).get('paper', {}).get('status')}")
    test("试卷-删除", "DELETE", f"/api/teacher/paper/{pid}", token=token)
else:
    print("[--] 试卷新增失败，跳过流程")

# ===== 2. 批改详情 -> 手动微调(测后还原) =====
st, j = test("批改-作答列表(paper1)", "GET", "/api/teacher/correct/1/answers", token=token)
answers = j.get("data")
if isinstance(answers, list) and answers:
    aid = answers[0].get("id")
    st2, j2 = test("批改-详情", "GET", f"/api/teacher/correct/{aid}", token=token)
    recs = j2.get("data")
    if isinstance(recs, list) and recs:
        rec = recs[0].get("record") or {}
        cid = rec.get("id")
        orig_score = rec.get("score")
        orig_remark = rec.get("remark")
        if cid:
            test("批改-手动微调", "PUT", f"/api/teacher/correct/{cid}/adjust",
                 {"score": 4.5, "remark": "接口自测微调"}, token=token)
            st3, j3 = call("GET", f"/api/teacher/correct/{aid}", token=token)
            after = [x for x in (j3.get("data") or []) if (x.get("record") or {}).get("id") == cid]
            if after:
                r2 = after[0]["record"]
                print(f"[{st3}] 批改-微调后 score={r2.get('score')} remark={r2.get('remark')!r}")
            test("批改-微调还原", "PUT", f"/api/teacher/correct/{cid}/adjust",
                 {"score": orig_score, "remark": orig_remark}, token=token)
        else:
            print("[--] 微调: 记录无id，跳过")
    else:
        print("[--] 微调: 批改详情无记录，跳过")
else:
    print("[--] 微调: paper1 无作答，跳过")

# ===== 3. 听说作业：发布/复制/报告/学生端 =====
st, j = test("听说作业-创建", "POST", "/api/teacher/listening-speaking-homework/create",
             {"title": "接口自测R3作业", "classId": 1, "subjectId": 1, "type": 1}, token=token)
hw = j.get("data")
if isinstance(hw, (int, float)):
    hw = int(hw)
    test("听说作业-详情", "GET", f"/api/teacher/listening-speaking-homework/{hw}", token=token)
    test("听说作业-发布(无题目)", "POST", f"/api/teacher/listening-speaking-homework/{hw}/publish", token=token)
    test("听说作业-列表", "GET", "/api/teacher/listening-speaking-homework/list", token=token)
    test("听说作业-复制", "POST", f"/api/teacher/listening-speaking-homework/{hw}/copy?regenerate=false", token=token)
    test("听说作业-报告", "GET", f"/api/teacher/listening-speaking-homework/{hw}/report", token=token)
else:
    print("[--] 听说作业创建失败，跳过")

print("\n===== 汇总 =====")
fails = [r for r in results if r[1] < 0 or (isinstance(r[2], dict) and r[2].get("code") not in (200, None))]
print(f"共 {len(results)} 个请求, 异常/失败 {len(fails)} 个")
for name, st, j in fails:
    print(f"  FAIL: {name} -> {brief(j)}")
