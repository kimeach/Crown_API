#!/bin/bash
# Cafe24 1GB 서버 운영 실행 스크립트
java -Xms128m -Xmx300m \
     -jar build/libs/Crown_API-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=prod
