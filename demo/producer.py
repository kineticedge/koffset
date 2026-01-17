import time
import json
from datetime import datetime, timezone
from confluent_kafka import Producer

# Configuration
conf = {
    'bootstrap.servers': 'localhost:19092',
    'client.id': 'python-heartbeat-producer'
}

# Initialize Producer
producer = Producer(conf)
topic = 'foo'

def delivery_report(err, msg):
    """ Called once for each message produced to indicate delivery result. """
    if err is not None:
        print(f"Message delivery failed: {err}")
    else:
        print(f"Message delivered to {msg.topic()} [{msg.partition()}] at offset {msg.offset()}")

print(f"Starting producer on {topic}. Press Ctrl+C to stop.")

try:
    while True:
        # Create a payload matching your ProductAnalytic/PurchaseOrder structures
        now = datetime.now(timezone.utc)
        payload = {
            "id": int(now.timestamp() * 1000),
            "timestamp": now.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3],
            "status": "HEARTBEAT"
        }

        # Produce message
        producer.produce(
            topic, 
            key=str(payload["id"]), 
            value=json.dumps(payload), 
            callback=delivery_report
        )

        # Serve delivery callbacks
        producer.poll(0)
        
        time.sleep(0.001)

except KeyboardInterrupt:
    print("\nStopping producer...")
finally:
    # Wait for any outstanding messages to be delivered
    producer.flush()
