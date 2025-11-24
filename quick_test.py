#!/usr/bin/env python3
# -*- coding: utf-8 -*-

print("开始测试...")

# 测试qstock
try:
    import qstock as qs
    print("✅ qstock库可用")
    stock_list = qs.stock_list()
    print(f"✅ qstock获取了 {len(stock_list)} 只股票")
    print(f"   首个股票: {stock_list.iloc[0, 0]}")
except Exception as e:
    print(f"❌ qstock测试失败: {e}")

# 测试AKShare
try:
    import akshare as ak
    print("✅ akshare库可用")
    sse_data = ak.stock_sse_summary()
    stock_count = int(sse_data.iloc[4, 1])
    print(f"✅ AKShare获取了上海交易所 {stock_count} 只股票")
except Exception as e:
    print(f"❌ AKShare测试失败: {e}")

print("测试完成")
