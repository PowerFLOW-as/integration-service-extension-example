#!/bin/bash
mkdir -p zeebe/broker/data
mkdir -p zeebe/monitor/db

chmod -R 770 zeebe
sudo chown -R 65532 zeebe/monitor
sudo chown -R 1001 zeebe/broker