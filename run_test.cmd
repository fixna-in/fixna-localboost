@echo off
cd /d c:\Users\Dell\workspace\fixna-localboost
mvn -f backend\pom.xml test > nodocker_test2.log 2>&1
if errorlevel 1 (echo EXIT-NONZERO >> nodocker_test2.log) else (echo EXIT-0 >> nodocker_test2.log)
