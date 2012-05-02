#!/bin/sh

find . -name '*~' |xargs rm
cscope -Rbkq
find $(pwd) -name "*.h" \
		 -o -name "*.S" \
		 -o -name "*.s" \
		 -o -name "*.c" \
		 -o -name "*.cc" \
		 -o -name "*.g" \
		 -o -name "*.java" > cscope.files
cscope -bkq -i cscope.files
ctags -R --c++-kinds=+p --fields=+iaS --extra=+q
