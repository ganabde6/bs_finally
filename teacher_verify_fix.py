# -*- coding: utf-8 -*-
"""修复验证：①试卷无classId应返回友好错误而非500 ②带classId试卷流程正常 ③听说作业复制不再500。"""
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


ok = []
fail = []


def check(name, cond, detail=""):
    (ok if cond else fail).append(name)
    print(f"[{'PASS' if cond else 'FAIL'}] {name} {detail}")


st, login = call("POST", "/api/auth/login", {"username": "teacher01", "password": "123456"})
token = login["data"]["token"]

# ① 创建试卷不带 classId -> 应返回友好错误，不再是 SQL 500
st, j = call("POST", "/api/teacher/paper",
             {"paper": {"paperName": "无班级测试", "paperType": 1, "subjectId": 1, "duration": 30},
              "questions": [{"questionId": 1, "score": 5}]}, token=token)
msg = (j or {}).get("message", "")
check("试卷-无classId返回友好错误", j.get("code") != 500 and "班级" in str(msg), f"-> code={j.get('code')} msg={msg!r}")

# ② 带 classId 正常流程
st, j = call("POST", "/api/teacher/paper",
             {"paper": {"paperName": "验证R5试卷", "paperType": 1, "subjectId": 1, "classId": 1, "duration": 30},
              "questions": [{"questionId": 1, "score": 5}]}, token=token)
pid = j.get("data")
check("试卷-带classId新增", isinstance(pid, (int, float)) and j.get("code") == 200, f"-> id={pid}")
if isinstance(pid, (int, float)):
    st, j = call("PUT", f"/api/teacher/paper/{pid}/publish", token=token)
    check("试卷-发布", j.get("code") == 200, f"-> code={j.get('code')}")
    st, j = call("DELETE", f"/api/teacher/paper/{pid}", token=token)
    check("试卷-删除", j.get("code") == 200, f"-> code={j.get('code')}")

# ③ 听说作业复制 -> 不再 500
st, j = call("POST", "/api/teacher/listening-speaking-homework/create",
             {"title": "验证R5作业", "classId": 1, "subjectId": 1, "type": 1}, token=token)
hw = j.get("data")
check("听说作业-创建", isinstance(hw, (int, float)) and j.get("code") == 200, f"-> id={hw}")
if isinstance(hw, (int, float)):
    st, j = call("POST", f"/api/teacher/listening-speaking-homework/{hw}/copy?regenerate=false", token=token)
    check("听说作业-复制", j.get("code") == 200, f"-> code={j.get('code')} msg={j.get('message')!r}")
    st, j = call("GET", "/api/teacher/listening-speaking-homework/list", token=token)
    lst = j.get("data") or []
    mine = [x for x in lst if isinstance(x, dict) and str(x.get("title", "")).startswith("验证R5作业")]
    check("听说作业-复制产物可查", len(mine) == 2, f"-> 列表中找到 {len(mine)} 条(原+副本)")

print(f"\n===== 结果: PASS {len(ok)} / FAIL {len(fail)} =====")
if fail:
    print("失败项:", fail)
    raise SystemExit(1)
