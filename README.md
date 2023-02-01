# BioProver

## Backend

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


