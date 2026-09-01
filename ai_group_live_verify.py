# -*- coding: utf-8 -*-
"""验证教师 AI 智能组卷: 应返回 source=ai 且为库中新增题目ID。"""
import json
import time
import urllib.request
import urllib.error

BASE = "http://localhost:9000"


def call(method, path, body=None, token=None, timeout=300):
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

t0 = time.time()
st, j = call("POST", "/api/teacher/questions/ai-group",
             {"subjectId": 2, "totalQuestions": 3, "difficulty": 3}, token=token)
cost = time.time() - t0
data = j.get("data") or {}
ids = data.get("questionIds") or []
print(f"[{st}] 组卷耗时 {cost:.1f}s")
print(f"    source={data.get('source')} count={data.get('count')} ids={ids}")

ok = st == 200 and j.get("code") == 200 and data.get("source") == "ai" and len(ids) >= 1
print("结果:", "PASS" if ok else "FAIL")
print("IDS=" + ",".join(str(x) for x in ids))
raise SystemExit(0 if ok else 1)
