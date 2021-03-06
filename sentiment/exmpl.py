from nltk.corpus import movie_reviews
import nltk
import random
import string
from nltk import bigrams
from nltk.corpus import stopwords

stop = stopwords.words('english')

def normalizeWords(words):
    return [w.lower() for w in words if w.lower() not in stop and w.strip(string.punctuation)]

documents = [(set(movie_reviews.words(fileid)), category)
              for category in movie_reviews.categories()
              for fileid in movie_reviews.fileids(category)]

random.shuffle(documents)

all_words = nltk.FreqDist(movie_reviews.words())
word_features = list(all_words)[:2000]

def document_features(document):
    document_words = set(document)
    features = {}
    for word in word_features:
        features['contains({})'.format(word)] = (word in document_words)
    return features

featuresets = [(document_features(d), c) for (d,c) in documents]
train_set, test_set = featuresets[100:], featuresets[:100]
classifier = nltk.NaiveBayesClassifier.train(train_set)
classifier.show_most_informative_features(20)
print(nltk.classify.accuracy(classifier, test_set))