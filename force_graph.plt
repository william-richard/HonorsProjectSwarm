set terminal pdf color linewidth 3 fname "Helvetica"

set style line 1 pt 3 ps 3

set key right top

set xlabel "Distance"

set ylabel "Force"

set output "force_graph.pdf"

sep_multiplier = 50
sep_min = .1
sep_max = 40*2
sep_shape = 2.5
sep_exp = sep_shape -2

dan_multiplier = 55
dan_min =.1
dan_max = 12
dan_shape = 4.5
dan_exp = dan_shape - 2

fir_multiplier = sep_multiplier * 2
fir_min = 1
fir_max = 12
fir_shape = 4.5
fir_exp = fir_shape - 2


plot [0:40] [0:fir_multiplier+5] \
sep_multiplier * ((x**sep_exp - sep_max**sep_exp)/(sep_min**sep_exp - sep_max**sep_exp)), \
dan_multiplier * ((x**dan_exp - dan_max**dan_exp)/(dan_min**dan_exp - dan_max**dan_exp)), \
fir_multiplier * ((x**fir_exp - fir_max**fir_exp)/(fir_min**fir_exp - fir_max**fir_exp)), \
x/2
