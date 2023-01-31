import torch
import cv2
import numpy as np

# PocketFace
from featureExtractionAndComparison.pocketBackbone.augment_cnn import AugmentCNN
import featureExtractionAndComparison.pocketBackbone.genotypes as gt

from skimage import transform
from facenet_pytorch import MTCNN
from featureExtractionAndComparison.crop import norm_crop


class ImageProcessing():
    def __init__(self, threshold, model_path):
        self.model = self.load_model(model_path)
        self.threshold = threshold

    #TODO write cleaner
    def load_model(self, pocket_model_path):
        # PocketFace
        genotype = gt.from_str("Genotype(normal=[[('dw_conv_3x3', 0), ('dw_conv_1x1', 1)], [('dw_conv_3x3', 2), ('dw_conv_5x5', 0)], [('dw_conv_3x3', 3), ('dw_conv_3x3', 0)], [('dw_conv_3x3', 4), ('skip_connect', 0)]], normal_concat=range(2, 6), reduce=[[('dw_conv_3x3', 1), ('dw_conv_7x7', 0)], [('skip_connect', 2), ('dw_conv_5x5', 1)], [('max_pool_3x3', 0), ('skip_connect', 2)], [('max_pool_3x3', 0), ('max_pool_3x3', 1)]], reduce_concat=range(2, 6))")

        pocket_model = AugmentCNN(C=16, n_layers=18, genotype=genotype, stem_multiplier=4, emb=128).to("cpu")
        pocket_model.load_state_dict(torch.load(pocket_model_path, map_location=torch.device("cpu")))
        pocket_model.train(False)

        return pocket_model

    # Detects the faces, crop and transform it
    def detect_face(self, cv2_image):
        # returns images cropped to include the face only.
        mtcnn = MTCNN(select_largest=True, min_face_size=60, post_process=False, device="cpu")

        # Detect all faces in PIL image and return bounding boxes and optional facial landmarks.
        # given raw input images types: PIL image or list of PIL images, numpy.ndarray (uint8)
        boxes, probs, landmarks = mtcnn.detect(cv2_image, landmarks=True)
        print("b", boxes, "p", probs, "l", landmarks)
        facial5points = landmarks[0]

        # transforms image to match the landmarks with reference landmarks
        # returns the cropped and transformed image
        return norm_crop(cv2_image, landmark=facial5points, image_size=112)

    def is_similar(self, emb_1, emb_2):
        cos_sim = self.cos_sim(emb_1, emb_2)
        print("Similarity: " + str(cos_sim) + " and Threshold: " + str(self.threshold))
        return self.verify(cos_sim)

    # Takes an image and a model and creates the feature vector
    # embedd_image is a vector of numbers to represent an image derived by passing the image through some Neural Network
    def embedd_image(self, image):
        prepared_image = self.prepare_image(image)

        # Disabling gradient calculation is useful for inference, when  not call Tensor.backward(). It will reduce memory consumption for computation
        with torch.no_grad():
            feature = self.model(prepared_image) # Pass image through model
            feature = feature[0] # Get feature vector
            feature = feature.data.cpu().numpy() # Convert to numpy array
            feature = feature.flatten()
        return feature

    # Takes an raw images and preprocesses it to be used by the model
    # (deprecated) WARNING: Might be different for different models (checked it's thesame for both of them.)
    def prepare_image(self, image):
        image_rbg = cv2.cvtColor(image, cv2.COLOR_BGR2RGB) # Convert to RGB
        image_norm = ((image_rbg/255) - 0.5) / 0.5 # Normalize
        aligned = np.transpose(image_norm, (2,0,1)).astype('float32') # Transpose
        aligned = np.expand_dims(aligned, axis=0) # Expand dimension
        aligned = torch.tensor(aligned).to("cpu") # Convert to tensor
        return aligned

    #TODO maybe split evaluation into new class
    # Takes two feature vector and calculates the cosine similarity
    def cos_sim(self, reference, probe):
        reference_reshaped, probe_reshaped = reference.reshape(-1), probe.reshape(-1)
        return np.dot(reference_reshaped, probe_reshaped) / (np.linalg.norm(reference_reshaped) * np.linalg.norm(probe_reshaped)) # Cosine similarity

    # Does the actually matching in a Verification scenario.
    def verify(self, similarity):
        return similarity > self.threshold

    # Preprocess image e.g Transformation
    # preprocess method is deprecated
    # exapmle: image = preprocess(image, bbox=boxes[0], landmark=facial5points, image_size=[112, 112])
    def preprocess(self, bbox=None, landmark=None, image_size=[112, 112]):
        if landmark is not None:
            src = np.array([
                [30.2946, 51.6963],
                [65.5318, 51.5014],
                [48.0252, 71.7366],
                [33.5493, 92.3655],
                [62.7299, 92.2041] ], dtype=np.float32)
            src[:, 0] += 8.0
            dst = landmark.astype(np.float32)

            tform = transform.SimilarityTransform()
            tform.estimate(dst, src)
            M = tform.params[0:2, :]

            return cv2.warpAffine(self.image, M, (image_size[1], image_size[0]), borderValue=0.0)
