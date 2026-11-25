-- 펫에 얹은 장식 id (예: winter-scarf). 친구 목록·친구 펫에 같이 보여요. 없으면 NULL
ALTER TABLE users
    ADD COLUMN decoration VARCHAR(32) NULL AFTER pet_kind;
