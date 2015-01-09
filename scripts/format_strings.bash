for i in `find ../app/src/main/res/ -type d -name value*`; do
    echo $i
    git diff ^HEAD $i | grep + | grep -E "("$(cat $(find ../app/src/main/res/values/ -name *.xml) | grep -E '%([0-9]\$|)[sd]' | perl -p -w -e 's/.*name="(\w+)".*$/$1/g' | sed -e :a -e N -e 's/\n/|/' -e ta)")"
done
