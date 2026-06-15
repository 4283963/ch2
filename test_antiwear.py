#!/usr/bin/env python3
import urllib.request
import json
import time

BASE_URL = 'http://localhost:8080'

def api_get(path):
    with urllib.request.urlopen(f'{BASE_URL}{path}') as resp:
        return json.loads(resp.read())

def api_post(path, data):
    data_bytes = json.dumps(data).encode()
    req = urllib.request.Request(
        f'{BASE_URL}{path}',
        data=data_bytes,
        headers={'Content-Type': 'application/json'}
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def scan_package(barcode):
    return api_post('/api/sorting/scan', {
        'barcode': barcode,
        'scannerId': 'TEST',
        'conveyorLine': 1,
        'timestamp': int(time.time() * 1000)
    })

def get_antiwear_status():
    return api_get('/api/antiwear/status')

def print_sorter_status(status):
    sorter = status['sorters'].get('101', {})
    print(f"  启用: {status['enabled']} | 保持: {sorter.get('isHeld')} | "
          f"连续: {sorter.get('consecutiveCount')} | "
          f"仓库: {sorter.get('currentWarehouse')} | "
          f"节省: {sorter.get('totalHoldSavings')}")

def test_1_continuous_hold():
    print("\n=== 测试1: 连续同方向包裹进入保持模式 ===")
    api_post('/api/antiwear/enable', {})
    status = get_antiwear_status()
    print("初始状态:")
    print_sorter_status(status)
    
    base = str(int(time.time() * 1000))
    for i in range(7):
        barcode = f'BJ{base}T1{i}'
        result = scan_package(barcode)
        time.sleep(0.15)
    
    time.sleep(1)
    status = get_antiwear_status()
    print("\n7个北京仓包裹后:")
    print_sorter_status(status)
    
    sorter = status['sorters'].get('101', {})
    assert sorter.get('isHeld') == True, "应该进入保持模式"
    assert sorter.get('consecutiveCount') >= 5, "连续计数应>=5"
    assert sorter.get('totalHoldSavings') >= 2, "应有节省次数"
    print("✅ 测试1通过: 连续同方向进入保持模式")

def test_2_direction_change():
    print("\n=== 测试2: 方向变更自动复位 ===")
    base = str(int(time.time() * 1000))
    barcode = f'SH{base}T2'
    result = scan_package(barcode)
    print(f"  发送上海仓包裹: {barcode}")
    
    time.sleep(1)
    status = get_antiwear_status()
    print_sorter_status(status)
    
    sorter = status['sorters'].get('101', {})
    assert sorter.get('isHeld') == False, "应该复位"
    assert sorter.get('consecutiveCount') == 1, "连续计数应为1"
    assert sorter.get('currentWarehouse') == 'SH01', "应为上海仓"
    print("✅ 测试2通过: 方向变更自动复位")

def test_3_idle_timeout():
    print("\n=== 测试3: 空闲超时自动复位 ===")
    api_post('/api/antiwear/enable', {})
    
    base = str(int(time.time() * 1000))
    for i in range(6):
        barcode = f'BJ{base}T3{i}'
        scan_package(barcode)
        time.sleep(0.1)
    
    time.sleep(1)
    status = get_antiwear_status()
    sorter = status['sorters'].get('101', {})
    print("6个北京仓包裹后:")
    print_sorter_status(status)
    assert sorter.get('isHeld') == True, "应该处于保持模式"
    
    print("等待 6 秒（超时时间5秒）...")
    time.sleep(6)
    
    status = get_antiwear_status()
    sorter = status['sorters'].get('101', {})
    print("超时后:")
    print_sorter_status(status)
    
    assert sorter.get('isHeld') == False, "应该因空闲超时而复位"
    print("✅ 测试3通过: 空闲超时自动复位")

def test_4_disable_mode():
    print("\n=== 测试4: 关闭减磨模式 ===")
    api_post('/api/antiwear/disable', {})
    
    base = str(int(time.time() * 1000))
    for i in range(10):
        barcode = f'BJ{base}T4{i}'
        scan_package(barcode)
        time.sleep(0.1)
    
    time.sleep(1)
    status = get_antiwear_status()
    sorter = status['sorters'].get('101', {})
    print("10个北京仓包裹后（减磨关闭）:")
    print_sorter_status(status)
    
    assert status['enabled'] == False, "减磨模式应关闭"
    assert sorter.get('isHeld') == False, "关闭后不应保持"
    print("✅ 测试4通过: 关闭减磨模式后不保持")

if __name__ == '__main__':
    try:
        print("开始减磨模式后端测试...")
        
        test_1_continuous_hold()
        test_2_direction_change()
        test_3_idle_timeout()
        test_4_disable_mode()
        
        print("\n🎉 所有测试通过！")
    except AssertionError as e:
        print(f"\n❌ 测试失败: {e}")
        exit(1)
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()
        exit(1)
