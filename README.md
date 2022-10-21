# Native Change Detection via ETS

## Setup 

* Make sure you have the IdentityNow SDK 

### Build saas-sdk-java

```
git clone https://github.com/sailpoint/saas-sdk-java
cd saas-sdk-java
git checkout origin/updateServices
git checkout updateServices
gradle init
gradle build
```

### Add SDK to this project

* From within the SDK project: 

```
mvn deploy:deploy-file -Durl=file://[[location of this project]]ets/repo -Dfile=build/libs/identitynow-java--sdk-2.0.7.jar -DgroupId=sailpoint.identitynow.api -DartifactId=IdentityNowService -Dpackaging=jar -Dversion=2.0.7
```

### Build ETS code

#### Checkout

```
git clone https://github.com/wim-vandijck-sp/ets.git
cd ets
```

#### Configure
* Edit main/resources/application.properties and set the tenant and PAT info.
* Add the workflowId, and workflow PAT info

#### Build
```
mvn clean compile package
```

### Setup Heroku
(I followed Deploying Spring Boot Applications to Heroku | Heroku Dev Center )

After creating a Heroku account…

```
heroku login
```

### Test Locally
Start local heroku
```
heroku local web
```

## IDN

### Import the workflow

* see `src/main/resources` for a workflow example. I'm not sure how well that will import, since when you use an external trigger, you need to create a PAT for the workflow.