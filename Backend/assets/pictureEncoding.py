import base64


class PictureEncoding:
    @staticmethod
    def base64_to_image(base64_string, image_path):
        with open(image_path, "wb") as image:
            image.write(base64.b64decode(base64_string))

    @staticmethod
    def image_to_base64(image_path):
        with open(image_path) as image:
            return base64.b64decode(image.read())
