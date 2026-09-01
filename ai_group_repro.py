# -*- coding: utf-8 -*-
"""复现 AI 智能组卷返回 0 道题：内部登录，token 仅内存。"""
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


st, login = call("POST", "/api/auth/login", {"username": "teacher01", "password": "123456"})
token = login["data"]["token"]
print("[200] 登录成功\n")

# 前端默认: totalQuestions=10, difficulty=3, 各学科轮询
for sid in (1, 2, 5, 9, 10):
    for diff in (3, None):
        body = {"subjectId": sid, "totalQuestions": 10}
        label = f"diff={diff}"
        if diff is not None:
            body["difficulty"] = diff
        st, j = call("POST", "/api/teacher/questions/ai-group", body, token=token)
        data = j.get("data")
        n = len(data) if isinstance(data, list) else "?"
        print(f"科目{sid} {label} -> 抽取 {n} 道  code={j.get('code')}")
