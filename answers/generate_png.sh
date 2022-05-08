#!/bin/bash

x=".png"
for variable in `ls | grep ".dot"`
  do
    echo "$variable"
    sfdp -x -Goverlap=scale -Tpng "$variable" > "$variable$x"
  done