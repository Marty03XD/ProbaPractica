# Porneste aplicatia Spring Boot (nu depinde de PATH-ul din terminal)
$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$mavenBin = "C:\Tools\apache-maven-3.9.16\bin"
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$mavenBin;" + $env:Path
Set-Location $PSScriptRoot
& "$mavenBin\mvn.cmd" spring-boot:run
