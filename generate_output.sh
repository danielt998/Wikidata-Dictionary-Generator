java -cp out/production/CC-CEDICT-additions src.main.java.Main > output/output_cedict_format.txt
java -cp out/production/CC-CEDICT-additions src.main.java.Main -f pleco > output/output_pleco_format.txt
java -cp out/production/CC-CEDICT-additions src.main.java.Main -f all_data > output/all_data.tsv

cedict_line_count=$(wc -l output/output_cedict_format.txt)
pleco_line_count=$(wc -l output/output_pleco_format.txt)
all_data_line_count=$(wc -l output/all_data.tsv)
echo "Line counts:"
echo "$cedict_line_count"
echo "$pleco_line_count"
echo "$all_data_line_count"