unzip -o ../../1218-xabber.zip -d ../
find ../res/ -name faq.xml -delete
perl -pi -w -e 's/&amp;([l|g]t;)/&$1/g' $(find ../res/ -path ../res/values*.xml)
perl -pi -w -e "s/\\\\\\\\'/\\\\'/g" $(find ../res/ -path ../res/values*.xml)
for i in `find ../res/ -type d -name value*`; do
    echo $i
    git diff ^HEAD $i | grep + | grep -P "("$(cat $(find ../res/values/ -name *.xml) | grep -P '%(\d\$|)[sd]' | perl -p -w -e 's/.*name="(\w+)".*$/$1/g' | sed -e :a -e N -e 's/\n/|/' -e ta)")"
    git diff ^HEAD $i | grep + | grep "<string" | perl -pi -w -e 's/^\+\s*<string\sname="(\w+)">([^\n]*)<\/string>/$1: $2/g' | perl -pi -w -e 's/\\n/\n/g'
done
