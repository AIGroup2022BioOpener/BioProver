# BioProver

## Backend

The backend uses ElasticFace and PocketFace models to do face recognition, which mainly divided into the following steps: 
- Image loading
- Face detection, including face cropping and saving images cropped. If no face is detected, an exception will be reported
- Model loading, feature vectors creation and embed based on ElasticFace/PocketFace model
- Image similarity calculation
- Set the threshold and do the actually matching in a verification scenario 

### Instructions for use

#### Model
The ElasticFace model is too big to transfer to github, download "ElasticFaceArc.pth" from the Google Drive and put it in the backend working path ```BioProver/Backend/featureExtractionAndComparison```

[ElasticFaceArc.pth Download](https://drive.google.com/file/d/17MeoOkF7lnZMgi9bteN7SBr3MTr0Agdt/view)

#### Requirements
```
cd Backend
pip install -r requirements.txt
```

#### Run backend
```
cd Backend/server
python3 main.py
```
If you have problems importing classes within the same project
```
Linux
export PYTHONPATH="${PYTHONPATH}:/path/to/your/project/"

Windows:
set PYTHONPATH=%PYTHONPATH%;C:\path\to\your\project\

```
Before starting the app, change the SDK path to your own path in the gradle scripts under local.properties



If the app shall be accessible within the network, set the host to 0.0.0.0
```
app.run(debug=True, host=0.0.0.0)
```

#### Test Api Calls
Can be done with Postman

**Endpoints:**

*http://{localhost}:{port}/authenticate/type/picture*
- http://127.0.0.1:5000/authenticate/type/picture
- Input: {"user": "test", "image": base64String, "net": "String"}

*http://{localhost}:{port}/register*
- http://127.0.0.1:5000/register
- {"user": "test", "image": base64String}


