#!/bin/bash

#enables execution from another directory
p=/home/tam/workspaces/current/DatasetTools
#p=.

java -cp $p/bin:$p/lib/pdfbox-1.8.2.jar:$p/lib/pdfxtk-backend.jar:$p/lib/commons-logging.jar:$p/lib/log4j-1.2.14.jar:$p/lib/xercesImpl.jar at.ac.tuwien.dbai.pdfwrap.MeasureRecognitionPerformance "$@"
