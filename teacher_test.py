# -*- coding: utf-8 -*-
"""教师端接口自测脚本：内部登录(本地测试账号)，token 仅存内存，不落盘、不进命令行。"""
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
                j = json.loads(raw)
            except Exception:
                j = {"raw": raw[:200]}
            return resp.status, j
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
            keys = list(data.keys())[:6]
            return f"code={code} msg={msg} data_keys={keys}"
        if isinstance(data, list):
            return f"code={code} msg={msg} list_len={len(data)}"
        if isinstance(data, str):
            return f"code={code} msg={msg} data={data[:80]!r}"
        return f"code={code} msg={msg} data={str(data)[:80]}"
    return str(j)[:150]


results = []


def test(name, method, path, body=None, token=None, timeout=60):
    st, j = call(method, path, body, token, timeout)
    results.append((name, st, j))
    print(f"[{st}] {name} -> {brief(j)}")
    return st, j


# 1. 登录(教师)
st, login = test("登录 teacher01", "POST", "/api/auth/login",
                 {"username": "teacher01", "password": "123456"})
if st != 200 or login.get("code") != 200:
    print("登录失败，终止。")
    raise SystemExit(1)
token = login["data"]["token"]
uid = login["data"]["userInfo"]["userId"]

# 2. 首页统计
test("教师首页统计", "GET", "/api/teacher/dashboard", token=token)

# 3. 题库管理
test("题库-分页", "GET", "/api/teacher/questions?current=1&size=3", token=token)
st, q = test("题库-AI组卷", "POST", "/api/teacher/questions/ai-group",
             {"subjectId": 1, "totalQuestions": 3, "difficulty": 2}, token=token)
qid = None
if isinstance(q.get("data"), list) and q["data"]:
    qid = q["data"][0]
    test(f"题库-查看题目{id}", "GET", f"/api/teacher/question/{qid}", token=token)

# 4. 试卷管理
st, p = test("试卷-分页", "GET", "/api/teacher/papers?current=1&size=5", token=token)
paper_id = None
paper_data = p.get("data") if isinstance(p.get("data"), dict) else {}
records = paper_data.get("records") or []
if records:
    paper_id = records[0].get("id")
    test("试卷-详情", "GET", f"/api/teacher/paper/{paper_id}", token=token)

# 5. 批改管理
if paper_id:
    test("批改-作答列表", "GET", f"/api/teacher/correct/{paper_id}/answers", token=token)
    test("批改-批量AI批改", "POST", f"/api/teacher/correct/{paper_id}/batch", token=token, timeout=120)
    test("批改-雷同查重", "POST", f"/api/teacher/correct/{paper_id}/similarity", token=token, timeout=120)
else:
    print("[--] 批改相关: 无试卷数据，跳过")

# 6. 班级学情 / 家校反馈 / 自主学练
st, mc = test("我的班级", "GET", "/api/teacher/my-classes", token=token)
class_id = None
mc_data = mc.get("data")
if isinstance(mc_data, list) and mc_data:
    class_id = mc_data[0].get("id") or mc_data[0].get("classId")
elif isinstance(mc_data, dict):
    class_id = (mc_data.get("records") or [{}])[0].get("id")
if class_id:
    test("班级学情-分析", "GET", f"/api/teacher/class/{class_id}/analysis", token=token)
    test("班级学情-学生列表", "GET", f"/api/teacher/class/{class_id}/students", token=token)
    test("自主学练-概况", "GET", f"/api/teacher/class/self-study/stats?classId={class_id}", token=token)
    st, cls = test("班级-学生", "GET", f"/api/teacher/class/{class_id}/students", token=token)
    studs = cls.get("data")
    if isinstance(studs, list) and studs:
        sid = studs[0].get("id")
        test("家校反馈-生成评语", "GET", f"/api/teacher/feedback/{sid}", token=token)
    else:
        print("[--] 家校反馈: 无学生数据，跳过")
else:
    print("[--] 班级学情/反馈: 教师无班级，跳过")

# 7. 学员管理
test("学员-分页", "GET", "/api/teacher/students?current=1&size=3", token=token)

# 8. 听说作业
test("听说作业-列表", "GET", "/api/teacher/listening-speaking-homework/list", token=token)

print("\n===== 汇总 =====")
fails = [r for r in results if r[1] < 0 or (isinstance(r[2], dict) and r[2].get("code") not in (200, None))]
print(f"共 {len(results)} 个请求, 异常/失败 {len(fails)} 个")
for name, st, j in fails:
    print(f"  FAIL: {name} -> {brief(j)}")
