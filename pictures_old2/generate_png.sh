#!/bin/bash

x=".png"
for variable in `ls | grep "real_test_new_y_module0____.dot"`
  do
    echo "$variable"
    sfdp -Goverlap=scale -Tpng "$variable" > "$variable$x"
  done
