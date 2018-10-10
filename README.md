## PRE-INSTALLATION
1. Project requires Java 1.8. Test your version from a shell/command prompt by typing:
``` java -version```

2. Install Grails 3.3.X
 *  http://docs.grails.org/latest/guide/gettingStarted.html#downloadingAndInstalling
 
3. Login to mysql as 'root' and create database (you can use any database you wish; see 'NOTE' below)
```
create database <yourdatabase>;
```
4. While in mysql shell, create user and grant privileges using login/password you just created:
```
GRANT ALL PRIVILEGES ON <yourdatabase>.* to <login>@'localhost' IDENTIFIED BY '<password>';
flush privileges;
```
...or if database isn't local...
```
GRANT ALL PRIVILEGES ON <yourdatabase>.* to <login>@'your.server.ip.address' IDENTIFIED BY '<password>';
flush privileges;
```
5. Clone the BeAPI Plugin locally and do a build.
``` 
git clone https://github.com/orubel/Beapi-API-Framework.git
cd Beapi-API-Framework
./gradlew clean;grails install
```

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
NOTE: Use the JDBC Driver and dialect for whatever database you want; this is merely an example for MySQL. To change the database, go into the 'build.gradle' file in 'root' and editing it (http://docs.grails.org/latest/guide/conf.html#dataSource)


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
            preloadDir: '/home/{your home dir}/.beapi/.iostate'
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

## RUN
After you have built the Beapi Plugin and have built THIS implementing project, you can then typed the following command from within the directory structure of the project to run it:
```
java -Dgrails.env=dev -jar build/libs/beapi-backend-0.1.jar -Xms1024m -Xmx2048m -XX:PermSize=128m -XX:MaxPermSize=256m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycleMin=10 -XX:CMSIncrementalDutyCycle=10 -XX:+UseParNewGC -XX:MaxGCPauseMillis=250 -XX:MaxGCMinorPauseMillis=100 -server
```

## Run as a Service
You can also copy the 'beapi_backend.sh' script to your /etc/init.d' directory on your server to run the jar as a service.



