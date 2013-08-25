unzip -o 1218-xabber.zip -d ../
perl -p -i.bak -w -e 's/&amp;([l|g]t;)/&$1/g' $(find ../res/ -path ../res/values*.xml)
perl -p -i.bak -w -e "s/\\\\\\\\'/\\\\'/g" $(find ../res/ -path ../res/values*.xml)
find ../res/ -type f -name "*.bak" -exec rm -f {} \;
