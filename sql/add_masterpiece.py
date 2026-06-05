import json, urllib.request

content = open(__file__.rsplit('/', 1)[0] + '/content.txt', 'r', encoding='utf-8').read()

data = json.dumps({
    "title": "麦琪的礼物",
    "author": "欧·亨利",
    "country": "英美",
    "dynasty": "",
    "summary": "一对穷困的年轻夫妇为了给对方买圣诞礼物，各自卖掉了自己最珍贵的东西——妻子卖掉了长发为丈夫的金表买了表链，丈夫却卖掉了金表为妻子的长发买了梳子。一个关于爱与牺牲的经典短篇。",
    "content": content,
    "coverUrl": "",
    "tagNames": ["爱情", "讽刺", "心理"]
}, ensure_ascii=False).encode('utf-8')

req = urllib.request.Request('http://localhost:8086/api/operation/masterpiece',
    data=data, headers={'Content-Type': 'application/json; charset=utf-8'}, method='POST')
resp = urllib.request.urlopen(req, timeout=15)
print(resp.read().decode())
