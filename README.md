Add this line to Grasscutter build.gradle and recompile.
```
implementation 'commons-logging:commons-logging:1.2'
implementation 'io.jsonwebtoken:jjwt-api:0.11.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.3', 'io.jsonwebtoken:jjwt-gson:0.11.3'
implementation 'org.springframework.security:spring-security-crypto:5.6.3'
```