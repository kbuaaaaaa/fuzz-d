#!/usr/bin/env bash

# Check if folder name is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <test_folder>"
    exit 1
fi

# Folder name
test_folder=$1
fuzzd_log=$test_folder/fuzz-d.log
# Name of the script to be generated
test_script=$test_folder/interestingness_test.sh

# Create the script file
touch $test_script

# Make the script file executable
chmod +x $test_script

# Write the script
echo '#!/usr/bin/env bash' > $test_script
echo 'java -jar ../../../../app/build/libs/app.jar validate main.dfy' >> $test_script
echo 'fuzzd_log='fuzz-d.log'' >> $test_script

if grep -q "Java crash: true" $fuzzd_log; then
    echo 'if ! grep -q "Java crash: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
else
    echo 'if grep -q "Java crash: true" "$fuzzd_log"; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
fi

if grep -q "Compiler crash: true" $fuzzd_log; then
    echo 'if ! grep -q "Compiler crash: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
else
    echo 'if grep -q "Compiler crash: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
fi

if grep -q "Execute crash: true" $fuzzd_log; then
    echo 'if ! grep -q "Execute crash: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
else
    echo 'if grep -q "Execute crash: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
fi

if grep -q "Different output: true" $fuzzd_log; then
    echo 'if ! grep -q "Different output: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
else
    echo 'if grep -q "Different output: true" $fuzzd_log; then' >> $test_script
    echo '  exit 1' >> $test_script
    echo 'fi' >> $test_script
fi

echo "exit 0" >> $test_script

exit 0