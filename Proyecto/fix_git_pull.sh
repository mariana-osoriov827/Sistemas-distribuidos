#!/bin/bash
cd ~/Desktop/Sistemas-distribuidos/Proyecto
rm -rf target
git pull
mvn clean compile
