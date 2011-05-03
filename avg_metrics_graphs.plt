set terminal pdf color linewidth 3 fname "Helvetica"

set style line 1 pt 1 ps 1 lc 1 lt 1
set style line 2 pt 2 ps 1 lc 3 lt 2
set style line 3 pt 4 ps 1 lc 4 lt 4

set key left top

set xlabel "Simulation Time (seconds)"

set datafile missing 'NaN' # The IEEE floating point not-a-number 


curFile = "surFound.dat"

set ylabel "Percent of Survivors Found"
set yrange [-.05:1.05]

set output "./surFound.pdf"
plot curFile u 1:2 with lines ls 1 t "Average", \
curFile u 1:3 with lines ls 2 t  "Worst", \
curFile u 1:4 with lines ls 3 t "Best"


curFile = "pathQal.dat"
set ylabel "Path Plotting Metric"
set yrange [.95:3.05]

set output "./pathPlotting.pdf"
plot curFile u 1:2 with lines ls 1 t "Average", \
curFile u 1:3 with lines ls 3 t "Best", \
curFile u 1:4 with lines ls 2 t "Worst"


curFile = "pathMark.dat"
set ylabel "Path Marking Metric"
set yrange [.99:1.55]

set output "./pathMarking.pdf"
plot curFile u 1:2 with lines ls 1t "Average", \
curFile u 1:3 with lines ls 3 t "Best", \
curFile u 1:4 with lines ls 2 t "Worst"


curFile = "overall.dat"
set ylabel "Overall Metric"
set yrange [-.05:1.05]

set output "./overall.pdf"
plot curFile u 1:2 with lines ls 1 t "Average", \
curFile u 1:3 with lines ls 2 t "Worst", \
curFile u 1:4 with lines ls 3 t "Best"
