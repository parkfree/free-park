# Free Park Service

## Setup Development Environment

### Clone code

```
git clone git@github.com:parkfree/free-park.git
```

### Install dependencies

```
cd free-park
./gradlew build
```

### Setup Database

Create a MySQL instance with docker

```
docker run --name freepark -p 43306:3306 -e MYSQL_ROOT_PASSWORD=any -d mysql:8
```

Import DDL to MySQL

```
mysql -h 127.0.0.1 -u root -p freepark < schema.sql
```

## MySQL Operation

Generate the DDL from existing MySQL database

```
mysqldump --host=127.0.0.1 --port=43306 -u root -p freepark --no-data --databases --skip-add-drop-table --skip-comments | sed 's/ AUTO_INCREMENT=[0-9]*//g'  > schema.sql
```

Backup data

```
mysqldump -h 127.0.0.1 -u root --port 43306 -p freepark > backup-2020-10-08-1020.sql
```

## Deploy

```
nohup java -jar free-park-2.0.0.jar --spring.profiles.active=prod > /dev/null 2>&1 &
```