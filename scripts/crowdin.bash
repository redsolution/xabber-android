unzip -o xabber.zip -d ../xabber/src/main/res
perl -p -i.bak -w -e 's/&amp;([l|g]t;)/&$1/g' $(find ../xabber/src/main/res/ -path ../xabber/src/main/res/values*.xml)
perl -p -i.bak -w -e "s/\\\\\\\\'/\\\\'/g" $(find ../xabber/src/main/res/ -path ../xabber/src/main/res/values*.xml)
perl -p -i.bak -w -e 's/  (<resources)/$1/g' $(find ../xabber/src/main/res/ -path ../xabber/src/main/res/values*.xml)
find ../xabber/src/main/res/ -type f -name "*.bak" -exec rm -f {} \;
