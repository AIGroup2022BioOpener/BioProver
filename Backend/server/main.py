from flask import Flask
from flask_restful import Resource, Api, reqparse
import json


app = Flask(__name__)
api = Api(app)

@app.route('/')
def index():
    return json.dumps({'name': 'alice',
                       'email': 'alice@outlook.com'})

if __name__ == '__main__':
    app.run(debug=True, host="")
