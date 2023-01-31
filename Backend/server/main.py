from flask import Flask, request, session, redirect, url_for
from flask_restful import Api
from enum import Enum
from pathlib import Path
import cv2
from featureExtractionAndComparison.imageProcessing import ImageProcessing
from assets.pictureEncoding import PictureEncoding
import os
import json
from cache import cache

app = Flask(__name__)
api = Api(app)


def store_data(user, image_path):
    cache.set(user, image_path)


def get_data(user):
    if cache.get(user):
        return cache.get(user)
    else:
        return False


@app.route('/authenticate/type/picture', methods=['Post'])
def authenticate_via_picture():
    try:
        data = request.json
        picture_as_byte_string = data["image"]

        image_db = get_data(data["user"])

        if not image_db:
            return json.dumps({'error': "userNotExisting"})

        # picture_as_byte_string = open("./pictureData.txt").read()

        image_to_verify = "../featureExtractionAndComparison/imageToVerify.jpg"
        PictureEncoding.base64_to_image(base64_string=picture_as_byte_string, image_path=image_to_verify)

        path = os.getcwd()

        img1_path = os.path.join("../featureExtractionAndComparison/Sylvester_Stallone_0002.jpg")
        img2_path = os.path.join("../featureExtractionAndComparison/Sylvester_Stallone_0005.jpg")
        img3_path = os.path.join("../featureExtractionAndComparison/Pamela_Anderson_0003.jpg")

        pocket_model_path = Path("../featureExtractionAndComparison/PocketNetS.pth")
        pocket_threshold = 0.19586977362632751

        image_processor = ImageProcessing(
            model_path=os.path.join(path, pocket_model_path),
            threshold=pocket_threshold)

        picture_db = image_processor.detect_face(cv2.imread(image_db))
        embedded_picture_db = image_processor.embedd_image(picture_db)

        picture_for_auth = image_processor.detect_face(cv2.imread(image_to_verify))
        embedded_image_for_auth = image_processor.embedd_image(picture_for_auth)

        return json.dumps({"isSimilar": str(image_processor.is_similar(embedded_picture_db, embedded_image_for_auth))})

    except Exception as error:
        return json.dumps({'error': str(error)})


@app.route('/register', methods=['Post'])
def register():
    try:
        data = request.json
        picture_as_byte_string = data["image"]
        user = data["user"]

        image_for_database = "../featureExtractionAndComparison/imageForDatabase.jpg"
        PictureEncoding.base64_to_image(base64_string=picture_as_byte_string, image_path=image_for_database)

        store_data(user=user, image_path=image_for_database)

        return json.dumps("success")

    except Exception as error:
        return json.dumps({'error': str(error)})


class Model(Enum):
    POCKET_NET = 0
    MIX_FACE_NETS = 1
    ELASTIC_FACE = 2


if __name__ == '__main__':
    app.secret_key = "seeeecret"
    cache.init_app(app, config={"CACHE_TYPE": "filesystem",'CACHE_DIR': Path('/tmp')})
    app.run(host="0.0.0.0")
