import sys, pickle

import os
dir = os.path.dirname(__file__)

import nltk
from nltk import TweetTokenizer

from featureExtractors import bigramsFeatures, mpqaSentimentWordsCountFeatures, extraTwitterFeaturesCount
from featureExtractors import unigramsFeatures
from lexicons.mpqa.mpqaDictionary import MpqaDictionaryWrapper
from normalization import normalizeTwitterWordsWithNegationHandle, normalizeTwitterWordsWithExtraFeatures

mpqaDictionary = MpqaDictionaryWrapper()

def getfeaturesTest(normalizedWords, extraNormalizedWords):
    features = {}
    wordsTagged = nltk.pos_tag(normalizedWords)
    features.update(unigramsFeatures(normalizedWords))
    features.update(bigramsFeatures(normalizedWords))
    features.update(mpqaSentimentWordsCountFeatures(wordsTagged, mpqaDictionary))
    # features.update(mpqaSubjectivityWordsCountFeatures(wordsTagged, mpqaDictionary))
    features.update(extraTwitterFeaturesCount(extraNormalizedWords))
    return features

def main():
    if len(sys.argv) < 2:
        return "Bad command"

    tweet = sys.argv[1]

    tweetTokenizer = TweetTokenizer(reduce_len=True, preserve_case=True, strip_handles=False)

    tokenizedTweet = tweetTokenizer.tokenize(tweet)

    normalizedWords = normalizeTwitterWordsWithNegationHandle(tokenizedTweet)
    extraNormalizedWords = normalizeTwitterWordsWithExtraFeatures(tokenizedTweet)
    testfeatures = getfeaturesTest(normalizedWords, extraNormalizedWords=extraNormalizedWords)

    path = os.path.join(dir, 'twitter', 'dumps', '3way', 'logreg', 'uni-bi-extra-mpqa-senti')
    with open(path, 'rb') as dumpfile:
        classifier = pickle._load(dumpfile)
        print(classifier.classify(testfeatures), end='')


main()
