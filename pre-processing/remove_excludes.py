import sys
import re

orig_directory = sys.argv[1]

ZH_REGEX = re.compile(r'[\u4e00-\u9fff]+')

excludeList = None
with open('resources/excludeList') as f:
    excludeList = f.readlines()
excludeList = [element.split('#')[0] for element in excludeList]

the_list= open(orig_directory)
for line in the_list:
    types = line.split('\t')
    types_list = types[20].split(',')
    exclude = False
    for a_type in types_list:
        for exclude_item in excludeList:
            if a_type.strip() == exclude_item.strip():
                exclude = True

    # ignore anything with no Han text
    # I think this may miss stuff but only obscure characters?
    contains_zh = False
    for field in types:
        if re.findall(ZH_REGEX, field):
            contains_zh = True
    if not contains_zh:
        exclude = True

    if not exclude:
        sys.stdout.write(line)
        sys.stdout.flush()
