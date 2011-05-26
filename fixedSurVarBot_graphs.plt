set terminal pdf color linewidth 3 fname "Helvetica,8"

set style line 1 pt 1 ps 1 lc 1 lt 1
set style line 2 pt 2 ps 1 lc 3 lt 2
set style line 3 pt 4 ps 1 lc 4 lt 4

set xlabel "Number of Robots"

set datafile missing 'NaN' # The IEEE floating point not-a-number 

set ylabel "Average Overall Metric for\nTimesteps 200 to 1800"
set yrange [-.05:1.05]

set key left top

sortedFile = "< sort -n avgOverall.dat"

set output "./avgOverall.pdf"
plot sortedFile u 1:2:3 with errorbars ls 1 title "Average Value +- 1 std dev", \
sortedFile u 1:2 ls 2 smooth bezier title "Trend Curve defined by Bezier Curve"