INSERT INTO categories (name)
VALUES ('Clothes'),
       ('Electronics'),
       ('Food'),
       ('Furniture')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (username, password, name)
VALUES ('admin', '$2a$10$2SNmiVY92TKT4o85FfBHVeLKodsAzFyEK/AONL4VgK2xmdcvaELSC', 'Administrator')
ON CONFLICT (username) DO NOTHING;
