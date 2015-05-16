#!/bin/bash

for x in $(cat res/values/extracted.xml | grep -oP 'app_ops_summaries_\w+'); do 
	op=${x:18}
   	if ! grep -q app_ops_labels_$op res/values/extracted.xml; then 
		echo "Missing: app_ops_labels_$op"
	fi
done
