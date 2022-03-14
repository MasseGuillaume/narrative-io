# Narrative Code Challenge

```bash
sbt
server/reStart # Running server
loadtest/GatlingIt/test # Load Tests

server/test # Unit test
```

```bash

curl -v -XPOST http://localhost:8090/analytics?timestamp=0&user=u1&event=click
# 204

curl http://localhost:8090/analytics?timestamp=0
# unique_users,1
# clicks,1
# impressions,0
```