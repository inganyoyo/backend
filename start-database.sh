#!/bin/bash

echo "======================================="
echo "🚀 전자정부 마이크로서비스 DB 시작"
echo "======================================="

# Docker Compose로 PostgreSQL과 Redis 시작
echo "📦 PostgreSQL과 Redis 컨테이너 시작 중..."
docker-compose up -d

# 컨테이너 상태 확인
echo ""
echo "📋 컨테이너 상태 확인..."
docker-compose ps

# PostgreSQL 연결 대기
echo ""
echo "⏳ PostgreSQL 준비 상태 확인 중..."
for i in {1..30}; do
    if docker-compose exec -T postgres pg_isready -U egovframe -d egovframe_user > /dev/null 2>&1; then
        echo "✅ PostgreSQL 준비 완료!"
        break
    fi
    echo "   대기 중... ($i/30)"
    sleep 2
done

# Redis 연결 확인
echo ""
echo "⏳ Redis 준비 상태 확인 중..."
if docker-compose exec -T redis redis-cli -p 6379 -a egovframe123 ping > /dev/null 2>&1; then
    echo "✅ Redis 준비 완료!"
else
    echo "❌ Redis 연결 실패"
fi

echo ""
echo "======================================="
echo "🎉 데이터베이스 준비 완료!"
echo "======================================="
echo "📍 PostgreSQL:"
echo "   - 주소: localhost:5432"
echo "   - 데이터베이스: egovframe_user"
echo "   - 사용자: egovframe"
echo "   - 비밀번호: egovframe123"
echo ""
echo "📍 Redis:"
echo "   - 주소: localhost:6380"
echo "   - 비밀번호: egovframe123"
echo ""
echo "🔧 관리 명령어:"
echo "   - 중지: docker-compose down"
echo "   - 로그 확인: docker-compose logs -f"
echo "   - 데이터 초기화: docker-compose down -v && docker-compose up -d"
echo "======================================="
