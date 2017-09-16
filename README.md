Project relies on Java 1.8.
Please make sure to have this pre-installed on your system.

## PRE-INSTALLATION
1. Project requires Java 1.8. Test your version from a shell/command prompt by typing:
``` java -version```

2. Install Grails 3.3.X
 *  http://docs.grails.org/latest/guide/gettingStarted.html#downloadingAndInstalling
 
3. Login to mysql as 'root' and create database
```
create database <yourdatabase>;
```
4. While in mysql shell, create user and grant privileges using login/password you just created:
```
GRANT ALL PRIVILEGES ON beapi.* to <login>@'localhost' IDENTIFIED BY '<password>';

```
...or if database isn't local...
```
GRANT ALL PRIVILEGES ON <yourdatabase>.* to <login>@'your.server.ip.address' IDENTIFIED BY '<password>';
flush privileges;
```
5. Clone the BeAPI Plugin locally and do a build
``` git clone https://github.com/orubel/Beapi-API-Framework.git```

6. Build Beapi Plugin
```grails clean;grails install```

## INSTALLATION (this can all be an install script in future)
1. create 'BEAPI' user (with home dir) for server (do not run as root) // todo
2. Move grails-app/conf/templates/parser/APIParse.groovy script to separate dir and change 'login' and 'password' variables for database at top of script
3. Edit /grails-app/conf/beapi.yml and change the default login and password for the database in all environments:
```
        dataSource:
            pooled: true
            jmxExport: true
            driverClassName: "com.mysql.jdbc.Driver"
            dialect: 'org.hibernate.dialect.MySQL5Dialect'
            username: "changeMe"
            password: "changeMe"
            url: "jdbc:mysql://localhost/<yourdatabase>"
            dbCreate: update
```
NOTE: Use the JDBC Driver and dialect for whatever database you want; this is merely an example for MySQL

5. Create a self signed SSL key and store it in '/home/{your home dir}/.keys/keystore.jks' (https://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html)
6. Edit /grails-app/conf/beapi.yml and change the default ssl config:
```
        ssl:
            enabled: true
            key-store: /home/{your home dir}/.keys/keystore.jks
            key-store-password: CHANGESTOREPASSWORD
            key-alias: selfsigned
            key-password: CHANGEPASSWORD
```
7. Edit /grails-app/conf/beapi.yml and change the iostate default directory in the different environments:
```
        iostate:
            preloadDir: '/home/{your home dir}/.iostate'
            archInstanceUrls:
                proxy: "127.0.0.1"
                mq: "127.0.0.1"
```
8. Now create two directories in the directory of the user that will be running the script (NOTE: if you are running this from 'beapi_backend', these are created in 'root'):
```
mkdir ~/.beapi
cd ~/.beapi
mkdir ~/.beapi/.iostate
```
9. Now move all files from 'grails-app/conf/iostate' into '~/.beapi/.iostate'
10 Move 'grails-app/conf/beapi.*' into '~/.beapi'

## BUILD
1. From inside the project directory, type:
```
./gradlew --stop;./gradlew clean;./gradlew build
```


### INITIALIZATION

1. move 'beapi_backend' to '/etc/init.d/' directory and change 'PATH_TO_JAR' so it points to your application
2. start the application by typing:
```
sudo /etc/init.d/beapi_backend start
```


**NOTE** : Will add an 'init.d' script in the future so it can be run as daemon. This will be added to 'INSTALLATION' instructions


