from flask import Flask, jsonify, request
from flask_restful import Api
from enum import Enum
from pathlib import Path
import torch
import cv2
import numpy as np
from Backend.featureExtractionAndComparison.imageProcessing import ImageProcessing
import os

app = Flask(__name__)
api = Api(app)

"""
@app.route('/authenticate/type/featureVector', methods=['Post'])
def authenticate_via_feature_vector():
    print(request)
    data = request

    picture_db = imageProcessor.detect_face(cv2.imread(img1_path))
    embedded_picture_db = imageProcessor.embedd_image(picture_db)

    picture_for_auth = imageProcessor.detect_face(cv2.imread(img2_path))
    embedded_image_for_auth = imageProcessor.embedd_image(picture_for_auth)

    return jsonify(imageProcessor.is_similar(embedded_picture_db, embedded_image_for_auth))
"""

@app.route('/authenticate/type/picture', methods=['Post'])
def authenticate_via_picture():
    print(request)
    data = request

    path = os.getcwd()

    img1_path = os.path.join("../featureExtractionAndComparison/Sylvester_Stallone_0002.jpg")
    img2_path = os.path.join("../featureExtractionAndComparison/Sylvester_Stallone_0005.jpg")

    pocket_model_path = Path("../featureExtractionAndComparison/PocketNetS.pth")
    pocket_threshold = 0.19586977362632751

    imageProcessor = ImageProcessing(
        model_path=os.path.join(path, pocket_model_path),
        threshold=pocket_threshold)


    picture_db = imageProcessor.detect_face(cv2.imread(img1_path))
    embedded_picture_db = imageProcessor.embedd_image(picture_db)

    picture_for_auth = imageProcessor.detect_face(cv2.imread(img2_path))
    embedded_image_for_auth = imageProcessor.embedd_image(picture_for_auth)

    print(imageProcessor.is_similar(embedded_picture_db, embedded_image_for_auth))
    return "true"


class DoorOpener:
    def openDoor(self):
        return True


class Model(Enum):
    POCKET_NET = 0
    MIX_FACE_NETS = 1
    ELASTIC_FACE = 2


if __name__ == '__main__':
    app.run(debug=True)

    pocket_model_path = "PocketNetS.pth"
    pocket_threshold = 0.19586977362632751

    imageProcessor = ImageProcessing(threshold=pocket_threshold)

    # Image loading &  preparing
    img1_path = "Sylvester_Stallone_0002.jpg"
    img2_path = "Sylvester_Stallone_0005.jpg"
