unzip -o xabber.zip -d ../app/src/main/res
perl -p -i.bak -w -e 's/&amp;([l|g]t;)/&$1/g' $(find ../app/src/main/res/ -path ../app/src/main/res/values*.xml)
perl -p -i.bak -w -e "s/\\\\\\\\'/\\\\'/g" $(find ../app/src/main/res/ -path ../app/src/main/res/values*.xml)
perl -p -i.bak -w -e 's/  (<resources)/$1/g' $(find ../app/src/main/res/ -path ../app/src/main/res/values*.xml)
find ../app/src/main/res/ -type f -name "*.bak" -exec rm -f {} \;
