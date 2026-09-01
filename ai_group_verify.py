# -*- coding: utf-8 -*-
"""验证 AI 组卷难度回退：题库无该难度时自动不限难度抽取，不再返回 0 道。"""
import json
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


st, login = call("POST", "/api/auth/login", {"username": "teacher01", "password": "123456"})
token = login["data"]["token"]

# 科目1 题库难度只有 1-4(共4题)，选难度5 以前必现 0 道，现在应回退抽满
st, j = call("POST", "/api/teacher/questions/ai-group",
             {"subjectId": 1, "totalQuestions": 10, "difficulty": 5}, token=token)
n1 = len(j.get("data") or [])
print(f"科目1 difficulty=5 -> {n1} 道 (预期>0, 难度回退)")
# 科目5 题库难度只有 1-3(共5题)，选难度5 同理
st, j = call("POST", "/api/teacher/questions/ai-group",
             {"subjectId": 5, "totalQuestions": 10, "difficulty": 5}, token=token)
n5 = len(j.get("data") or [])
print(f"科目5 difficulty=5 -> {n5} 道 (预期>0, 难度回退)")
# 科目2 有难度5的题(13道)，选难度5 应正常按难度抽取
st, j = call("POST", "/api/teacher/questions/ai-group",
             {"subjectId": 2, "totalQuestions": 10, "difficulty": 5}, token=token)
n2 = len(j.get("data") or [])
print(f"科目2 difficulty=5 -> {n2} 道 (预期=10, 正常按难度抽)")

ok = n1 > 0 and n5 > 0 and n2 == 10
print("\n结果:", "PASS" if ok else "FAIL")
raise SystemExit(0 if ok else 1)
