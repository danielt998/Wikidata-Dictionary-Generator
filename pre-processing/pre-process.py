import json

DELIMITER = '\t'
# ideally stop doing this and use escaping - this is a little silly
ALIAS_DELIMITER = 'ᚋ'  # random Ogham character
LIST_DELIMITER = ','


def getDesc(lang_code, dict):
    if dict == None:
        return ''
    descs = dict.get('descriptions')
    if descs == None:
        return ''
    lang = descs.get(lang_code)
    if lang == None:
        return ''
    value = lang.get('value')
    if value == None:
        return ''
    return value


def getAliases(lang_code, dict):
    if dict == None:
        return ''
    aliases = dict.get('aliases')
    if aliases == None:
        return ''
    aliases_for_lang = aliases.get(lang_code)
    if aliases_for_lang == None or aliases_for_lang == []:
        return ""
    alias_string = ''
    for alias in aliases_for_lang:
        if alias == None:
            continue
        value = alias.get('value')
        alias_string = alias_string + (ALIAS_DELIMITER if alias_string != '' else '') + value
    return alias_string


def getLangString(lang_code, dict):
    if dict == None:
        return ''
    labels = dict.get('labels')
    if labels == None:
        return ''
    lang = labels.get(lang_code)
    if lang == None:
        return ''
    value = lang.get('value')
    if value == None:
        return ''
    return value


def getCountry(dict):
    return getValuesFromPropertyID(dict, "P17")


def getValueFromProperty(element):
    mainsnak = element.get('mainsnak')
    if mainsnak is None:
        return ''
    datavalue = mainsnak.get('datavalue')
    if datavalue is None:
        return ''
    value = datavalue.get('value')
    if value is None:
        return ''
    item_id = value.get('id')
    if item_id is None:
        return ''
    return item_id




# return as a list separated by LIST_DELIMITER
def getRegions(dict):
    return getValuesFromPropertyID(dict, 'P131')


def getTypes(dict):
    return getValuesFromPropertyID(dict, 'P31')


def getID(dict):
    return dict.get('id')


def getValuesFromPropertyID(dict, propertyID):
    items = []
    if dict == None:
        return ''
    claimsList = dict.get('claims')
    if claimsList == None:
        return ''
    values = claimsList.get(propertyID)
    if values == None:
        return ''
    for element in values:
        items.append(getValueFromProperty(element))
    ListString = ""
    for index, item in enumerate(items):
        if index == 0:
            ListString = item
        else:
            ListString = ListString + LIST_DELIMITER + item
    return ListString


FILE='/Volumes/Seagate Expansion Drive/wikidata-20240101-all.json/wikidata-20240101-all.json'
# FILE = '../resources/first_10000.json'

count = 0
langs = {'zh', 'zh-hans', 'zh-hant', 'zh-hk', 'zh-mo', 'zh-my', 'zh-sg', 'zh-tw', 'zh-cn'}

# for i in json_lib:
with open(FILE) as infile:
    count = count + 1
    for line in infile:
        if line.startswith("[") or line.startswith("]"):
            continue
        dict = ''
        line = line.rstrip()
        if line.endswith(","):
            dict = json.loads(line[:-1])
        else:
            dict = json.loads(line)  # TODO:test this works on the last line...
        if dict == None:
            continue
        str = ''
        for lang in langs:
            str = str + getLangString(lang, dict) + DELIMITER + getAliases(lang, dict) + DELIMITER
        if str.isspace():  # this only applies while delimiter is a tab **change this**
            continue
        str = str + getLangString('en', dict) + DELIMITER
        print(str + getDesc('en', dict) + DELIMITER
              + getTypes(dict) + DELIMITER
              + getID(dict) + DELIMITER
              + getCountry(dict) + DELIMITER
              + getRegions(dict))
