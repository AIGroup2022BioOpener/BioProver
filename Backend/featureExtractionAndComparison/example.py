# Short example how to do Face Recognition
# Author: Marco Huber, Fraunhofer IGD, 2022
# marco.huber@igd.fraunhofer.de
# Co-Author/Modified by: Jieqing Yang

import torch
import cv2
import numpy as np

# ElasticFace
from elasticBackbone.iresnet import iresnet100

# PocketFace
from pocketBackbone.augment_cnn import AugmentCNN 
import pocketBackbone.genotypes as gt

from skimage import transform
from facenet_pytorch import MTCNN
from crop import norm_crop

# Detects the faces, crop and transform it
def detect_face(image):
    # returns images cropped to include the face only.
    mtcnn = MTCNN(select_largest=True, min_face_size=60, post_process=False, device="cpu")
    # Detect all faces in PIL image and return bounding boxes and optional facial landmarks.
    # given raw input images types: PIL image or list of PIL images, numpy.ndarray (uint8)
    boxes, probs, landmarks = mtcnn.detect(image, landmarks=True)
    facial5points = landmarks[0]
    
    # transforms image to match the landmarks with reference landmarks
    # returns the cropped and transformed image
    warped_face = norm_crop(image, landmark=facial5points, image_size=112)

    return warped_face

# Preprocess image e.g Transformation
# preprocess method is deprecated
# exapmle: image = preprocess(image, bbox=boxes[0], landmark=facial5points, image_size=[112, 112]) 
def preprocess(img, bbox=None, landmark=None, image_size=[112, 112]):
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
        M = tform.params[0:2,:]
        
        warped = cv2.warpAffine(img,M,(image_size[1],image_size[0]), borderValue = 0.0)
        return warped

# Creates model with correct architecture and set parameters as saved
def load_model(elastic_model_path, pocket_model_path):
    
    # ElasticFace
    elastic_model = iresnet100(num_features=512).to("cpu") # Create model
    elastic_model.load_state_dict(torch.load(elastic_model_path, map_location=torch.device("cpu"))) # Load parameters
    elastic_model.train(False) # Set model to inference mode and disable training
    
    # PocketFace
    genotype = gt.from_str("Genotype(normal=[[('dw_conv_3x3', 0), ('dw_conv_1x1', 1)], [('dw_conv_3x3', 2), ('dw_conv_5x5', 0)], [('dw_conv_3x3', 3), ('dw_conv_3x3', 0)], [('dw_conv_3x3', 4), ('skip_connect', 0)]], normal_concat=range(2, 6), reduce=[[('dw_conv_3x3', 1), ('dw_conv_7x7', 0)], [('skip_connect', 2), ('dw_conv_5x5', 1)], [('max_pool_3x3', 0), ('skip_connect', 2)], [('max_pool_3x3', 0), ('max_pool_3x3', 1)]], reduce_concat=range(2, 6))")
   
    pocket_model = AugmentCNN(C=16, n_layers=18, genotype=genotype, stem_multiplier=4, emb=128).to("cpu")
    pocket_model.load_state_dict(torch.load(pocket_model_path, map_location=torch.device("cpu")))
    pocket_model.train(False)
    
    return elastic_model, pocket_model

# Takes an image and a model and creates the feature vector
# embedd_image is a vector of numbers to represent an image derived by passing the image through some Neural Network
def embedd_image(image, model):
    image = prepare_image(image)
    # Disabling gradient calculation is useful for inference, when  not call Tensor.backward(). It will reduce memory consumption for computation
    with torch.no_grad():
        feature = model(image) # Pass image through model
        feature = feature[0] # Get feature vector
        feature = feature.data.cpu().numpy() # Convert to numpy array
        feature = feature.flatten() 
    return feature

# Takes an raw images and preprocesses it to be used by the model
# (deprecated) WARNING: Might be different for different models (checked it's thesame for both of them.)
def prepare_image(image):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB) # Convert to RGB
    image = ((image / 255) - 0.5) / 0.5 # Normalize
    aligned = np.transpose(image, (2,0,1)).astype('float32') # Transpose
    aligned = np.expand_dims(aligned, axis=0) # Expand dimension
    aligned = torch.tensor(aligned).to("cpu") # Convert to tensor
    return aligned

# Takes two feature vector and calculates the cosine similarity
def cos_sim(reference, probe):
    reference, probe = reference.reshape(-1), probe.reshape(-1) 
    return np.dot(reference, probe) / (np.linalg.norm(reference) * np.linalg.norm(probe)) # Cosine similarity

# Does the actually matching in a Verification scenario.
def verify(similarity, threshold):
    if similarity > threshold: 
        print("Match!: Similarity:" + str(similarity) + " and Threshold:" + str(threshold))
    else: 
        print("No Match! : Similarity:" + str(similarity) + " and Threshold:" + str(threshold))
        
## MAIN ##
if __name__ == '__main__':

    elastic_model_path = "ElasticFaceArc.pth"
    pocket_model_path = "PocketNetS.pth"
    
    # Different Threshold for different models 
    elastic_threshold = 0.16728267073631287 
    pocket_threshold = 0.19586977362632751 
    
    # Just as an example
    
    # Image loading &  preparing
    
    img1_path = "Sylvester_Stallone_0002.jpg" 
    img2_path = "Sylvester_Stallone_0005.jpg"
    img3_path = "Pamela_Anderson_0003.jpg"
    
    # Load images
    img1 = cv2.imread(img1_path)
    img2 = cv2.imread(img2_path)
    img3 = cv2.imread(img3_path)
    
    # Face detection
    face_img1 = detect_face(img1)
    face_img2 = detect_face(img2)
    face_img3 = detect_face(img3)
    
    # Just for you
    cv2.imwrite("img1.jpg", face_img1) # Save image
    cv2.imwrite("img2.jpg", face_img2)
    cv2.imwrite("img3.jpg", face_img3)
    
    # load model & create feature vectors
    elastic_model, pocket_model = load_model(elastic_model_path, pocket_model_path)
    
    # Embedd based on ElasticFace
    elastic_emb1 = embedd_image(face_img1, elastic_model)
    elastic_emb2 = embedd_image(face_img2, elastic_model)
    elastic_emb3 = embedd_image(face_img3, elastic_model)
    
    # Embedd based on PocketFace
    pocket_emb1 = embedd_image(face_img1, pocket_model)
    pocket_emb2 = embedd_image(face_img2, pocket_model)
    pocket_emb3 = embedd_image(face_img3, pocket_model)
    
    # Sim for Elastic
    elastic_cos_sim12 = cos_sim(elastic_emb1, elastic_emb2) 
    elastic_cos_sim13 = cos_sim(elastic_emb1, elastic_emb3)
    
    # Sim for Pocket
    pocket_cos_sim12 = cos_sim(pocket_emb1, pocket_emb2) 
    pocket_cos_sim13 = cos_sim(pocket_emb1, pocket_emb3)
    
    # Matching - Elastic  
    verify(elastic_cos_sim12, elastic_threshold)
    verify(elastic_cos_sim13, elastic_threshold)
    
    # Matching - Pocket
    verify(pocket_cos_sim12, pocket_threshold)
    verify(pocket_cos_sim13, pocket_threshold)
    