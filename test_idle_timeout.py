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

print("=== 测试: 空闲超时自动复位 ===")
print()

print("步骤1: 开启减磨模式")
api_post('/api/antiwear/enable')
print_status()
print()

print("步骤2: 发送6个北京仓包裹")
ts = str(int(time.time()*1000))
for i in range(6):
    result = scan(f'BJ{ts}I{i}')
    time.sleep(0.1)

time.sleep(1.5)
print("\n6个北京仓包裹后:")
print_status()
print()

s = api_get('/api/antiwear/status')
s101 = s['sorters'].get('101', {})
if not s101.get('isHeld'):
    print("❌ 未进入保持模式，测试无法继续")
    sys.exit(1)

print("步骤3: 等待 6 秒 (超时时间 5 秒)...")
for i in range(6):
    time.sleep(1)
    print(f"  等待中... {i+1}s")

print("\n超时后:")
print_status()
print()

s = api_get('/api/antiwear/status')
s101 = s['sorters'].get('101', {})

if s101.get('isHeld') == False:
    print("✅ 空闲超时后分拣机101已成功复位")
    print()
    print("🎉 空闲超时测试通过！")
    sys.exit(0)
else:
    print("❌ 空闲超时后分拣机101未复位")
    print()
    print("💥 空闲超时测试失败！")
    sys.exit(1)
