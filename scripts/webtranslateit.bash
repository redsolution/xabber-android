unzip -o ../../1218-xabber.zip -d ../
perl -p -i.bak -w -e 's/&amp;([l|g]t;)/&$1/g' $(find ../res/ -path ../res/values*.xml)
perl -p -i.bak -w -e "s/\\\\\\\\'/\\\\'/g" $(find ../res/ -path ../res/values*.xml)
find ../res/ -type f -name "*.bak" -exec rm -f {} \;
for i in `find ../res/ -type d -name value*`; do
    echo $i
    git diff ^HEAD $i | grep + | grep -E "("$(cat $(find ../res/values/ -name *.xml) | grep -E '%([0-9]\$|)[sd]' | perl -p -w -e 's/.*name="(\w+)".*$/$1/g' | sed -e :a -e N -e 's/\n/|/' -e ta)")"
    git diff ^HEAD $i | grep + | grep "<string" | perl -p -i.bak -w -e 's/^\+\s*<string\sname="(\w+)">([^\n]*)<\/string>/$1: $2/g' | perl -p -i.bak -w -e 's/\\n/\n/g'
done
