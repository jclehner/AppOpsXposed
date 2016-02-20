#!/bin/bash

for x in $(cat res/values/ops.xml | grep -oP 'app_ops_summaries_\w+'); do
	op=${x:18}
	if ! grep -q app_ops_labels_$op res/values/ops.xml; then
		echo "<string name=\"app_ops_labels_$op\">@string/app_ops_summaries_$op</string>"
		#echo "Missing: app_ops_labels_$op"
	fi
done
