for i in `find ../res/ -type d -name value*`; do
    echo $i
    git diff ^HEAD $i | grep + | grep "<string" | perl -p -i.bak -w -e 's/^\+\s*<string\sname="(\w+)">([^\n]*)<\/string>/$1: $2/g' | perl -p -i.bak -w -e 's/\\n/\n/g'
done
