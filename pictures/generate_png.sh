#!/bin/bash

x=".png"
for variable in `ls | grep ".dot"`
  do
    echo "$variable"
    sfdp -x -Goverlap=prism -Tpng "$variable" > "$variable$x"
  done
