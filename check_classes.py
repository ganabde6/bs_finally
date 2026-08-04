import json
import urllib.request

token = open('token.txt', encoding='utf-8').read().strip()
req = urllib.request.Request(
    'http://localhost:9000/api/common/classes?gradeLevel=3',
    headers={'Authorization': 'Bearer ' + token}
)
with urllib.request.urlopen(req, timeout=5) as r:
    print('HTTP', r.status)
    d = json.loads(r.read().decode('utf-8'))
    print('code:', d.get('code'), '| message:', d.get('message'))
    for c in (d.get('data') or []):
        print(' -', c.get('className'), '|', c.get('grade'), '| gradeLevel =', c.get('gradeLevel'))
