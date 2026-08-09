# mud-stock

Minimal Spring Boot application that connects to a local MySQL database.

Configuration is in `src/main/resources/application.yml`.

Run:

```bash
export SPRING_PROFILES_ACTIVE=server
mvn spring-boot:run 
mvn spring-boot:run -Dspring-boot.run.profiles=server
mvn spring-boot:run -Dspring-boot.run.profiles=local,cronjob
```

CSV Data Import

mvn -q -DskipTests exec:java   -Dexec.mainClass=com.rama.mudstock.util.TestDataCsvImportUtil   -Dexec.args="--spring.profiles.active=local"


Local Development details available Data Dates 

option_snapshot_flatfile >  13 to 17 July 2026, 20 to 24 July 2026
option_snapshot > 10 July 2026
option_contract > 17 July, 24-July
day_stock_movement_entry > 
  Adjusted for MU 20, 21, 22, 23 & 24 July 2026. 23 & 24 data adjusted for snapshot & flatfile. 





SAMPLE QUERY:  
SELECT DATE(local_time) AS data_date, COUNT(*) AS row_count
FROM option_snapshot_flatfile
GROUP BY DATE(local_time)
ORDER BY data_date;


The app exposes a small REST API at `GET /api/stocks` and `POST /api/stocks`.



Bezinga

Firm Details 
http://localhost:9003/benzinga/v1/firms?limit=1000&sort=name.asc&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV
https://api.massive.com/benzinga/v1/firms?limit=8000&sort=name.asc&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV




Analyst 


rating enum > outperform, neutral, overweight, buy, "sector perform", "equal-weight", sell, hold, positive, "market perform"

rating_action enum >  maintains, raises, "reiterates", "initiates_coverage_on", "announces", downgrades, lowers, 




Consensus

http://localhost:9003/benzinga/v1/consensus-ratings/AAOI?date=2026-06-01&limit=100&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV

https://api.massive.com/benzinga/v1/consensus-ratings/MU?date=2026-06-01&limit=100&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV


Analyst Rating

https://api.massive.com/benzinga/v1/ratings?ticker=ACN&limit=100&sort=last_updated.desc&last_updated.gte=2026-01-01&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV

http://localhost:9003/benzinga/v1/ratings?ticker=AAOI&limit=100&sort=last_updated.desc&last_updated.gte=2026-01-01&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV

Analyst Details

http://localhost:9003/benzinga/v1/analysts?benzinga_id=5f634eb692f07400010e2bcc&limit=100&sort=full_name.asc&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV

https://api.massive.com/benzinga/v1/analysts?benzinga_id=628b6bab3547bd000104dc00&limit=100&sort=full_name.asc&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV


Firm Details

https://api.massive.com/benzinga/v1/firms?limit=8000&sort=name.asc&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV

http://localhost:9003/benzinga/v1/firms?limit=1000&sort=name.asc&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV

Firm Details By ID
https://api.massive.com/benzinga/v1/firms?benzinga_id=643e5ba89eebb2000134dc4e&apiKey=0jSwNlbvfYIgQ8OAVJsZ7T1sEmpWm7JV





Go Back Ctrl + -
Go Forward Ctrl + Shift + -
Go to Definition F12
Peek Definition Option + F12
Go to File Cmd + P
Go to Symbol Cmd + Shift + O