-- Clickstream enriched with user account data
CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS 
SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent 
FROM clickstream c 
LEFT JOIN web_users u 
ON c.userid = u.user_id;

{
    depends:
            - clickstream
            - web_users
}