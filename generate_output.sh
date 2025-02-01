python3 pre-processing/remove_excludes.py intermediate_data/intermediate.tsv > intermediate_data/intermediate_after_excluding_stuff.tsv
gradle run --console=plain --quiet > output/output_cedict_format.txt
gradle run --console=plain --quiet --args="-f pleco" > output/output_pleco_format.txt
gradle run --console=plain --quiet --args="-f all_data" > output/all_data.tsv

cedict_line_count=$(wc -l output/output_cedict_format.txt)
pleco_line_count=$(wc -l output/output_pleco_format.txt)
all_data_line_count=$(wc -l output/all_data.tsv)
echo "Line counts:"
echo "$cedict_line_count"
echo "$pleco_line_count"
echo "$all_data_line_count"

if command -v gsplit
then
  gsplit -C 99m --numeric-suffixes output/output_cedict_format.txt output/output_cedict_format_split_
  gsplit -C 99m --numeric-suffixes output/output_pleco_format.txt output/output_pleco_format_split_
elif [[ "$OSTYPE" == "darwin" ]]; then
    #This won't work if the total size becomes >200MB but Mac OS doesn't support the -C flag by default
    split -n 2 --numeric-suffixes output/output_cedict_format.txt output/output_cedict_format_split_
    split -n 2 --numeric-suffixes output/output_pleco_format.txt output/output_pleco_format_split_
else
  split -C 99m --numeric-suffixes output/output_cedict_format.txt output/output_cedict_format_split_
  split -C 99m --numeric-suffixes output/output_pleco_format.txt output/output_pleco_format_split_suffixes
fi