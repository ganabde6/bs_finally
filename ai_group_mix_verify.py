# -*- coding: utf-8 -*-
"""端到端验证教师 AI 组卷多题型: 指定 [单选,简答,计算] 应生成混合题型(含大题)。"""
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

# 提交任务: 题型=单选(1)+简答(5)+计算(7), 4 道
st, j = call("POST", "/api/teacher/questions/ai-group",
             {"subjectId": 2, "totalQuestions": 4, "difficulty": 3,
              "questionTypes": [1, 5, 7]}, token=token)
task_id = (j.get("data") or {}).get("taskId")
print(f"[{st}] 提交 taskId={task_id}")

if not task_id:
    raise SystemExit(1)

t0 = time.time()
result = None
while True:
    st, j = call("GET", f"/api/teacher/questions/ai-group/status/{task_id}", token=token)
    data = j.get("data") or {}
    if data.get("status") == "DONE":
        result = data
        break
    if data.get("status") == "FAILED":
        print(f"FAIL: {data.get('message')}")
        raise SystemExit(1)
    time.sleep(3)

print(f"完成耗时 {time.time()-t0:.1f}s source={result.get('source')} ids={result.get('questionIds')}")
ids = result.get("questionIds") or []
print("IDS=" + ",".join(str(x) for x in ids))
raise SystemExit(0 if ids else 1)
