#!/bin/bash

bucket=$1
profile=$2

echo "Removing all versions from $bucket"

cmd="aws s3 rm s3://$bucket/ --recursive --profile $profile"
echo "$cmd"
$cmd

versions=`aws s3api list-object-versions --bucket $bucket --profile $profile | jq '.Versions'`
markers=`aws s3api list-object-versions --bucket $bucket --profile $profile | jq '.DeleteMarkers'`
let count=`echo $versions |jq 'length'`-1

if [ $count -gt -1 ]; then
        echo "removing files"
        for i in $(seq 0 $count); do
                key=`echo $versions | jq .[$i].Key |sed -e 's/\"//g'`
                versionId=`echo $versions | jq .[$i].VersionId |sed -e 's/\"//g'`
                cmd="aws s3api delete-object --bucket $bucket --key $key --version-id $versionId --profile $profile"
                echo $cmd
                $cmd
        done
fi

let count=`echo $markers |jq 'length'`-1

if [ $count -gt -1 ]; then
        echo "removing delete markers"

        for i in $(seq 0 $count); do
                key=`echo $markers | jq .[$i].Key |sed -e 's/\"//g'`
                versionId=`echo $markers | jq .[$i].VersionId |sed -e 's/\"//g'`
                cmd="aws s3api delete-object --bucket $bucket --key $key --version-id $versionId --profile $profile"
                echo $cmd
                $cmd
        done
fi