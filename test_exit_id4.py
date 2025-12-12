#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import requests
from datetime import datetime

# 发送出场消息，触发id=4的支付
def send_exit_message():
    # 假设id=4的车牌是需要查询数据库的，这里先尝试发送一个测试车牌
    # 由于我们需要测试id=4，建议直接查询数据库获取id=4的entry_plate_number

    exit_message = {
        "message_id": "test-exit-id4",
        "event_type": "exit",
        "action": "exit_normal",
        "status": "exited",
        "exit_plate_number": "TEST-ID4",  # 需要与id=4的entry_plate_number匹配
        "exit_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "exit_camera_ip": "192.168.1.101",
        "exit_camera_id": 2,
        "exit_camera_name": "测试出口",
        "exit_event_id": 2001,
        "exit_detection_count": 6,
        "exit_weight": 28.30,
        "exit_snapshot": "test_exit.jpg",
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }

    # 发送MQTT消息
    url = "http://localhost:8080/mqtt/publish"
    data = {
        "message": json.dumps(exit_message)
    }

    response = requests.post(url, data=data)
    print(f"发送出场消息:")
    print(json.dumps(exit_message, indent=2, ensure_ascii=False))
    print(f"\n响应: {response.json()}")

if __name__ == "__main__":
    send_exit_message()
