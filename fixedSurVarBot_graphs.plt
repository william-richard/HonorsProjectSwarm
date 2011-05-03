set terminal pdf color linewidth 3 fname "Helvetica"

set style line 1 pt 1 ps 1 lc 1 lt 1
set style line 2 pt 2 ps 1 lc 3 lt 2
set style line 3 pt 4 ps 1 lc 4 lt 4

set nokey
#set key left top

set xlabel "Number of Robots"

set datafile missing 'NaN' # The IEEE floating point not-a-number 

set ylabel "Average Overall Metric for Timesteps 200 to 1800"
set yrange [-.05:1.05]

sortedFile = "< sort -n avgOverall.dat"

set output "./avgOverall.pdf"
plot sortedFile u 1:2 ls 1, \
sortedFile u 1:2 ls 2 smooth bezier