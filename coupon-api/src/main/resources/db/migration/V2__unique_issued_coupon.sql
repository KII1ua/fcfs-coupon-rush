-- Redis 가 1인 1장을 판정하지만, Redis 를 거치지 않는 경로나 Redis 데이터 유실에 대비해 DB 가 최후 방어선으로 중복을 거부한다.
-- 기존 (round_id, users_id) 일반 인덱스는 Unique 인덱스가 대신한다.
alter table issued_coupon
    add constraint uk_issued_coupon_round_users unique (round_id, users_id),
    drop index idx_issued_coupon_round_users;
