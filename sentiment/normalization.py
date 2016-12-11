from nltk.corpus import stopwords
from stemmingLemmingUtils import doStemming
import string, re
from nltk.stem.snowball import SnowballStemmer

hashtag_code = "_hashtag_"
user_handle_code = "_user_handle_"
url_code = "_url_"
retweet_code = "_retweet_"
positive_emoticon_code = "_positive_emoticon_"
negative_emoticon_code = "_negative_emoticon_"
emoticon_code = "_emoticon_"

stop = stopwords.words('english')
notWantedChars = string.punctuation + string.whitespace + string.digits

urlRegex = r'^(?:(?:https?|ftp)://)(?:\S+(?::\S*)?@)?(?:(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]+-?)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]+-?)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:/[^\s]*)?$'
wwwRegex = r'^www\.\w+(\.\w{2,3})+.*$'
userHandleRegex = r'(^|[^@\w])@(\w{1,15})\b'
hashtagRegex = r'#+[\w_]+[\w\'_\-]*[\w_]+'
positiveEmoticonRegex = r'\|?>?[:*;Xx8=]-?o?\^?[DPpb3)}\]>]\)?'
negativeEmoticonRegex = r'([:><].?-?[@><cC(\[{\|]\|?|[D][:8;=X]<?|v.v)'
emoticonRegex = r'[<>]?[:;=8][\-o\*\']?[\)\]\(\[dDpP/\:\}\{@\|\\]|[\)\]\(\[dDpP/\:\}\{@\|\\][\-o\*\']?[:;=8][<>]?'

negationRegex = r"(?:^(?:never|no|nothing|nowhere|noone|none|not|havent|hasnt|hadnt|cant|couldnt|shouldnt|wont|wouldnt|dont|doesnt|didnt|isnt|arent|aint)$)|n't"
clauseLevelPunctuationRegex = r"^[.:;!?]$"

def normalizeWords(words):
    return [w.lower() for w in words if stripNegation(w).strip(notWantedChars)]

def normalizeTwitterWords(words):
    return [word for word in normalizeWords(words) if not re.match(hashtagRegex, word) and not re.match(urlRegex, word)
            and not re.match(wwwRegex, word) and not re.match(userHandleRegex, word) if len(word) > 1]

def normalizeTwitterWordsWithNegationHandle(words):
    words = handleNegation(list(words))
    return [word for word in normalizeWords(words) if not re.match(hashtagRegex, stripNegation(word)) and not re.match(urlRegex, stripNegation(word))
            and not re.match(wwwRegex, stripNegation(word)) and not re.match(userHandleRegex, stripNegation(word))]

def normalizeTwitterWordsWithExtraFeatures(words):
    words = list(words)
    for i, word in enumerate(words):
        if re.match(urlRegex, word) or re.match(wwwRegex, word):
            words[i] = url_code
        elif re.match(userHandleRegex, word):
            words[i] = user_handle_code
        elif re.match(hashtagRegex, word):
            words[i] = hashtag_code
        elif re.match(positiveEmoticonRegex, word):
            words[i] = positive_emoticon_code
        elif re.match(negativeEmoticonRegex, word):
            words[i] = negative_emoticon_code
        elif re.match(emoticonRegex, word):
            words[i] = emoticon_code
        elif word.lower() == 'rt':
            words[i] = retweet_code
        elif word.lower() in notWantedChars and word != "!" and word != "?":
            del words[i]
    return words

def handleNegationSimple(words):
    i = 0
    while i + 1 < len(words):
        if re.match(negationRegex, words[i]):
            words[i + 1] = "NEG_" + words[i+1]
            i += 2
        else:
            i += 1

    return words

def handleNegation(words):
    i = 0
    while i < len(words):
        if re.match(negationRegex, words[i]):
            j = i + 1
            while j < len(words) and not re.match(clauseLevelPunctuationRegex, words[j]):
                words[j] = "NEG_" + words[j]
                j += 1
            i = j
        else:
            i += 1

    return words

def stripNegation(word):
    if word.startswith("NEG_") or word.startswith("neg_"):
        return word[4:]
    else:
        return word

def isWordNegated(word):
    return word.startswith("NEG_") or word.startswith("neg_")