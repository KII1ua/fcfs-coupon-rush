-- k6 실행 후 DB 기준으로 정합성을 확인한다. (k6 의 201 카운트는 클라이언트가 본 숫자이고, 진실은 DB 에 있다)
-- docker exec -i fcfs-postgres psql -U fcfs -d fcfs -v round_id=<회차 id> < k6/verify.sql

\echo '== 1. 발급 수량: issued 가 total_quantity 를 넘으면 초과 발급'
select r.total_quantity,
       count(c.id)                        as issued,
       count(c.id) - r.total_quantity     as over_issued
from issue_round r
left join issued_coupon c on c.round_id = r.id
where r.id = :round_id
group by r.total_quantity;

\echo '== 2. 1인 1장: 한 줄이라도 나오면 중복 발급'
select users_id, count(*) as coupons
from issued_coupon
where round_id = :round_id
group by users_id
having count(*) > 1
order by coupons desc
limit 20;

\echo '== 3. 중복 발급 받은 유저 수 / 초과로 나간 장수'
select count(*) as duplicated_users, coalesce(sum(coupons - 1), 0) as extra_coupons
from (select users_id, count(*) as coupons
      from issued_coupon
      where round_id = :round_id
      group by users_id
      having count(*) > 1) d;
