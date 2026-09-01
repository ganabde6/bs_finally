# -*- coding: utf-8 -*-
"""提交 AI 组卷任务: 验证总分分配(4道单选, 总分100 -> 每题25分)。"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:9000"


def call(method, path, body=None, token=None, timeout=30):
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
print("[200] 登录成功")

payload = {
    "subjectId": 2,
    "totalQuestions": 4,
    "totalScore": 100,
    "easyRatio": 30,
    "mediumRatio": 40,
    "hardRatio": 30,
    "typeCounts": {"1": 4},
    "questionTypes": [1],
    "knowledgePoints": []
}
st, j = call("POST", "/api/teacher/questions/ai-group", payload, token=token)
print(f"[{st}] 提交 -> {j.get('data')}")
