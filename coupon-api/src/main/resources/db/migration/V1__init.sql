-- 1단계: 단순 CRUD. 엔티티는 서로 객체로 참조하지 않고 id(Long)로만 가리키지만, DB 에는 FK 를 건다.

create table users (
    id         bigint       not null auto_increment primary key,
    user_id    varchar(50)  not null,
    password   varchar(255) not null,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    constraint uk_users_user_id unique (user_id)
);

create table coupon_policy (
    id                  bigint       not null auto_increment primary key,
    name                varchar(100) not null,
    discount_value      int          not null,
    max_discount_amount int          not null,
    valid_until         datetime(6)  not null,
    created_at          datetime(6)  not null,
    updated_at          datetime(6)  not null
);

create table issue_round (
    id             bigint      not null auto_increment primary key,
    policy_id      bigint      not null,
    opened_at      datetime(6) not null,
    closed_at      datetime(6) not null,
    total_quantity int         not null,
    created_at     datetime(6) not null,
    updated_at     datetime(6) not null,
    constraint fk_issue_round_policy foreign key (policy_id) references coupon_policy (id)
);

-- (round_id, users_id) Unique 를 일부러 걸지 않았다.
-- 락 없는 1단계에서 동시 요청 시 중복 발급·초과 발급이 실제로 일어나는지 정합성 테스트로 드러내기 위해서다.
create table issued_coupon (
    id         bigint      not null auto_increment primary key,
    round_id   bigint      not null,
    users_id   bigint      not null,
    status     varchar(20) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    index idx_issued_coupon_round_users (round_id, users_id),
    constraint fk_issued_coupon_round foreign key (round_id) references issue_round (id),
    constraint fk_issued_coupon_users foreign key (users_id) references users (id)
);
