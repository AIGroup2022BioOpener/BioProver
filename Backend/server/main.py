from flask import Flask, jsonify, request
from flask_restful import Api
from enum import Enum


app = Flask(__name__)
api = Api(app)

featureVector = {
    'username': 'test',
    'featureVector': 'test2'
}

picture = {
    'username': 'test',
    'picture': 'test'
}

database = {
    'test': 'test',
    'test1': 'test2'
}


@app.route('/authenticate/type/featureVector', methods=['Post'])
def authenticateViaFeatureVector():
    data = request.json
    username = data['username']
    featureVector = data['featureVector']

    databasePicture = database[username]

    evaluation = Evaluation(databasePicture, featureVector)
    cosineSimilarity = evaluation.calculateCosineSimilarity()
    twoPicturesAreSimilarEnough = evaluation.isSimilar(cosineSimilarity, treshhold=2)

    doorToOpen = DoorOpener()

    if twoPicturesAreSimilarEnough:
        doorToOpen.openDoor()

    return jsonify(twoPicturesAreSimilarEnough)


@app.route('/authenticate/type/picture', methods=['Post'])
def authenticateViaPicture():
    data = request.json
    username = "test"
    print("hello---------------------------------")
    print(data)
    picture = data['image']

    # print(data)

    return "true"


class Evaluation:
    def __init__(self, featureVectorForAuthentification, featureVectorFromDatabase):
        self.featureVectorForAuthentification = featureVectorForAuthentification
        self.featureVectorFromDatabase = featureVectorFromDatabase

    def calculateCosineSimilarity(self):
        return 3

    def isSimilar(self, similarityScore=5 ,treshhold=3):
        return True if similarityScore >= treshhold else False



class CreateFeatureVector():
    def __init__(self, model, picture):
        self.model = model
        self.picture = picture

    def getFeatureVector(self):
        return 5


class DoorOpener:
    def openDoor(self):
        return True


class Model(Enum):
    POCKET_NET = 0
    MIX_FACE_NETS = 1
    ELASTIC_FACE = 2


if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0")