# -*- coding: utf-8 -*-
"""端到端验证异步 AI 组卷: POST 秒回 taskId, 轮询直至 DONE。"""
import json
import time
import urllib.request
import urllib.error

BASE = "http://localhost:9000"


def call(method, path, body=None, token=None, timeout=60):
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8", "replace"))
    except Exception as e:
        return -1, {"error": str(e)}


st, login = call("POST", "/api/auth/login", {"username": "teacher01", "password": "123456"})
token = login["data"]["token"]
print("[200] 登录成功")

# 1. 提交任务: 关键指标是耗时(应秒回,不再同步等待)
t0 = time.time()
st, j = call("POST", "/api/teacher/questions/ai-group",
             {"subjectId": 2, "totalQuestions": 3, "difficulty": 3}, token=token)
post_cost = time.time() - t0
task_id = (j.get("data") or {}).get("taskId")
print(f"[{st}] 提交耗时 {post_cost:.2f}s taskId={task_id} code={j.get('code')}")

if not task_id:
    print("FAIL: 未返回 taskId")
    raise SystemExit(1)

# 2. 轮询状态
st, j = call("GET", f"/api/teacher/questions/ai-group/status/{task_id}", token=token)
print(f"[{st}] 首次状态 -> {j.get('data', {}).get('status')}")

t0 = time.time()
result = None
while True:
    st, j = call("GET", f"/api/teacher/questions/ai-group/status/{task_id}", token=token)
    data = j.get("data") or {}
    if data.get("status") == "DONE":
        result = data
        break
    if data.get("status") == "FAILED":
        print(f"FAIL: 任务失败 {data.get('message')}")
        raise SystemExit(1)
    time.sleep(3)

total = time.time() - t0
print(f"完成耗时(轮询起) {total:.1f}s source={result.get('source')} count={result.get('count')} ids={result.get('questionIds')}")

ok = post_cost < 10 and result.get("source") == "ai" and result.get("count", 0) >= 1
print("结果:", "PASS" if ok else "FAIL")
print("IDS=" + ",".join(str(x) for x in (result.get("questionIds") or [])))
raise SystemExit(0 if ok else 1)
