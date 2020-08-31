## Set up mysql

```
docker run --name freepark -p 43306:3306 -e MYSQL_ROOT_PASSWORD=any -d mysql:8
```

```
nohup ./free-park-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod  > /dev/null 2>&1 &
```