-- number of events per minute - think about key-for-distribution-purpose - shuffling etc - shouldnt use 'userid'
CREATE table events_per_min AS 
SELECT userid, count(*) AS events 
FROM clickstream window TUMBLING (size 60 second) 
GROUP BY userid;

{
    depends:
            - clickstream
    delete: true
}