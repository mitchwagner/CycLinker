# Test when three paths are available, out of four 
java -Xmx15360m -jar ../build/libs/quicklinker.jar \
    -n 3-paths/edges.txt \
    -nodeTypes 3-paths/nodes.txt \
    -dfa dfa-edges.txt \
    -dfaNodeTypes dfa-nodes.txt \
    -o ./3-paths/output \
    -rlcsp

# Test when no paths are available
java -Xmx15360m -jar ../build/libs/quicklinker.jar \
    -n no-paths/edges.txt \
    -nodeTypes no-paths/nodes.txt \
    -dfa dfa-edges.txt \
    -dfaNodeTypes dfa-nodes.txt \
    -o ./no-paths/output \
    -rlcsp
