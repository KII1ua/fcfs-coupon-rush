-- 발급 명단 판정. Redis 는 스크립트를 한 번에 실행하므로 확인과 추가 사이에 다른 요청이 끼어들 수 없다.
-- KEYS[1] : 회차 발급 명단 (Set)
-- ARGV[1] : 유저 id
-- ARGV[2] : 회차 총 수량
-- 반환    : 0 발급, 1 이미 받음, 2 품절

if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
    return 1
end
if redis.call('SCARD', KEYS[1]) >= tonumber(ARGV[2]) then
    return 2
end
redis.call('SADD', KEYS[1], ARGV[1])
return 0
