#!/usr/bin/env python3
import urllib.request
import json
import time
import sys

BASE = 'http://localhost:8080'

def api_post(path, data=None):
    if data is None:
        data = {}
    req = urllib.request.Request(
        f'{BASE}{path}',
        data=json.dumps(data).encode(),
        headers={'Content-Type': 'application/json'}
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def api_get(path):
    with urllib.request.urlopen(f'{BASE}{path}') as resp:
        return json.loads(resp.read())

def scan(barcode):
    return api_post('/api/sorting/scan', {
        'barcode': barcode,
        'scannerId': 'TEST',
        'conveyorLine': 1,
        'timestamp': int(time.time()*1000)
    })

def print_status():
    s = api_get('/api/antiwear/status')
    print(f"  减磨启用: {s['enabled']}")
    for sid in sorted(s['sorters'].keys(), key=lambda x: int(x)):
        sorter = s['sorters'][sid]
        print(f"  分拣机{sid}: 保持={sorter['isHeld']}, 连续={sorter['consecutiveCount']}, "
              f"仓库={sorter['currentWarehouse']}, 节省={sorter['totalHoldSavings']}")

print("=== 测试: 方向变更自动复位 ===")
print()

print("步骤1: 开启减磨模式")
api_post('/api/antiwear/enable')
print_status()
print()

print("步骤2: 发送7个北京仓包裹")
ts = str(int(time.time()*1000))
for i in range(7):
    result = scan(f'BJ{ts}X{i}')
    print(f"  BJ{ts}X{i}: {result['success']} -> {result.get('destinationWarehouse')}")
    time.sleep(0.12)

time.sleep(1.5)
print("\n7个北京仓包裹后:")
print_status()
print()

print("步骤3: 发送1个上海仓包裹")
result = scan(f'SH{ts}Y0')
print(f"  SH{ts}Y0: {result['success']} -> {result.get('destinationWarehouse')}")

time.sleep(1.5)
print("\n发送上海仓包裹后:")
print_status()
print()

s = api_get('/api/antiwear/status')
s101 = s['sorters'].get('101', {})
s102 = s['sorters'].get('102', {})

passed = True
if s101.get('isHeld') == False:
    print("✅ 分拣机101(北京仓)已成功复位")
else:
    print("❌ 分拣机101(北京仓)未复位")
    passed = False

if s102.get('isHeld') == False and s102.get('consecutiveCount') == 1:
    print("✅ 分拣机102(上海仓)计数正常")
else:
    print("❌ 分拣机102(上海仓)状态异常")
    passed = False

print()
if passed:
    print("🎉 方向变更测试通过！")
    sys.exit(0)
else:
    print("💥 方向变更测试失败！")
    sys.exit(1)
