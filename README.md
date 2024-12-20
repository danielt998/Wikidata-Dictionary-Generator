# CC-CEDICT-additions
Generate some dictionary entries for CC-CEDICT and Pleco using a json wikidata dump. See /output for the generated output files. Note that old_output.txt has more entries but it was generated before I modified the code not to output anything with ambiguous pinyin and only to auto-convert between traditional/simplified if cases that are unambiguous (the original functionality is still available by modifying some of the constants in Main.java and might be useful if you weren't interested in the pinyin or whatever and just wanted to use the data for segmentation or something else)

There is a two stage process for doing this as parsing the json dump takes ages and I wanted to be able to develop more quickly. The `pre-process.py` Python script in `pre-processing` will do generate the intermediate .tsv file in `intermediate_data` which is used by the Java applicaiton.

There is also a script to exclude certain types of Wikidata item but I didn't get too far with this as there are too many types of item that I'd like to exclude. Unfortunately that does mean that there are a whole load of entries that make no sense in a dictionary.

I haven't updated the data for years (2018/2019)ish I think) - using a new version of the json dump would make a huge difference but I have yet to download it (130GB compressed and I don't even know uncompressed - probably in the Terabytes) and re-run.




ideas for other translation sources:
  there must be some lists of place name translations, e.g. where does Facebook get these from?
  
  extract from wikipedia? There may be some that have translations here but not in Wikidata, some
    html parsing/regex would be required here and could get ugly (look into zh template, especially as they often have pinyin too)
  

also, would be nice to do some analytics, i.e. find the total number of instances of each type and sort these by frequency
