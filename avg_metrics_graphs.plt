set terminal pdf color linewidth 3 fname "Helvetica"

set style line 1 pt 1 ps 1 lc 4 lt 1
set style line 2 pt 2 ps 1 lc 3 lt 2
set style line 3 pt 4 ps 1 lc 1 lt 4

set xlabel "Simulation Time (seconds)"

set datafile missing 'NaN' # The IEEE floating point not-a-number 


curFile = "surFound.dat"

set ylabel "Percent Survivors Found"
set yrange [-1:105]


set key right bottom

set output "./surFound.pdf"
plot curFile u 1:($2*100) with lines ls 1 t "Average", \
curFile u 1:(($2+$5)*100) with lines ls 2 t "Average + 1 standard deviation", \
curFile u 1:(($2-$5)*100) with lines ls 3 t "Average - 1 standard deviation"#, \
#curFile u 1:3 with lines ls 2 t  "Worst", \
#curFile u 1:4 with lines ls 3 t "Best"


set key right top

curFile = "pathQal.dat"
set ylabel "Robot Path Length / Optimal Path Length"
set yrange [.95:3.05]

set output "./pathPlotting.pdf"
plot curFile u 1:2 with lines ls 1 t "Average", \
curFile u 1:($2+$5) with lines ls 2 t "Average + 1 standard deviation", \
curFile u 1:($2-$5) with lines ls 3 t "Average - 1 standard deviation"#, \
#curFile u 1:3 with lines ls 3 t "Best", \
#curFile u 1:4 with lines ls 2 t "Worst"

set key right top

curFile = "pathMark.dat"
set ylabel "Path Marking Quality"
set yrange [.99:1.55]

set output "./pathMarking.pdf"
plot curFile u 1:2 with lines ls 1 t "Average", \
curFile u 1:($2+$5) with lines ls 2 t "Average + 1 standard deviation", \
curFile u 1:($2-$5) with lines ls 3 t "Average - 1 standard deviation"#, \
#curFile u 1:3 with lines ls 3 t "Best", \
#curFile u 1:4 with lines ls 2 t "Worst"


set key right bottom

curFile = "overall.dat"
set ylabel "Overall Metric"
set yrange [-.05:1.05]

set output "./overall.pdf"
plot curFile u 1:2 with lines ls 1 t "Average", \
curFile u 1:($2+$5) with lines ls 2 t "Average + 1 standard deviation", \
curFile u 1:($2-$5) with lines ls 3 t "Average - 1 standard deviation"#, \
#curFile u 1:3 with lines ls 2 t "Worst", \
#curFile u 1:4 with lines ls 3 t "Best"
