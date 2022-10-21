# Native Change Detection via ETS

This is a Spring Boot project that can be loaded into heroku, and called as an ETS from IDN.

It will trigger on an account change, send a call to the Heroku app. The app will check the identity snapshots for the new access, and if not recognized, launch a certification to the manager.

## Requirementns

* Have heroku setup (see below)
* Have the IDN SDK installed (see below)
* Have the account-changed trigger enabled on your tenant.


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

#### application.properties example

```
logging.level.root=warn
logging.level.org.springframework.web=warn
logging.level.sailpoint.ets=trace
logging.level.org.hibernate=error

tenant=company1234-poc
patid=83c6bff740a24735919b0ce1fc...
patsecret=ce079eb807f1....0df0704221c3056056847457e
demotenant=yes # for DemoHub tenants

workflowid=4da1271a-9eb7-4648-a9b2-5aac7e30345a
wfpatid=33a2b779-9580-4e18-9c50-f30fb9011f42
wfpatsecret=763292c4abbbdc05a185c278abea7c82ec0fc586d57035ba2484adc204766cde
```

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
git push heroku master
heroku local web
```

## IDN

### Import the workflow

* see `src/main/resources` for a workflow example. I'm not sure how well that will import, since when you use an external trigger, you need to create a PAT for the workflow.

The workflow takes following input :
```
{
  "input": {
    "identityId": "2c9180868222b4c8018236703ab85803",
    "sourceId" : "2c9180868222b4c8018236703ab85802",
    "entitlement": "[Administrators]"
  }
}
```