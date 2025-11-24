#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
测试qstock和AKShare两种API获取A股股票列表
"""

import json
import sys

print("=" * 60)
print("开始测试股票列表API")
print("=" * 60)

results = {}

# 测试1: qstock API
print("\n【测试1】qstock API获取A股列表")
print("-" * 60)
try:
    import qstock as qs
    print("✅ qstock库已安装")
    
    stock_list = qs.stock_list()
    total_count = len(stock_list)
    
    print(f"✅ 获取成功！总股票数: {total_count}")
    print(f"首5条记录:")
    for i in range(min(5, len(stock_list))):
        code = stock_list.iloc[i, 0]
        name = stock_list.iloc[i, 1] if stock_list.shape[1] > 1 else "N/A"
        print(f"  {i+1}. {code} - {name}")
    
    results['qstock'] = {
        'success': True,
        'total_count': total_count,
        'first_code': stock_list.iloc[0, 0] if len(stock_list) > 0 else None
    }
except ImportError as e:
    print(f"❌ qstock库未安装: {e}")
    print("   请执行: pip install qstock -U --no-cache-dir")
    results['qstock'] = {'success': False, 'error': 'ImportError', 'message': str(e)}
except Exception as e:
    print(f"❌ qstock执行出错: {e}")
    results['qstock'] = {'success': False, 'error': type(e).__name__, 'message': str(e)}

# 测试2: AKShare API - 获取上海交易所数据
print("\n【测试2】AKShare API获取上海交易所股票")
print("-" * 60)
try:
    import akshare as ak
    print("✅ akshare库已安装")
    
    sse_summary = ak.stock_sse_summary()
    print("✅ 上海交易所数据获取成功")
    print(f"上市公司数: {sse_summary.iloc[3, 1]}")
    print(f"上市股票数: {sse_summary.iloc[4, 1]}")
    
    results['akshare_sse'] = {
        'success': True,
        'company_count': int(sse_summary.iloc[3, 1]),
        'stock_count': int(sse_summary.iloc[4, 1])
    }
except ImportError as e:
    print(f"❌ akshare库未安装: {e}")
    print("   请执行: pip install akshare")
    results['akshare_sse'] = {'success': False, 'error': 'ImportError', 'message': str(e)}
except Exception as e:
    print(f"❌ AKShare执行出错: {e}")
    results['akshare_sse'] = {'success': False, 'error': type(e).__name__, 'message': str(e)}

# 测试3: AKShare API - 获取深圳交易所数据
print("\n【测试3】AKShare API获取深圳交易所股票")
print("-" * 60)
try:
    import akshare as ak
    from datetime import datetime
    
    # 使用当前日期的前一个交易日
    szse_summary = ak.stock_szse_summary(date=datetime.now().strftime("%Y%m%d"))
    print("✅ 深圳交易所数据获取成功")
    
    # 查找股票行
    for idx, row in szse_summary.iterrows():
        if row['证券类别'] == '股票':
            print(f"股票数量: {row['数量']}")
            results['akshare_szse'] = {
                'success': True,
                'stock_count': int(row['数量'])
            }
            break
            
except Exception as e:
    print(f"⚠️  AKShare获取深圳交易所数据出错: {e}")
    results['akshare_szse'] = {'success': False, 'error': type(e).__name__, 'message': str(e)}

# 输出对比结果
print("\n" + "=" * 60)
print("测试结果总结")
print("=" * 60)
print(json.dumps(results, indent=2, ensure_ascii=False))

# 总体评价
print("\n【结论】")
qstock_ok = results.get('qstock', {}).get('success', False)
akshare_ok = results.get('akshare_sse', {}).get('success', False) or results.get('akshare_szse', {}).get('success', False)

if qstock_ok and akshare_ok:
    print("✅ 两种API都可用，可以根据需求选择使用")
    print(f"   qstock: 获取{results['qstock']['total_count']}只股票（包含已退市）")
    print(f"   AKShare: 获取上海交易所{results.get('akshare_sse', {}).get('stock_count', 'N/A')}只股票")
elif qstock_ok:
    print("✅ qstock API可用")
    print(f"   获取{results['qstock']['total_count']}只股票（包含已退市）")
elif akshare_ok:
    print("✅ AKShare API可用")
else:
    print("❌ 两种API都未安装，请先安装库：")
    print("   pip install qstock -U --no-cache-dir")
    print("   pip install akshare")

sys.exit(0)
