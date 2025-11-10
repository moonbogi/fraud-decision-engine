#!/bin/bash

# Transaction event generator for load testing

KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"
TOPIC="${TOPIC:-txn-events}"
NUM_EVENTS="${NUM_EVENTS:-1000}"

echo "Generating $NUM_EVENTS transaction events to Kafka topic: $TOPIC"

USER_IDS=("user-001" "user-002" "user-003" "user-004" "user-005")
MERCHANTS=("Amazon" "Walmart" "Target" "Costco" "Starbucks" "Apple" "BestBuy")
LOCATIONS=("US" "CA" "UK" "DE" "FR")

for i in $(seq 1 $NUM_EVENTS); do
    TXN_ID="txn-$(uuidgen | cut -d'-' -f1)"
    USER_ID=${USER_IDS[$((RANDOM % ${#USER_IDS[@]}))]}
    AMOUNT=$((RANDOM % 5000 + 10))
    MERCHANT=${MERCHANTS[$((RANDOM % ${#MERCHANTS[@]}))]}
    LOCATION=${LOCATIONS[$((RANDOM % ${#LOCATIONS[@]}))]}
    DEVICE_ID="device-$((RANDOM % 10 + 1))"
    TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")

    EVENT=$(cat <<EOF
{
  "transactionId": "$TXN_ID",
  "userId": "$USER_ID",
  "amount": $AMOUNT,
  "currency": "USD",
  "merchant": "$MERCHANT",
  "merchantCategory": "RETAIL",
  "deviceId": "$DEVICE_ID",
  "location": "$LOCATION",
  "timestamp": "$TIMESTAMP"
}
EOF
)

    echo "$EVENT" | kafka-console-producer \
        --broker-list $KAFKA_BROKER \
        --topic $TOPIC \
        --property parse.key=true \
        --property key.separator=: > /dev/null 2>&1

    if [ $((i % 100)) -eq 0 ]; then
        echo "Sent $i events..."
    fi
    
    # Add small delay for sustained load
    sleep 0.01
done

echo "✓ Generated $NUM_EVENTS events successfully"
